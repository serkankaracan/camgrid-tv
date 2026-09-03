package io.github.serkankaracan.camgridtv.data

import io.github.serkankaracan.camgridtv.model.CameraConfiguration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryCameraSelectionRepository(
    initialConfiguration: CameraConfiguration = CameraConfiguration()
) : CameraSelectionRepository {
    private val mutex = Mutex()
    private val mutableConfiguration = MutableStateFlow(initialConfiguration)

    override val configuration: StateFlow<CameraConfiguration> = mutableConfiguration.asStateFlow()

    override suspend fun update(transform: (CameraConfiguration) -> CameraConfiguration) {
        mutex.withLock {
            mutableConfiguration.value = transform(mutableConfiguration.value)
        }
    }
}
