package io.bluetape4k.redis.lettuce

import io.bluetape4k.redis.lettuce.LettuceConst.DEFAULT_HOST
import io.bluetape4k.redis.lettuce.LettuceConst.DEFAULT_PORT


/**
 * Lettuce Redis 연결 및 키 네이밍에 사용하는 공통 상수 모음입니다.
 *
 * ## 동작/계약
 * - 모든 값은 불변 상수이며 런타임 계산 없이 즉시 접근 가능합니다.
 * - 기본 URL은 [DEFAULT_HOST], [DEFAULT_PORT] 조합으로 구성됩니다.
 * - 모듈 전반에서 동일 기본값을 재사용하기 위한 용도로 사용됩니다.
 *
 * ```kotlin
 * val url = LettuceConst.DEFAULT_URL
 * // url == "redis://127.0.0.1:6379"
 * ```
 */
object LettuceConst {
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
