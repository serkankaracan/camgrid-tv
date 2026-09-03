package io.github.serkankaracan.camgridtv.playback

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackCoordinatorTest {
    private val uriFactory = RtspUriFactory()

    @Test
    fun `reapplying same grid never creates duplicate engines`() = runTest {
        val factory = FakePlaybackEngineFactory()
        val coordinator = coordinator(factory)
        val requests = listOf(gridRequest("one", 11), gridRequest("two", 12))

        coordinator.showGrid(requests)
        coordinator.showGrid(requests)
        coordinator.onForeground()

        assertEquals(2, factory.created.size)
        assertEquals(2, coordinator.activeEngineCount())
        assertTrue(factory.created.all { it.startCount == 1 && it.releaseCount == 0 })
        coordinator.leaveScreen()
    }

    @Test
    fun `background releases every engine and foreground recreates desired plan`() = runTest {
        val factory = FakePlaybackEngineFactory()
        val coordinator = coordinator(factory)
        coordinator.showGrid(listOf(gridRequest("one", 11), gridRequest("two", 12)))

        coordinator.onBackground()
        coordinator.onBackground()

        assertEquals(0, coordinator.activeEngineCount())
        assertTrue(factory.created.all { it.releaseCount == 1 })
        assertEquals(PlaybackState.Idle, coordinator.stateFor("one"))

        coordinator.onForeground()
        coordinator.onForeground()
        assertEquals(4, factory.created.size)
        assertEquals(2, coordinator.activeEngineCount())
        coordinator.leaveScreen()
    }

    @Test
    fun `fullscreen transition releases grid and owns only primary player`() = runTest {
        val factory = FakePlaybackEngineFactory()
        val coordinator = coordinator(factory)
        coordinator.showGrid(listOf(gridRequest("one", 11), gridRequest("two", 12)))
        val gridEngines = factory.created.toList()

        coordinator.showFullscreen(fullscreenRequest("one", 11))

        assertTrue(gridEngines.all { it.releaseCount == 1 })
        assertEquals(3, factory.created.size)
        assertEquals(1, coordinator.activeEngineCount())
        assertEquals(RtspStream.PRIMARY, factory.created.last().startedUri?.stream)
        coordinator.leaveScreen()
    }

    @Test
    fun `grid and fullscreen APIs enforce stream quality invariant`() = runTest {
        val coordinator = coordinator(FakePlaybackEngineFactory())

        assertThrows(IllegalArgumentException::class.java) {
            coordinator.showGrid(listOf(fullscreenRequest("one", 11)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            coordinator.showFullscreen(gridRequest("one", 11))
        }
        coordinator.leaveScreen()
    }

    @Test
    fun `connectivity loss releases resources and return reconnects`() = runTest {
        val factory = FakePlaybackEngineFactory()
        val coordinator = coordinator(factory)
        coordinator.showGrid(listOf(gridRequest("one", 11)))
        val first = factory.latest("one")

        coordinator.onConnectivityChanged(false)

        assertEquals(1, first.releaseCount)
        assertEquals(0, coordinator.activeEngineCount())
        assertEquals(PlaybackState.Offline, coordinator.stateFor("one"))

        coordinator.onConnectivityChanged(true)
        assertEquals(2, factory.created.size)
        assertEquals(1, coordinator.activeEngineCount())
        coordinator.leaveScreen()
    }

    @Test
    fun `missing camera route retries while a local transport remains available`() = runTest {
        val factory = FakePlaybackEngineFactory()
        val coordinator = coordinator(factory)
        coordinator.showGrid(listOf(gridRequest("one", 11)))
        val first = factory.latest("one")

        first.emit(PlaybackEngineEvent.Failed(PlaybackFailureReason.NETWORK_UNAVAILABLE))

        assertEquals(PlaybackState.Retrying(1, 1_000L), coordinator.stateFor("one"))
        advanceTimeBy(1_000L)
        runCurrent()
        assertEquals(2, factory.created.size)
        assertEquals(1, coordinator.activeEngineCount())
        coordinator.leaveScreen()
    }

    @Test
    fun `real network loss cancels a pending route retry until connectivity returns`() = runTest {
        val factory = FakePlaybackEngineFactory()
        val coordinator = coordinator(factory)
        coordinator.showGrid(listOf(gridRequest("one", 11)))
        factory
            .latest("one")
            .emit(PlaybackEngineEvent.Failed(PlaybackFailureReason.NETWORK_UNAVAILABLE))

        coordinator.onConnectivityChanged(false)
        advanceUntilIdle()

        assertEquals(1, factory.created.size)
        assertEquals(0, coordinator.activeEngineCount())
        assertEquals(PlaybackState.Offline, coordinator.stateFor("one"))

        coordinator.onConnectivityChanged(true)
        assertEquals(2, factory.created.size)
        assertEquals(1, coordinator.activeEngineCount())
        coordinator.leaveScreen()
    }

    @Test
    fun `authentication failure is terminal for one tile and leaves peers running`() = runTest {
        val factory = FakePlaybackEngineFactory()
        val coordinator = coordinator(factory)
        val requests = listOf(gridRequest("one", 11), gridRequest("two", 12))
        coordinator.showGrid(requests)
        val first = factory.latest("one")
        val second = factory.latest("two")
        second.emit(PlaybackEngineEvent.Live)

        first.emit(PlaybackEngineEvent.Failed(PlaybackFailureReason.AUTHENTICATION))
        coordinator.showGrid(requests)

        assertEquals(1, first.releaseCount)
        assertEquals(0, second.releaseCount)
        assertEquals(PlaybackState.AuthenticationFailed, coordinator.stateFor("one"))
        assertEquals(PlaybackState.Live, coordinator.stateFor("two"))
        assertEquals(1, coordinator.activeEngineCount())
        assertEquals(2, factory.created.size)
        coordinator.leaveScreen()
    }

    @Test
    fun `transient retry replaces only failed tile after delay`() = runTest {
        val factory = FakePlaybackEngineFactory()
        val coordinator = coordinator(factory)
        coordinator.showGrid(listOf(gridRequest("one", 11), gridRequest("two", 12)))
        val first = factory.latest("one")
        val second = factory.latest("two")

        first.emit(PlaybackEngineEvent.Failed(PlaybackFailureReason.TRANSIENT))

        assertEquals(PlaybackState.Retrying(1, 1_000L), coordinator.stateFor("one"))
        assertEquals(1, first.releaseCount)
        assertEquals(0, second.releaseCount)
        advanceTimeBy(999L)
        runCurrent()
        assertEquals(2, factory.created.size)

        advanceTimeBy(1L)
        runCurrent()
        assertEquals(3, factory.created.size)
        assertEquals(2, coordinator.activeEngineCount())
        assertEquals(0, second.releaseCount)
        coordinator.leaveScreen()
    }

    @Test
    fun `leaving screen cancels pending retries and ignores stale callbacks`() = runTest {
        val factory = FakePlaybackEngineFactory()
        val coordinator = coordinator(factory)
        coordinator.showGrid(listOf(gridRequest("one", 11)))
        val first = factory.latest("one")
        first.emit(PlaybackEngineEvent.Failed(PlaybackFailureReason.TRANSIENT))

        coordinator.leaveScreen()
        first.emit(PlaybackEngineEvent.Live)
        advanceUntilIdle()

        assertEquals(1, factory.created.size)
        assertEquals(0, coordinator.activeEngineCount())
        assertTrue(coordinator.states.value.isEmpty())
    }

    @Test
    fun `engine allocation failure is isolated to its tile`() = runTest {
        val factory = FakePlaybackEngineFactory(failingSlots = setOf("one"))
        val coordinator = coordinator(factory)

        coordinator.showGrid(listOf(gridRequest("one", 11), gridRequest("two", 12)))

        assertEquals(PlaybackState.DecoderResourceExhausted, coordinator.stateFor("one"))
        assertEquals(PlaybackState.Connecting, coordinator.stateFor("two"))
        assertEquals(1, coordinator.activeEngineCount())
        assertEquals(1, factory.created.size)
        coordinator.leaveScreen()
    }

    private fun kotlinx.coroutines.test.TestScope.coordinator(
        factory: PlaybackEngineFactory
    ): PlaybackCoordinator =
        PlaybackCoordinator(
            engineFactory = factory,
            scope = this,
            retryPolicy = RetryPolicy(jitter = RetryJitter { 0L }),
        )

    private fun gridRequest(slotId: String, finalOctet: Int): PlaybackRequest =
        request(slotId, finalOctet, RtspStream.SECONDARY)

    private fun fullscreenRequest(slotId: String, finalOctet: Int): PlaybackRequest =
        request(slotId, finalOctet, RtspStream.PRIMARY)

    private fun request(
        slotId: String,
        finalOctet: Int,
        stream: RtspStream,
    ): PlaybackRequest =
        PlaybackRequest(
            slotId = slotId,
            uri =
                uriFactory.create(
                    username = "viewer",
                    password = "password",
                    host = "192.168.1.$finalOctet",
                    stream = stream,
                ),
        )
}

private class FakePlaybackEngineFactory(private val failingSlots: Set<String> = emptySet()) :
    PlaybackEngineFactory {
    val created = mutableListOf<FakePlaybackEngine>()

    override fun create(slotId: String): PlaybackEngine {
        if (slotId in failingSlots) error("Synthetic allocation failure")
        return FakePlaybackEngine(slotId).also(created::add)
    }

    fun latest(slotId: String): FakePlaybackEngine = created.last { it.slotId == slotId }
}

private class FakePlaybackEngine(val slotId: String) : PlaybackEngine {
    var startCount = 0
        private set

    var releaseCount = 0
        private set

    var startedUri: RtspUri? = null
        private set

    private var listener: ((PlaybackEngineEvent) -> Unit)? = null

    override fun start(
        uri: RtspUri,
        listener: (PlaybackEngineEvent) -> Unit,
    ) {
        startCount += 1
        startedUri = uri
        this.listener = listener
    }

    override fun release() {
        releaseCount += 1
    }

    fun emit(event: PlaybackEngineEvent) {
        listener?.invoke(event)
    }
}
