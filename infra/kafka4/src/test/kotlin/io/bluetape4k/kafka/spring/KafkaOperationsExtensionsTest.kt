package io.bluetape4k.kafka.spring

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.kafka.AbstractKafkaTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.mq.KafkaServer
import io.bluetape4k.testcontainers.mq.Spring
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.DisposableBean
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.messaging.support.MessageBuilder

/**
 * [KafkaOperationsExtensions]의 suspendSend 오버로드들에 대한 통합 테스트입니다.
 *
 * Spring KafkaTemplate 을 실제 Kafka(testcontainer)와 함께 사용하여
 * suspend 확장 함수들이 올바르게 동작하는지 검증합니다.
 */
class KafkaOperationsExtensionsTest: AbstractKafkaTest() {

    companion object: KLoggingChannel() {
        // 다른 테스트와 토픽이 겹치면 offset 불일치로 검증이 어려우므로 전용 토픽 사용
        private const val TOPIC = "$TEST_TOPIC_NAME-ops-ext"
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
        // DefaultKafkaProducerFactory 는 DisposableBean 을 구현하므로 destroy() 로 내부 Producer 를 해제한다.
        // 해제하지 않으면 테스트 반복 시 Kafka 브로커 연결이 축적되어 리소스가 누수된다.
        (producerFactory as? DisposableBean)?.destroy()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `suspendSend ProducerRecord 로 메시지 발송`() = runSuspendIO {
        val record = ProducerRecord<String, String>(TOPIC, "key-1", "value-${System.currentTimeMillis()}")

        // suspendSend(ProducerRecord) 오버로드 검증
        val result = kafkaTemplate.suspendSend(record)

        result.shouldNotBeNull()
        result.recordMetadata.topic() shouldBeEqualTo TOPIC
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `suspendSend topic-value 로 메시지 발송`() = runSuspendIO {
        val value = "value-${System.currentTimeMillis()}"

        // suspendSend(topic, value) 오버로드 검증
        val result = kafkaTemplate.suspendSend(TOPIC, value)

        result.shouldNotBeNull()
        result.recordMetadata.topic() shouldBeEqualTo TOPIC
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `suspendSend topic-key-value 로 메시지 발송`() = runSuspendIO {
        val key = "key-kv"
        val value = "value-${System.currentTimeMillis()}"

        // suspendSend(topic, key, value) 오버로드 검증
        val result = kafkaTemplate.suspendSend(TOPIC, key, value)

        result.shouldNotBeNull()
        result.recordMetadata.topic() shouldBeEqualTo TOPIC
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `suspendSend topic-partition-key-value 로 파티션 지정 발송`() = runSuspendIO {
        val partition = 0
        val key = "key-partition"
        val value = "value-${System.currentTimeMillis()}"

        // suspendSend(topic, partition, key, value) 오버로드 검증
        val result = kafkaTemplate.suspendSend(TOPIC, partition, key, value)

        result.shouldNotBeNull()
        result.recordMetadata.topic() shouldBeEqualTo TOPIC
        result.recordMetadata.partition() shouldBeEqualTo partition
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `suspendSend topic-partition-timestamp-key-value 로 타임스탬프 지정 발송`() = runSuspendIO {
        val partition = 0
        val timestamp = System.currentTimeMillis()
        val key = "key-ts"
        val value = "value-$timestamp"

        // suspendSend(topic, partition, timestamp, key, value) 오버로드 검증
        val result = kafkaTemplate.suspendSend(TOPIC, partition, timestamp, key, value)

        result.shouldNotBeNull()
        result.recordMetadata.topic() shouldBeEqualTo TOPIC
        result.recordMetadata.partition() shouldBeEqualTo partition
    }

    @Test
    fun `suspendSend Spring Message 로 메시지 발송`() = runSuspendIO {
        val payload = "message-payload-${System.currentTimeMillis()}"
        // Spring Message 는 KafkaTemplate.defaultTopic 으로 라우팅된다.
        val message = MessageBuilder.withPayload(payload).build()

        // suspendSend(Message) 오버로드 검증
        val result = kafkaTemplate.suspendSend(message)

        result.shouldNotBeNull()
        result.recordMetadata.topic() shouldBeEqualTo TOPIC
    }

    @Test
    fun `suspendSendDefault 기본 토픽으로 value 발송`() = runSuspendIO {
        val value = "default-value-${System.currentTimeMillis()}"

        // suspendSendDefault(value) 오버로드 검증 — KafkaTemplate.defaultTopic 사용
        val result = kafkaTemplate.suspendSendDefault(value)

        result.shouldNotBeNull()
        result.recordMetadata.topic() shouldBeEqualTo TOPIC
    }

    @Test
    fun `suspendSendDefault 기본 토픽으로 key-value 발송`() = runSuspendIO {
        val key = "default-key"
        val value = "default-value-${System.currentTimeMillis()}"

        // suspendSendDefault(key, value) 오버로드 검증 — KafkaTemplate.defaultTopic 사용
        val result = kafkaTemplate.suspendSendDefault(key, value)

        result.shouldNotBeNull()
        result.recordMetadata.topic() shouldBeEqualTo TOPIC
    }
}
