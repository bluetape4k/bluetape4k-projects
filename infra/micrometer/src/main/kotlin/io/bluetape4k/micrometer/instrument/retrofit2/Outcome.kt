package io.bluetape4k.micrometer.instrument.retrofit2

enum class Outcome(private val code: Int) {
    UNKNOWN(0),
    INFORMATION(1),
    SUCCESS(2),
    REDIRECTION(3),
    CLIENT_ERROR(4),
    SERVER_ERROR(5);

    companion object {
        @JvmStatic
        fun fromHttpStatus(statusCode: Int): Outcome =
            Outcome.entries.firstOrNull { it.code == statusCode / 100 } ?: UNKNOWN
    }
}
