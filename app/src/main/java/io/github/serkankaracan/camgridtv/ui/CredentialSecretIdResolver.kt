package io.github.serkankaracan.camgridtv.ui

import io.github.serkankaracan.camgridtv.model.CredentialProfile

/** Resolves non-secret profile metadata to the encrypted-store identifier it references. */
internal object CredentialSecretIdResolver {
    fun resolve(
        profileId: String?,
        profiles: List<CredentialProfile>,
    ): String? = profileId?.let { id -> profiles.firstOrNull { it.id == id }?.secretId }
}
