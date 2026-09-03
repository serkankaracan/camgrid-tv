package io.github.serkankaracan.camgridtv.security

object SecretRedactor {
    // Greedy within one non-whitespace token so even malformed, unescaped '@' characters are
    // hidden.
    private val rtspUserInfo = Regex("(?i)\\brtsp://\\S+@")
    private val authorizationHeader =
        Regex("(?i)(authorization\\s*[:=]\\s*)(basic|digest)\\s+[^\\r\\n]+")
    private val namedSecret =
        Regex(
            "(?i)(\\b(?:password|passwd|pwd|username|user)\\b\\s*[:=]\\s*)(?:\"[^\"]*\"|'[^']*'|[^,;\\s}]+)"
        )

    fun redact(message: String, knownSecrets: Iterable<String> = emptyList()): String {
        var redacted =
            rtspUserInfo.replace(message) { result ->
                result.value.substring(0, result.value.indexOf("://") + 3) + "***:***@"
            }
        redacted = authorizationHeader.replace(redacted) { result -> result.groupValues[1] + "***" }
        redacted = namedSecret.replace(redacted) { result -> result.groupValues[1] + "***" }
        knownSecrets.filter(String::isNotEmpty).sortedByDescending(String::length).forEach { secret
            ->
            redacted = redacted.replace(secret, "***")
        }
        return redacted
    }
}
