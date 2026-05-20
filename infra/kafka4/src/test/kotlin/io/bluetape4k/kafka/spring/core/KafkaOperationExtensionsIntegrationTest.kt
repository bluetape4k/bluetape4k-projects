package io.bluetape4k.kafka.spring.core

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.kafka.AbstractKafkaTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
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
 * Integration tests for [KafkaOperationExtensions] using a real Kafka broker (Testcontainers).
 *
 * Covers: suspendSend, sendFlowAsParallel, sendAndForget.
 */
class KafkaOperationExtensionsIntegrationTest: AbstractKafkaTest() {

    companion object: KLoggingChannel() {
        private const val TOPIC = "$TEST_TOPIC_NAME-ops-core-ext"
    }

    private lateinit var producerFactory: ProducerFactory<String, String>
    private lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @BeforeEach
    fun setup() {
        producerFactory = KafkaServer.Launcher.Spring.getStringProducerFactory()
        kafkaTemplate = KafkaTemplate(producerFactory, true).apply {
            setDefaultTopic(TOPIC)
        }
    }

    @AfterEach
    fun tearDown() {
        (producerFactory as? DisposableBean)?.destroy()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `suspendSend ProducerRecord 로 메시지 발송`() = runSuspendIO {
        val record = ProducerRecord<String, String>(TOPIC, "key-1", "value-${System.currentTimeMillis()}")

        val result = kafkaTemplate.suspendSend(record)

        result.shouldNotBeNull()
        result.recordMetadata.topic() shouldBeEqualTo TOPIC
    }

    @Test
    fun `sendFlowAsParallel - Flow 의 모든 레코드를 병렬 발송하고 마지막 결과 반환`() = runSuspendIO {
        val records = flow {
            repeat(3) { i ->
                emit(ProducerRecord<String, String>(TOPIC, "flow-key-$i", "flow-value-$i"))
            }
        }

        val result = kafkaTemplate.sendFlowAsParallel(records)

        result.shouldNotBeNull()
        result.recordMetadata.topic() shouldBeEqualTo TOPIC
    }

    @Test
    fun `sendAndForget - Flow 의 모든 레코드를 발송하고 결과 무시`() = runSuspendIO {
        val records = flow {
            repeat(3) { i ->
                emit(ProducerRecord<String, String>(TOPIC, "forget-key-$i", "forget-value-$i"))
            }
        }

        // Should complete without throwing
        kafkaTemplate.sendAndForget(records)
    }

    @Test
    fun `sendAndForget needFlush true - 발송 후 flush 호출`() = runSuspendIO {
        val records = flow {
            repeat(2) { i ->
                emit(ProducerRecord<String, String>(TOPIC, "flush-key-$i", "flush-value-$i"))
            }
        }

        kafkaTemplate.sendAndForget(records, needFlush = true)
    }

}
