package io.bluetape4k.nats.client

import io.bluetape4k.nats.client.api.consumerConfiguration
import io.nats.client.Connection
import io.nats.client.ConsumerContext
import io.nats.client.api.ConsumerConfiguration

fun consumerContextOf(
    conn: Connection,
    streamName: String,
    consumerName: String,
): ConsumerContext {
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
    return conn
        .getStreamContext(streamName)
        .createOrUpdateConsumer(consumerCfg)
}
