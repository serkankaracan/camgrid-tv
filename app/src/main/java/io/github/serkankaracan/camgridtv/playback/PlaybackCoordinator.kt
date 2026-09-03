package io.github.serkankaracan.camgridtv.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns playback engines for one screen and provides explicit lifecycle/connectivity boundaries.
 *
 * Grid and fullscreen plans are deliberately separate modes. Moving between them releases every
 * engine before creating the new plan, which prevents retaining low-resolution grid players under a
 * fullscreen player. Each slot has an independent state, retry job and engine generation.
 */
class PlaybackCoordinator(
    private val engineFactory: PlaybackEngineFactory,
    private val scope: CoroutineScope,
    private val retryPolicy: RetryPolicy = RetryPolicy(),
    private val retryDelay: RetryDelay = RetryDelay { delay(it) },
    initiallyForeground: Boolean = true,
    initiallyNetworkAvailable: Boolean = true,
) {
    private val lock = Any()
    private val active = mutableMapOf<String, ActivePlayback>()
    private val retryJobs = mutableMapOf<String, Job>()
    private val retryAttempts = mutableMapOf<String, Int>()
    private val fallbackSlots = mutableSetOf<String>()
    private val mutableStates = MutableStateFlow<Map<String, PlaybackState>>(emptyMap())
    private val mutableEngineGenerations = MutableStateFlow<Map<String, Long>>(emptyMap())
    private var desiredRequests = linkedMapOf<String, PlaybackRequest>()
    private var mode = PlaybackMode.NONE
    private var foreground = initiallyForeground
    private var networkAvailable = initiallyNetworkAvailable
    private var nextGeneration = 0L

    val states: StateFlow<Map<String, PlaybackState>> = mutableStates.asStateFlow()
    val engineGenerations: StateFlow<Map<String, Long>> = mutableEngineGenerations.asStateFlow()

    fun showGrid(requests: List<PlaybackRequest>) {
        require(
            requests.all {
                it.uri.stream == RtspStream.SECONDARY && it.fallbackUri == null
            }
        ) {
            "Grid playback requires secondary streams"
        }
        setPlan(PlaybackMode.GRID, requests)
    }

    fun showFullscreen(request: PlaybackRequest) {
        require(
            request.uri.stream == RtspStream.PRIMARY &&
                request.fallbackUri?.stream == RtspStream.SECONDARY
        ) {
            "Fullscreen playback requires primary and secondary fallback streams"
        }
        setPlan(PlaybackMode.FULLSCREEN, listOf(request))
    }

    /** Clears the desired plan and permanently cancels its pending retries. */
    fun leaveScreen() {
        synchronized(lock) {
            releaseAllLocked()
            cancelAllRetriesLocked()
            retryAttempts.clear()
            fallbackSlots.clear()
            desiredRequests.clear()
            mode = PlaybackMode.NONE
            mutableStates.value = emptyMap()
        }
    }

    /** Call from the screen/process lifecycle before the app is no longer visible. */
    fun onBackground() {
        synchronized(lock) {
            if (!foreground) return
            foreground = false
            releaseAllLocked()
            cancelAllRetriesLocked()
            retryAttempts.clear()
            updateAllDesiredStatesLocked(PlaybackEvent.StopRequested)
        }
    }

    /** Recreates only the engines in the current desired plan. */
    fun onForeground() {
        synchronized(lock) {
            foreground = true
            if (networkAvailable) {
                reconcileLocked()
            } else {
                updateAllDesiredStatesLocked(PlaybackEvent.NetworkLost)
            }
        }
    }

    /**
     * Call from a route-aware connectivity observer. A repeated `true` is also a reconnect signal
     * for slots that were waiting without an engine.
     */
    fun onConnectivityChanged(available: Boolean) {
        synchronized(lock) {
            networkAvailable = available
            if (!available) {
                releaseAllLocked()
                cancelAllRetriesLocked()
                retryAttempts.clear()
                updateAllDesiredStatesLocked(PlaybackEvent.NetworkLost)
            } else if (foreground) {
                reconcileLocked()
            }
        }
    }

    fun stateFor(slotId: String): PlaybackState =
        synchronized(lock) { mutableStates.value[slotId] ?: PlaybackState.Idle }

    fun activeEngineCount(): Int = synchronized(lock) { active.size }

    /** Lets a rendering adapter obtain the engine without transferring ownership. */
    fun activeEngineFor(slotId: String): PlaybackEngine? =
        synchronized(lock) { active[slotId]?.engine }

    private fun setPlan(
        newMode: PlaybackMode,
        requests: List<PlaybackRequest>,
    ) {
        val newRequests = linkedMapOf<String, PlaybackRequest>()
        requests.forEach { request ->
            require(newRequests.put(request.slotId, request) == null) {
                "Playback plan contains duplicate slot identifiers"
            }
        }

        synchronized(lock) {
            val modeChanged = mode != newMode
            if (modeChanged) {
                releaseAllLocked()
                cancelAllRetriesLocked()
                retryAttempts.clear()
                fallbackSlots.clear()
            } else {
                val removedOrChanged =
                    desiredRequests.keys.filter { slotId ->
                        newRequests[slotId] != desiredRequests[slotId]
                    }
                removedOrChanged.forEach { slotId ->
                    releaseActiveLocked(slotId)
                    cancelRetryLocked(slotId)
                    retryAttempts.remove(slotId)
                    fallbackSlots.remove(slotId)
                }
            }

            val previousRequests = desiredRequests
            desiredRequests = newRequests
            mode = newMode
            val priorStates = mutableStates.value
            mutableStates.value = newRequests.mapValues { (slotId, request) ->
                if (!modeChanged && previousRequests[slotId] == request) {
                    priorStates[slotId] ?: PlaybackState.Idle
                } else {
                    PlaybackState.Idle
                }
            }

            when {
                !foreground -> updateAllDesiredStatesLocked(PlaybackEvent.StopRequested)
                !networkAvailable -> updateAllDesiredStatesLocked(PlaybackEvent.NetworkLost)
                else -> reconcileLocked()
            }
        }
    }

    private fun reconcileLocked() {
        desiredRequests.values.forEach { request ->
            if (active[request.slotId]?.request == request) return@forEach
            if (retryJobs.containsKey(request.slotId)) return@forEach
            val state = mutableStates.value[request.slotId] ?: PlaybackState.Idle
            if (state != PlaybackState.Idle && state != PlaybackState.Offline) return@forEach
            startLocked(request)
        }
    }

    private fun startLocked(request: PlaybackRequest) {
        releaseActiveLocked(request.slotId)
        val generation = ++nextGeneration
        val engine =
            try {
                engineFactory.create(request.slotId)
            } catch (_: Throwable) {
                setStateLocked(
                    request.slotId,
                    PlaybackEvent.Failed(PlaybackFailureReason.DECODER_RESOURCE_EXHAUSTED),
                )
                return
            }

        val activeUri =
            if (request.slotId in fallbackSlots) {
                checkNotNull(request.fallbackUri)
            } else {
                request.uri
            }
        active[request.slotId] = ActivePlayback(request, activeUri, engine, generation)
        mutableEngineGenerations.value =
            mutableEngineGenerations.value + (request.slotId to generation)
        setStateLocked(request.slotId, PlaybackEvent.StartRequested)
        try {
            engine.start(activeUri) { event ->
                onEngineEvent(request.slotId, generation, event)
            }
        } catch (_: Throwable) {
            val current = active[request.slotId]
            if (current?.generation == generation) {
                active.remove(request.slotId)
                mutableEngineGenerations.value = mutableEngineGenerations.value - request.slotId
                releaseSafely(engine)
                if (!activateFallbackLocked(current, PlaybackFailureReason.FATAL)) {
                    handleFailureLocked(request, PlaybackFailureReason.FATAL)
                }
            }
        }
    }

    private fun onEngineEvent(
        slotId: String,
        generation: Long,
        event: PlaybackEngineEvent,
    ) {
        synchronized(lock) {
            val current = active[slotId]
            if (current?.generation != generation) return

            when (event) {
                PlaybackEngineEvent.Connecting ->
                    setStateLocked(slotId, PlaybackEvent.StartRequested)
                PlaybackEngineEvent.Live -> {
                    retryAttempts.remove(slotId)
                    setStateLocked(slotId, PlaybackEvent.Ready)
                }
                is PlaybackEngineEvent.Failed -> {
                    active.remove(slotId)
                    mutableEngineGenerations.value = mutableEngineGenerations.value - slotId
                    releaseSafely(current.engine)
                    if (!activateFallbackLocked(current, event.reason)) {
                        handleFailureLocked(current.request, event.reason)
                    }
                }
            }
        }
    }

    private fun activateFallbackLocked(
        failedPlayback: ActivePlayback,
        reason: PlaybackFailureReason,
    ): Boolean {
        val request = failedPlayback.request
        request.fallbackUri ?: return false
        if (failedPlayback.uri != request.uri || !reason.allowsStreamFallback()) return false

        fallbackSlots += request.slotId
        retryAttempts.remove(request.slotId)
        startLocked(request)
        return true
    }

    private fun handleFailureLocked(
        request: PlaybackRequest,
        reason: PlaybackFailureReason,
    ) {
        setStateLocked(request.slotId, PlaybackEvent.Failed(reason))
        val attempt = (retryAttempts[request.slotId] ?: 0) + 1
        val retryReason =
            if (reason == PlaybackFailureReason.NETWORK_UNAVAILABLE && networkAvailable) {
                // The device still has a local transport, but the camera route may be changing.
                // Poll with the bounded transient cadence so a same-valued connectivity snapshot
                // is not required to wake this slot. A later global network-loss callback cancels
                // the scheduled job and switches all slots to Offline.
                PlaybackFailureReason.TRANSIENT
            } else {
                reason
            }
        when (val decision = retryPolicy.decisionFor(retryReason, attempt)) {
            RetryDecision.DoNotRetry -> Unit
            RetryDecision.WaitForConnectivity -> Unit
            is RetryDecision.RetryAfter -> {
                retryAttempts[request.slotId] = decision.attempt
                setStateLocked(
                    request.slotId,
                    PlaybackEvent.RetryScheduled(decision.attempt, decision.delayMillis),
                )
                scheduleRetryLocked(request, decision.delayMillis)
            }
        }
    }

    private fun scheduleRetryLocked(
        request: PlaybackRequest,
        delayMillis: Long,
    ) {
        cancelRetryLocked(request.slotId)
        val job =
            scope.launch(start = CoroutineStart.LAZY) {
                retryDelay.await(delayMillis)
                val thisJob = currentCoroutineContext()[Job]
                synchronized(lock) {
                    if (retryJobs[request.slotId] !== thisJob) return@synchronized
                    retryJobs.remove(request.slotId)
                    if (
                        foreground && networkAvailable && desiredRequests[request.slotId] == request
                    ) {
                        startLocked(request)
                    }
                }
            }
        retryJobs[request.slotId] = job
        job.invokeOnCompletion {
            synchronized(lock) {
                if (retryJobs[request.slotId] === job) {
                    retryJobs.remove(request.slotId)
                }
            }
        }
        job.start()
    }

    private fun releaseActiveLocked(slotId: String) {
        active.remove(slotId)?.engine?.let { engine ->
            mutableEngineGenerations.value = mutableEngineGenerations.value - slotId
            releaseSafely(engine)
        }
    }

    private fun releaseAllLocked() {
        val engines = active.values.map(ActivePlayback::engine)
        active.clear()
        if (mutableEngineGenerations.value.isNotEmpty()) {
            mutableEngineGenerations.value = emptyMap()
        }
        engines.forEach(::releaseSafely)
    }

    private fun cancelRetryLocked(slotId: String) {
        retryJobs.remove(slotId)?.cancel()
    }

    private fun cancelAllRetriesLocked() {
        val jobs = retryJobs.values.toList()
        retryJobs.clear()
        jobs.forEach(Job::cancel)
    }

    private fun updateAllDesiredStatesLocked(event: PlaybackEvent) {
        desiredRequests.keys.forEach { slotId -> setStateLocked(slotId, event) }
    }

    private fun setStateLocked(
        slotId: String,
        event: PlaybackEvent,
    ) {
        if (slotId !in desiredRequests) return
        val oldState = mutableStates.value[slotId] ?: PlaybackState.Idle
        mutableStates.value =
            mutableStates.value + (slotId to PlaybackStateReducer.reduce(oldState, event))
    }

    private fun releaseSafely(engine: PlaybackEngine) {
        try {
            engine.release()
        } catch (_: Throwable) {
            // Release is best effort; no raw exception or endpoint detail may leave this boundary.
        }
    }

    private data class ActivePlayback(
        val request: PlaybackRequest,
        val uri: RtspUri,
        val engine: PlaybackEngine,
        val generation: Long,
    )

    private enum class PlaybackMode {
        NONE,
        GRID,
        FULLSCREEN,
    }
}

private fun PlaybackFailureReason.allowsStreamFallback(): Boolean =
    when (this) {
        PlaybackFailureReason.UNSUPPORTED_STREAM,
        PlaybackFailureReason.DECODER_RESOURCE_EXHAUSTED,
        PlaybackFailureReason.TRANSIENT,
        PlaybackFailureReason.FATAL -> true
        PlaybackFailureReason.AUTHENTICATION,
        PlaybackFailureReason.NETWORK_UNAVAILABLE -> false
    }
