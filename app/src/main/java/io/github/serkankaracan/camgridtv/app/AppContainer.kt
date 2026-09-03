package io.github.serkankaracan.camgridtv.app

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import io.github.serkankaracan.camgridtv.R
import io.github.serkankaracan.camgridtv.data.DataStoreCameraSelectionRepository
import io.github.serkankaracan.camgridtv.data.DataStoreEncryptedCredentialRepository
import io.github.serkankaracan.camgridtv.discovery.AndroidConnectivityMonitor
import io.github.serkankaracan.camgridtv.discovery.AndroidMulticastWsDiscoveryClient
import io.github.serkankaracan.camgridtv.discovery.DefaultDiscoveryRepository
import io.github.serkankaracan.camgridtv.discovery.DiscoveredOnvifDeviceFactory
import io.github.serkankaracan.camgridtv.discovery.LocalNetworkPermissionCoordinator
import io.github.serkankaracan.camgridtv.playback.Media3PlaybackEngineFactory
import io.github.serkankaracan.camgridtv.playback.RtspUriFactory
import io.github.serkankaracan.camgridtv.security.AndroidKeystoreCredentialCipher
import io.github.serkankaracan.camgridtv.security.EncryptedCredentialSecretStore
import io.github.serkankaracan.camgridtv.security.SafeCredentialRecovery
import io.github.serkankaracan.camgridtv.util.AndroidLocalNetworkRouteResolver
import io.github.serkankaracan.camgridtv.util.Rfc1918LocalAddressPolicy

private val Context.camGridDataStore by preferencesDataStore(name = "camgrid_local_state")

class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext
    private val dataStore = applicationContext.camGridDataStore
    private val localNetworkRouteResolver = AndroidLocalNetworkRouteResolver(applicationContext)

    val cameraSelectionRepository = DataStoreCameraSelectionRepository(dataStore)
    private val credentialCipher =
        AndroidKeystoreCredentialCipher(
            keyAlias = AndroidKeystoreCredentialCipher.DEFAULT_KEY_ALIAS
        )
    val credentialSecretStore =
        EncryptedCredentialSecretStore(
            cipher = credentialCipher,
            repository = DataStoreEncryptedCredentialRepository(dataStore),
        )
    val credentialRecovery =
        SafeCredentialRecovery(
            keyInvalidator = credentialCipher,
            secretStore = credentialSecretStore,
            resetCredentialProfileLinks = cameraSelectionRepository::clearCredentialProfiles,
        )
    val discoveryRepository =
        DefaultDiscoveryRepository(
            client = AndroidMulticastWsDiscoveryClient(applicationContext),
            deviceFactory =
                DiscoveredOnvifDeviceFactory(
                    genericCameraNameProvider = {
                        applicationContext.getString(R.string.generic_onvif_camera)
                    }
                ),
        )
    val connectivityMonitor = AndroidConnectivityMonitor(applicationContext)
    val permissionCoordinator = LocalNetworkPermissionCoordinator(applicationContext)
    val playbackEngineFactory =
        Media3PlaybackEngineFactory(
            context = applicationContext,
            localNetworkRouteResolver = localNetworkRouteResolver,
        )
    val rtspUriFactory = RtspUriFactory(Rfc1918LocalAddressPolicy())
}
