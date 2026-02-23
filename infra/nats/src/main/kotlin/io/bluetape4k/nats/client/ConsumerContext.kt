package io.bluetape4k.nats.client

import io.bluetape4k.nats.client.api.consumerConfiguration
import io.bluetape4k.support.requireNotBlank
import io.nats.client.Connection
import io.nats.client.ConsumerContext
import io.nats.client.api.ConsumerConfiguration

fun consumerContextOf(
    conn: Connection,
    streamName: String,
    consumerName: String,
): ConsumerContext {
    streamName.requireNotBlank("streamName")
    consumerName.requireNotBlank("consumerName")

    val consumerCfg = consumerConfiguration {
        durable(consumerName)
    }
    return consumerContextOf(conn, streamName, consumerCfg)
}

fun consumerContextOf(
    conn: Connection,
    streamName: String,
    consumerCfg: ConsumerConfiguration,
): ConsumerContext {
    streamName.requireNotBlank("streamName")

    return conn
        .getStreamContext(streamName)
        .createOrUpdateConsumer(consumerCfg)
}
