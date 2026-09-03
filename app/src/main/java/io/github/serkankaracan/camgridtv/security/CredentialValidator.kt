package io.github.serkankaracan.camgridtv.security

object CredentialValidator {
    const val MAX_USERNAME_LENGTH = 256
    const val MAX_PASSWORD_LENGTH = 2_048

    fun validate(username: String, password: CharArray): CredentialValidationResult {
        val issues = buildSet {
            if (username.isBlank()) add(CredentialValidationIssue.USERNAME_REQUIRED)
            if (username.length > MAX_USERNAME_LENGTH)
                add(CredentialValidationIssue.USERNAME_TOO_LONG)
            if (username.any(::isUnsafeControlCharacter)) {
                add(CredentialValidationIssue.USERNAME_CONTAINS_CONTROL_CHARACTER)
            }
            if (password.isEmpty()) add(CredentialValidationIssue.PASSWORD_REQUIRED)
            if (password.size > MAX_PASSWORD_LENGTH)
                add(CredentialValidationIssue.PASSWORD_TOO_LONG)
            if (password.any(::isUnsafeControlCharacter)) {
                add(CredentialValidationIssue.PASSWORD_CONTAINS_CONTROL_CHARACTER)
            }
        }
        return if (issues.isEmpty()) {
            CredentialValidationResult.Valid
        } else {
            CredentialValidationResult.Invalid(issues)
        }
    }

    private fun isUnsafeControlCharacter(character: Char): Boolean =
        character.code in 0x00..0x1F || character.code == 0x7F
}

sealed interface CredentialValidationResult {
    data object Valid : CredentialValidationResult

    data class Invalid(val issues: Set<CredentialValidationIssue>) : CredentialValidationResult
}

enum class CredentialValidationIssue {
    USERNAME_REQUIRED,
    USERNAME_TOO_LONG,
    USERNAME_CONTAINS_CONTROL_CHARACTER,
    PASSWORD_REQUIRED,
    PASSWORD_TOO_LONG,
    PASSWORD_CONTAINS_CONTROL_CHARACTER,
}
