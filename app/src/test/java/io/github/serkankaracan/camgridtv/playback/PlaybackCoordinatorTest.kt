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
    fun `an initially stopped lifecycle does not create players before foreground`() = runTest {
        val factory = FakePlaybackEngineFactory()
        val coordinator = coordinator(factory, initiallyForeground = false)

        coordinator.showGrid(listOf(gridRequest("one", 11)))

        assertEquals(0, coordinator.activeEngineCount())
        assertTrue(factory.created.isEmpty())

        coordinator.onForeground()

        assertEquals(1, coordinator.activeEngineCount())
        assertEquals(1, factory.created.size)
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
    fun `fullscreen falls back immediately when primary stream is incompatible`() = runTest {
        listOf(
                PlaybackFailureReason.UNSUPPORTED_STREAM,
                PlaybackFailureReason.DECODER_RESOURCE_EXHAUSTED,
                PlaybackFailureReason.TRANSIENT,
                PlaybackFailureReason.FATAL,
            )
            .forEachIndexed { index, reason ->
                val slotId = "camera-$index"
                val factory = FakePlaybackEngineFactory()
                val coordinator = coordinator(factory)

                coordinator.showFullscreen(fullscreenRequest(slotId, 20 + index))
                val primary = factory.latest(slotId)
                val primaryGeneration = coordinator.engineGenerations.value.getValue(slotId)
                primary.emit(PlaybackEngineEvent.Failed(reason))

                assertEquals(1, primary.releaseCount)
                assertEquals(2, factory.created.size)
                assertTrue(coordinator.engineGenerations.value.getValue(slotId) > primaryGeneration)
                assertEquals(RtspStream.SECONDARY, factory.latest(slotId).startedUri?.stream)
                assertEquals(1, coordinator.activeEngineCount())

                factory.latest(slotId).emit(PlaybackEngineEvent.Live)
                assertEquals(PlaybackState.Live, coordinator.stateFor(slotId))
                coordinator.leaveScreen()
            }
    }

    @Test
    fun `fullscreen authentication and missing route failures never use fallback`() = runTest {
        val authenticationFactory = FakePlaybackEngineFactory()
        val authenticationCoordinator = coordinator(authenticationFactory)
        authenticationCoordinator.showFullscreen(fullscreenRequest("auth", 31))

        authenticationFactory
            .latest("auth")
            .emit(PlaybackEngineEvent.Failed(PlaybackFailureReason.AUTHENTICATION))

        assertEquals(1, authenticationFactory.created.size)
        assertEquals(PlaybackState.AuthenticationFailed, authenticationCoordinator.stateFor("auth"))
        authenticationCoordinator.leaveScreen()

        val routeFactory = FakePlaybackEngineFactory()
        val routeCoordinator = coordinator(routeFactory)
        routeCoordinator.showFullscreen(fullscreenRequest("route", 32))

        routeFactory
            .latest("route")
            .emit(PlaybackEngineEvent.Failed(PlaybackFailureReason.NETWORK_UNAVAILABLE))

        assertEquals(1, routeFactory.created.size)
        assertEquals(PlaybackState.Retrying(1, 1_000L), routeCoordinator.stateFor("route"))
        routeCoordinator.leaveScreen()
    }

    @Test
    fun `fallback retry stays on secondary stream`() = runTest {
        val factory = FakePlaybackEngineFactory()
        val coordinator = coordinator(factory)
        coordinator.showFullscreen(fullscreenRequest("one", 41))

        factory.latest("one").emit(PlaybackEngineEvent.Failed(PlaybackFailureReason.TRANSIENT))
        factory.latest("one").emit(PlaybackEngineEvent.Failed(PlaybackFailureReason.TRANSIENT))

        assertEquals(PlaybackState.Retrying(1, 1_000L), coordinator.stateFor("one"))
        advanceTimeBy(1_000L)
        runCurrent()

        assertEquals(
            listOf(RtspStream.PRIMARY, RtspStream.SECONDARY, RtspStream.SECONDARY),
            factory.created.map { it.startedUri?.stream },
        )
        assertEquals(1, coordinator.activeEngineCount())
        coordinator.leaveScreen()
    }

    @Test
    fun `new fullscreen session tries primary again after fallback`() = runTest {
        val factory = FakePlaybackEngineFactory()
        val coordinator = coordinator(factory)
        val request = fullscreenRequest("one", 51)
        coordinator.showFullscreen(request)
        factory.latest("one").emit(PlaybackEngineEvent.Failed(PlaybackFailureReason.TRANSIENT))
        assertEquals(RtspStream.SECONDARY, factory.latest("one").startedUri?.stream)

        coordinator.leaveScreen()
        coordinator.showFullscreen(request)

        assertEquals(RtspStream.PRIMARY, factory.latest("one").startedUri?.stream)
        coordinator.leaveScreen()
    }

    @Test
    fun `reapplying same fullscreen plan preserves active fallback`() = runTest {
        val factory = FakePlaybackEngineFactory()
        val coordinator = coordinator(factory)
        val request = fullscreenRequest("one", 52)
        coordinator.showFullscreen(request)
        factory.latest("one").emit(PlaybackEngineEvent.Failed(PlaybackFailureReason.TRANSIENT))
        val fallback = factory.latest("one")

        coordinator.showFullscreen(request)

        assertEquals(2, factory.created.size)
        assertEquals(0, fallback.releaseCount)
        assertEquals(RtspStream.SECONDARY, fallback.startedUri?.stream)
        coordinator.leaveScreen()
    }

    @Test
    fun `connectivity recovery preserves fullscreen fallback quality`() = runTest {
        val factory = FakePlaybackEngineFactory()
        val coordinator = coordinator(factory)
        coordinator.showFullscreen(fullscreenRequest("one", 53))
        factory.latest("one").emit(PlaybackEngineEvent.Failed(PlaybackFailureReason.TRANSIENT))
        factory.latest("one").emit(PlaybackEngineEvent.Live)

        coordinator.onConnectivityChanged(false)
        coordinator.onConnectivityChanged(true)

        assertEquals(RtspStream.SECONDARY, factory.latest("one").startedUri?.stream)
        assertEquals(1, coordinator.activeEngineCount())
        coordinator.leaveScreen()
    }

    @Test
    fun `background recovery preserves fullscreen fallback quality`() = runTest {
        val factory = FakePlaybackEngineFactory()
        val coordinator = coordinator(factory)
        coordinator.showFullscreen(fullscreenRequest("one", 54))
        factory.latest("one").emit(PlaybackEngineEvent.Failed(PlaybackFailureReason.TRANSIENT))
        factory.latest("one").emit(PlaybackEngineEvent.Live)

        coordinator.onBackground()
        coordinator.onForeground()

        assertEquals(RtspStream.SECONDARY, factory.latest("one").startedUri?.stream)
        assertEquals(1, coordinator.activeEngineCount())
        coordinator.leaveScreen()
    }

    @Test
    fun `stale primary callbacks cannot replace active fullscreen fallback`() = runTest {
        val factory = FakePlaybackEngineFactory()
        val coordinator = coordinator(factory)
        coordinator.showFullscreen(fullscreenRequest("one", 55))
        val primary = factory.latest("one")
        primary.emit(PlaybackEngineEvent.Failed(PlaybackFailureReason.TRANSIENT))
        val fallback = factory.latest("one")

        primary.emit(PlaybackEngineEvent.Live)
        primary.emit(PlaybackEngineEvent.Failed(PlaybackFailureReason.FATAL))

        assertEquals(2, factory.created.size)
        assertEquals(0, fallback.releaseCount)
        assertEquals(PlaybackState.Connecting, coordinator.stateFor("one"))
        assertEquals(RtspStream.SECONDARY, fallback.startedUri?.stream)
        coordinator.leaveScreen()
    }

    @Test
    fun `synchronous primary start failure uses fullscreen fallback`() = runTest {
        val factory = ThrowFirstStartPlaybackEngineFactory()
        val coordinator = coordinator(factory)

        coordinator.showFullscreen(fullscreenRequest("one", 56))

        assertEquals(2, factory.created.size)
        assertEquals(1, factory.created.first().releaseCount)
        assertEquals(RtspStream.SECONDARY, factory.created.last().startedUri?.stream)
        assertEquals(1, coordinator.activeEngineCount())
        assertEquals(setOf("one"), coordinator.engineGenerations.value.keys)
        coordinator.leaveScreen()
        assertTrue(coordinator.engineGenerations.value.isEmpty())
    }

    @Test
    fun `returning from fullscreen fallback recreates every grid stream as secondary`() = runTest {
        val factory = FakePlaybackEngineFactory()
        val coordinator = coordinator(factory)
        coordinator.showFullscreen(fullscreenRequest("one", 61))
        factory.latest("one").emit(PlaybackEngineEvent.Failed(PlaybackFailureReason.TRANSIENT))
        val fullscreenFallback = factory.latest("one")

        coordinator.showGrid(listOf(gridRequest("one", 61), gridRequest("two", 62)))

        assertEquals(1, fullscreenFallback.releaseCount)
        assertEquals(2, coordinator.activeEngineCount())
        assertEquals(
            listOf(RtspStream.SECONDARY, RtspStream.SECONDARY),
            factory.created.takeLast(2).map { it.startedUri?.stream },
        )
        coordinator.leaveScreen()
    }

    @Test
    fun `returning to grid cancels pending fallback retry`() = runTest {
        val factory = FakePlaybackEngineFactory()
        val coordinator = coordinator(factory)
        coordinator.showFullscreen(fullscreenRequest("one", 63))
        factory.latest("one").emit(PlaybackEngineEvent.Failed(PlaybackFailureReason.TRANSIENT))
        factory.latest("one").emit(PlaybackEngineEvent.Failed(PlaybackFailureReason.TRANSIENT))

        coordinator.showGrid(listOf(gridRequest("one", 63), gridRequest("two", 64)))
        advanceUntilIdle()

        assertEquals(4, factory.created.size)
        assertEquals(2, coordinator.activeEngineCount())
        assertEquals(
            listOf(RtspStream.SECONDARY, RtspStream.SECONDARY),
            factory.created.takeLast(2).map { it.startedUri?.stream },
        )
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
    fun `tile retry changes only its generation and terminal failure removes it`() = runTest {
        val factory = FakePlaybackEngineFactory()
        val coordinator = coordinator(factory)
        coordinator.showGrid(listOf(gridRequest("one", 11), gridRequest("two", 12)))
        val firstGeneration = coordinator.engineGenerations.value.getValue("one")
        val peerGeneration = coordinator.engineGenerations.value.getValue("two")

        factory.latest("one").emit(PlaybackEngineEvent.Failed(PlaybackFailureReason.TRANSIENT))
        advanceTimeBy(1_000L)
        runCurrent()

        assertTrue(coordinator.engineGenerations.value.getValue("one") > firstGeneration)
        assertEquals(peerGeneration, coordinator.engineGenerations.value.getValue("two"))

        factory.latest("one").emit(PlaybackEngineEvent.Failed(PlaybackFailureReason.AUTHENTICATION))

        assertEquals(setOf("two"), coordinator.engineGenerations.value.keys)
        assertEquals(peerGeneration, coordinator.engineGenerations.value.getValue("two"))
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
        factory: PlaybackEngineFactory,
        initiallyForeground: Boolean = true,
    ): PlaybackCoordinator =
        PlaybackCoordinator(
            engineFactory = factory,
            scope = this,
            retryPolicy = RetryPolicy(jitter = RetryJitter { 0L }),
            initiallyForeground = initiallyForeground,
        )

    private fun gridRequest(slotId: String, finalOctet: Int): PlaybackRequest =
        request(slotId, finalOctet, RtspStream.SECONDARY)

    private fun fullscreenRequest(slotId: String, finalOctet: Int): PlaybackRequest =
        PlaybackRequest(
            slotId = slotId,
            uri = uri(finalOctet, RtspStream.PRIMARY),
            fallbackUri = uri(finalOctet, RtspStream.SECONDARY),
        )

    private fun request(
        slotId: String,
        finalOctet: Int,
        stream: RtspStream,
    ): PlaybackRequest =
        PlaybackRequest(
            slotId = slotId,
            uri = uri(finalOctet, stream),
        )

    private fun uri(finalOctet: Int, stream: RtspStream): RtspUri =
        uriFactory.create(
            username = "viewer",
            password = "password",
            host = "192.168.1.$finalOctet",
            stream = stream,
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

private class ThrowFirstStartPlaybackEngineFactory : PlaybackEngineFactory {
    val created = mutableListOf<FakePlaybackEngine>()

    override fun create(slotId: String): PlaybackEngine =
        FakePlaybackEngine(slotId, throwOnStart = created.isEmpty()).also(created::add)
}

private class FakePlaybackEngine(
    val slotId: String,
    private val throwOnStart: Boolean = false,
) : PlaybackEngine {
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
        if (throwOnStart) error("Synthetic start failure")
    }

    override fun release() {
        releaseCount += 1
    }

    fun emit(event: PlaybackEngineEvent) {
        listener?.invoke(event)
    }
}
