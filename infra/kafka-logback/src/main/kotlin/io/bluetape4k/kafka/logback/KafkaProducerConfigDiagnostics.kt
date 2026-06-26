package io.bluetape4k.kafka.logback

/**
 * Formats Kafka producer configuration for Logback status output without
 * exposing credential-bearing values.
 */
internal object KafkaProducerConfigDiagnostics {

    private const val REDACTED = "[REDACTED]"

    private val sensitiveKeyFragments = listOf(
        "password",
        "secret",
        "token",
        "credential",
        "sasl.jaas.config",
        "keytab",
        "private.key",
        "access.key",
        "secret.key",
        "ssl.key",
        "oauthbearer",
        "basic.auth.user.info",
    )

    fun formatEntry(key: String, value: Any?): String =
        "key=$key, value=${redactValue(key, value)}"

    fun formatConfig(config: Map<String, Any?>): String =
        config.toSortedMap()
            .entries
            .joinToString(prefix = "{", postfix = "}") { (key, value) ->
                "$key=${redactValue(key, value)}"
            }

    fun formatMalformedPayload(keyValue: String): String =
        "payloadLength=${keyValue.length}"

    private fun redactValue(key: String, value: Any?): String =
        if (isSensitiveKey(key)) {
            REDACTED
        } else {
            value.toString()
        }

    private fun isSensitiveKey(key: String): Boolean {
        val normalized = key.lowercase()
        return sensitiveKeyFragments.any { fragment -> normalized.contains(fragment) }
    }
}
