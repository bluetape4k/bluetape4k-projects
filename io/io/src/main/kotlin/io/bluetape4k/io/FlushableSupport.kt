package io.bluetape4k.io

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.error
import java.io.Flushable
import java.io.IOException

private val log by lazy { KotlinLogging.logger { } }

/**
 * [Flushable]를 조용히 flush합니다. IOException이 발생해도 예외를 전파하지 않고 로그만 남깁니다.
 *
 * ```
 * outputStream.flushQuietly()
 * ```
 */
fun Flushable.flushQuietly() {
    try {
        flush()
    } catch (e: IOException) {
        log.error(e) { "Fail to flush. $this" }
    }
}
