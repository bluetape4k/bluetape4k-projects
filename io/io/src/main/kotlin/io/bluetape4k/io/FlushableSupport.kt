package io.bluetape4k.io

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.error
import java.io.Flushable
import java.io.IOException

private val log by lazy { KotlinLogging.logger { } }

/**
 * Flush the [Flushable] quietly.
 */
fun Flushable.flushQuietly() {
    try {
        flush()
    } catch (e: IOException) {
        log.error(e) { "Fail to flush. $this" }
    }
}
