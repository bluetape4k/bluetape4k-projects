package io.bluetape4k.kafka.spring.core

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.kafka.AbstractKafkaTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.mq.KafkaServer
import io.bluetape4k.testcontainers.mq.Spring
import kotlinx.coroutines.flow.flow
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.DisposableBean
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

/**
 * [KafkaOperationExtensions] (spring/core) 의 확장 함수들에 대한 통합 테스트입니다.
 *
 * `execute {}` 기반의 저수준 API를 사용하는 확장 함수들이 실제 Kafka(testcontainer)와 함께
 * 올바르게 동작하는지 검증합니다.
 */
class KafkaOperationExtensionsTest: AbstractKafkaTest() {

    companion object: KLoggingChannel() {
        private const val TOPIC = "$TEST_TOPIC_NAME-core-ops-ext"
    }

    private lateinit var producerFactory: ProducerFactory<String, String>
    private lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @BeforeEach
    fun setup() {
        producerFactory = KafkaServer.Launcher.Spring.getStringProducerFactory()
        kafkaTemplate = KafkaTemplate(producerFactory, true).apply {
            defaultTopic = TOPIC
        }
    }

    @AfterEach
    fun tearDown() {
        (producerFactory as? DisposableBean)?.destroy()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `suspendSend - ProducerRecord로 메시지를 발송한다`() = runSuspendIO {
        val record = ProducerRecord(TOPIC, "key-1", "value-${Base58.randomString(8)}")

        val result = kafkaTemplate.suspendSend(record)

        log.debug { "SendResult: $result" }
        result.recordMetadata.topic() shouldBeEqualTo TOPIC
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `sendFlowAsParallel - Flow로 여러 메시지를 병렬 발송하고 마지막 결과를 반환한다`() = runSuspendIO {
        val records = flow {
            repeat(3) { i ->
                emit(
                    ProducerRecord(
                        TOPIC,
                        "flow-key-$i",
                        "flow-value-$i-${Base58.randomString(8)}"
                    )
                )
            }
        }

        val result = kafkaTemplate.sendFlowAsParallel(records)
        log.debug { "SendResult: $result" }
        result.recordMetadata.topic() shouldBeEqualTo TOPIC
    }

    @Test
    fun `sendAndForget - Flow로 여러 메시지를 fire-and-forget 방식으로 발송한다`() = runSuspendIO {
        val records = flow {
            repeat(3) { i ->
                emit(ProducerRecord(TOPIC, "forget-key-$i", "forget-value-$i"))
            }
        }

        kafkaTemplate.sendAndForget(records)
    }

    @Test
    fun `sendAndForget with flush - needFlush=true로 발송 후 flush한다`() = runSuspendIO {
        val records = flow {
            repeat(3) { i ->
                emit(ProducerRecord(TOPIC, "flush-key-$i", "flush-value-$i"))
            }
        }

        kafkaTemplate.sendAndForget(records, needFlush = true)
    }

    @Test
    fun `getMetric - 존재하지 않는 메트릭은 null을 반환한다`() {
        val metric = kafkaTemplate.getMetric("nonexistent-metric-xyz-${Base58.randomString(8)}")
        metric.shouldBeNull()
    }

    @Test
    fun `getMetricValueOrNull - 존재하지 않는 메트릭은 null을 반환한다`() {
        val value = kafkaTemplate.getMetricValueOrNull("nonexistent-metric-xyz-${Base58.randomString(8)}")
        value.shouldBeNull()
    }

    @Test
    fun `getMetric - 메시지 발송 후 record-send-total 메트릭을 조회한다`() = runSuspendIO {
        val record = ProducerRecord(TOPIC, "metric-key", "metric-value")
        kafkaTemplate.suspendSend(record)

        // metrics()는 execute{}를 통해 producer에 접근하므로 메트릭이 존재할 수 있음
        // 없으면 null 반환 — 어느 쪽이든 예외 없이 완료되어야 함
        val metric = kafkaTemplate.getMetric("record-send-total")
        log.debug { "metric: $metric" }
    }

    @Test
    fun `getMetricValueOrNull - 메시지 발송 후 메트릭 값을 조회한다`() = runSuspendIO {
        val record = ProducerRecord(TOPIC, "metric-key-2", "metric-value-2")
        kafkaTemplate.suspendSend(record)

        val value = kafkaTemplate.getMetricValueOrNull("record-send-total")
        log.debug { "metric value: $value" }
    }
}
