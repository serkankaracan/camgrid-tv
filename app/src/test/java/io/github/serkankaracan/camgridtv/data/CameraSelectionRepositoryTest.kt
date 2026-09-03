package io.github.serkankaracan.camgridtv.data

import io.github.serkankaracan.camgridtv.model.CameraDevice
import io.github.serkankaracan.camgridtv.model.CredentialProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraSelectionRepositoryTest {
    private val first = camera("first")
    private val second = camera("second")
    private val third = camera("third")

    @Test
    fun `persists deterministic selection order`() = runTest {
        val repository = InMemoryCameraSelectionRepository()
        repository.upsertCamera(first)
        repository.upsertCamera(second)
        repository.upsertCamera(third)

        repository.setSelectedCameraIds(listOf("third", "first"))

        val configuration = repository.configuration.first()
        assertEquals(
            listOf("third", "first"),
            configuration.selectedCameras().map(CameraDevice::id),
        )
        assertFalse(configuration.cameras.single { it.id == "second" }.selected)
    }

    @Test
    fun `renames a camera without changing its stable identity`() = runTest {
        val repository = InMemoryCameraSelectionRepository()
        repository.upsertCamera(first)

        repository.renameCamera("first", "Front door")

        val stored = repository.current().cameras.single()
        assertEquals("first", stored.id)
        assertEquals("Front door", stored.displayName)
        assertEquals(first.endpointUuid, stored.endpointUuid)
    }

    @Test
    fun `shared profile can be assigned and safely detached`() = runTest {
        val repository = InMemoryCameraSelectionRepository()
        val profile =
            CredentialProfile(
                id = "shared",
                displayName = "Shared camera account",
                createdAtEpochMillis = 1L,
            )
        repository.upsertCredentialProfile(profile)
        repository.upsertCamera(first)
        repository.assignCredentialProfile("first", "shared")
        assertEquals("shared", repository.current().cameras.single().credentialProfileId)

        repository.removeCredentialProfile("shared")

        assertTrue(repository.current().credentialProfiles.isEmpty())
        assertNull(repository.current().cameras.single().credentialProfileId)
    }

    private fun camera(id: String) =
        CameraDevice(
            id = id,
            endpointUuid = "11111111-2222-4333-8444-$id".take(36),
            displayName = "Camera $id",
            host = "192.168.50.100",
            lastSeenEpochMillis = 1L,
        )
}
