package io.nats.examples.jetstream

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.nats.client.natsMessageOf
import io.bluetape4k.support.toUtf8Bytes
import io.nats.client.JetStream

private val log = KotlinLogging.logger { }

fun JetStream.publish(
    subject: String,
    prefix: String = "data",
    times: Int = 1,
    msgSize: Int,
    verbose: Boolean,
) {
    if (verbose) {
        log.debug { "Publish ->" }
    }
    repeat(times) { index ->
        val data = makeData(prefix, msgSize, verbose, index)
        val msg = natsMessageOf(subject, data)
        publish(msg)
    }
    if (verbose) {
        log.debug { " <-" }
    }
}

fun makeData(
    prefix: String,
    msgSize: Int,
    verbose: Boolean,
    index: Int,
): ByteArray? {
    if (msgSize == 0) {
        return null
    }

    val text = "$prefix-$index."
    if (verbose) {
        log.debug { " $text" }
    }

    var data = text.toUtf8Bytes()
    if (data.size < msgSize) {
        val larger = ByteArray(msgSize)
        data.copyInto(larger, 0, 0, data.size)
        data = larger
    }
    return data
}
