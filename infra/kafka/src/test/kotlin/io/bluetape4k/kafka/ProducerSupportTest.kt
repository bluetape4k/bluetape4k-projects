package io.bluetape4k.kafka

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.asDouble
import io.bluetape4k.testcontainers.mq.KafkaServer
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.util.Properties

/**
 * [ProducerSupport] 및 Producer 관련 유틸리티 함수에 대한 테스트 클래스입니다.
 */
class ProducerSupportTest: AbstractKafkaTest() {
    companion object: KLoggingChannel()

    private lateinit var producer: org.apache.kafka.clients.producer.Producer<String, String>

    @BeforeEach
    fun setup() {
        producer = KafkaServer.Launcher.createStringProducer()
    }

    @AfterEach
    fun tearDown() {
        producer.close()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `producerOf로 Producer 생성`() {
        val customProducer =
            producerOf<String, String>(
                mapOf(
                    "bootstrap.servers" to KafkaServer.Launcher.kafka.bootstrapServers,
                    "key.serializer" to org.apache.kafka.common.serialization.StringSerializer::class.java,
                    "value.serializer" to org.apache.kafka.common.serialization.StringSerializer::class.java,
                    "acks" to "all",
                    "retries" to 3,
                ),
            )

        customProducer.shouldNotBeNull()
        customProducer.close()
    }

    @Test
    fun `Producer 메트릭 값 조회`() {
        // 메시지 전송
        val record = ProducerRecord(TEST_TOPIC_NAME, "test-key", "test-value")
        producer.send(record).get()

        // 메트릭 조회
        val sendTotal = producer.getMetricValueOrNull("record-send-total").asDouble()
        sendTotal shouldBeGreaterOrEqualTo 0.0

        val retryTotal = producer.getMetricValueOrNull("record-retry-total").asDouble()
        retryTotal shouldBeGreaterOrEqualTo 0.0
    }

    @Test
    fun `getMetricValueOrNull로 메트릭 조회`() {
        val sendTotal = producer.getMetricValueOrNull("record-send-total")
        sendTotal.shouldNotBeNull()
    }

    @Test
    fun `존재하지 않는 메트릭은 null 반환`() {
        val value = producer.getMetricValueOrNull("non-existent-metric")
        value.shouldBeNull()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `메시지 전송 후 메트릭 증가 확인`() {
        val initialSendTotal = producer.getMetricValueOrNull("record-send-total").asDouble()

        // 여러 메시지 전송
        repeat(5) { i ->
            val record = ProducerRecord(TEST_TOPIC_NAME, "key-$i", "value-$i")
            producer.send(record).get()
        }

        val finalSendTotal = producer.getMetricValueOrNull("record-send-total").asDouble()
        finalSendTotal shouldBeGreaterOrEqualTo initialSendTotal + 5
    }

    @Test
    fun `Producer는 정상적으로 종료`() {
        val testProducer = KafkaServer.Launcher.createStringProducer()
        testProducer.shouldNotBeNull()
        testProducer.close()
    }

    @Test
    fun `producerOf Properties로 Producer 생성`() {
        val props = Properties().apply {
            put("bootstrap.servers", KafkaServer.Launcher.kafka.bootstrapServers)
            put("key.serializer", StringSerializer::class.java.name)
            put("value.serializer", StringSerializer::class.java.name)
            put("acks", "all")
        }
        val customProducer = producerOf<String, String>(props)
        customProducer.shouldNotBeNull()
        customProducer.close()
    }

}
