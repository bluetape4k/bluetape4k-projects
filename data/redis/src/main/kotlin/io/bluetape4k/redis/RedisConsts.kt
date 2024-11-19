package io.bluetape4k.redis

object RedisConst {
    const val OK: String = "OK"

    const val DEFAULT_HOST = "127.0.0.1"
    const val DEFAULT_PORT = 6379
    const val DEFAULT_URL = "redis://${DEFAULT_HOST}:${DEFAULT_PORT}"

    const val DEFAULT_SENTINEL_PORT = 26379
    const val DEFAULT_TIMEOUT_MILLIS: Long = 30_000L
    const val DEFAULT_DATABASE = 0

    const val DEFAULT_CHARSET = "UTF-8"
    const val DEFAULT_LOGBACK_CHANNEL = "channel:logback:logs"
    const val DEFAULT_DELIMETER = ":"
}
