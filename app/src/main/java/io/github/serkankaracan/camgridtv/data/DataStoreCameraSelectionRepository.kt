package io.github.serkankaracan.camgridtv.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.serkankaracan.camgridtv.model.CameraConfiguration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DataStoreCameraSelectionRepository(
    private val dataStore: DataStore<Preferences>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : CameraSelectionRepository {
    override val configuration: Flow<CameraConfiguration> =
        dataStore.data
            .map { preferences ->
                preferences[CONFIGURATION_KEY]?.let(CameraConfigurationCodec::decode)
                    ?: CameraConfiguration()
            }
            .distinctUntilChanged()

    override suspend fun update(transform: (CameraConfiguration) -> CameraConfiguration) {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                val current =
                    preferences[CONFIGURATION_KEY]?.let(CameraConfigurationCodec::decode)
                        ?: CameraConfiguration()
                preferences[CONFIGURATION_KEY] = CameraConfigurationCodec.encode(transform(current))
            }
        }
    }

    private companion object {
        val CONFIGURATION_KEY = stringPreferencesKey("camera_configuration_v1")
    }
}
