package io.bluetape4k.nats.client

import io.bluetape4k.nats.client.api.consumerConfiguration
import io.bluetape4k.support.requireNotBlank
import io.nats.client.Connection
import io.nats.client.ConsumerContext
import io.nats.client.api.ConsumerConfiguration

/**
 * [consumerName]을 durable 소비자 이름으로 사용해 [ConsumerContext]를 생성합니다.
 *
 * @param conn NATS 연결
 * @param streamName 대상 스트림 이름
 * @param consumerName durable 소비자 이름
 * @return 생성 또는 업데이트된 [ConsumerContext]
 */
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

/**
 * [consumerCfg] 설정으로 [ConsumerContext]를 생성하거나 업데이트합니다.
 *
 * @param conn NATS 연결
 * @param streamName 대상 스트림 이름
 * @param consumerCfg 소비자 설정
 * @return 생성 또는 업데이트된 [ConsumerContext]
 */
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
