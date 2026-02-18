package io.bluetape4k.kafka

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.mq.KafkaServer
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.time.Duration

/**
 * [ConsumerSupport] 및 Consumer 관련 유틸리티 함수에 대한 테스트 클래스입니다.
 */
class ConsumerSupportTest: AbstractKafkaTest() {
    companion object: KLoggingChannel()

    private lateinit var producer: org.apache.kafka.clients.producer.Producer<String, String>
    private lateinit var consumer: org.apache.kafka.clients.consumer.Consumer<String, String>
    private val testGroupId = "$TEST_TOPIC_NAME-consumer-group"

    @BeforeEach
    fun setup() {
        producer = KafkaServer.Launcher.createStringProducer()
        consumer =
            consumerOf<String, String>(
                mapOf(
                    "bootstrap.servers" to KafkaServer.Launcher.kafka.bootstrapServers,
                    "group.id" to testGroupId,
                    "key.deserializer" to org.apache.kafka.common.serialization.StringDeserializer::class.java,
                    "value.deserializer" to org.apache.kafka.common.serialization.StringDeserializer::class.java,
                    "auto.offset.reset" to "earliest",
                ),
            )
        consumer.subscribe(listOf(TEST_TOPIC_NAME))
    }

    @AfterEach
    fun tearDown() {
        consumer.close()
        producer.close()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `consumerOf로 Consumer 생성`() {
        val customConsumer =
            consumerOf<String, String>(
                mapOf(
                    "bootstrap.servers" to KafkaServer.Launcher.kafka.bootstrapServers,
                    "group.id" to "$testGroupId-custom",
                    "key.deserializer" to org.apache.kafka.common.serialization.StringDeserializer::class.java,
                    "value.deserializer" to org.apache.kafka.common.serialization.StringDeserializer::class.java,
                    "auto.offset.reset" to "earliest",
                ),
            )

        customConsumer.shouldNotBeNull()
        customConsumer.close()
    }

    @Test
    fun `Consumer는 토픽에서 메시지를 수신`() {
        // 메시지 전송
        val key = "test-key"
        val value = "test-value-${System.currentTimeMillis()}"
        val record = ProducerRecord(TEST_TOPIC_NAME, key, value)
        producer.send(record).get()
        producer.flush()

        // 메시지 수신
        var receivedRecord: ConsumerRecord<String, String>? = null
        val timeout = System.currentTimeMillis() + 10000

        while (System.currentTimeMillis() < timeout && receivedRecord == null) {
            val records = consumer.poll(Duration.ofMillis(100))
            for (r in records) {
                if (r.value() == value) {
                    receivedRecord = r
                    break
                }
            }
        }

        receivedRecord.shouldNotBeNull()
        receivedRecord.key() shouldBeEqualTo key
        receivedRecord.value() shouldBeEqualTo value
        receivedRecord.topic() shouldBeEqualTo TEST_TOPIC_NAME
    }

    @Test
    fun `Consumer 구독 목록 확인`() {
        val subscription = consumer.subscription()
        subscription.shouldNotBeEmpty()
        subscription shouldBeEqualTo setOf(TEST_TOPIC_NAME)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `여러 메시지 수신`() {
        val messageCount = 5
        val sentMessages = mutableListOf<String>()

        // 메시지 전송
        repeat(messageCount) { i ->
            val value = "message-$i-${System.currentTimeMillis()}"
            sentMessages.add(value)
            val record = ProducerRecord(TEST_TOPIC_NAME, "key-$i", value)
            producer.send(record).get()
        }
        producer.flush()

        // 메시지 수신
        val receivedMessages = mutableListOf<String>()
        val timeout = System.currentTimeMillis() + 15000

        while (System.currentTimeMillis() < timeout && receivedMessages.size < messageCount) {
            val records = consumer.poll(Duration.ofMillis(100))
            for (r in records) {
                if (sentMessages.contains(r.value())) {
                    receivedMessages.add(r.value())
                }
            }
        }

        receivedMessages.size shouldBeEqualTo messageCount
    }

    @Test
    fun `Consumer 메트릭 조회`() {
        val metrics = consumer.metrics()
        metrics.shouldNotBeNull()
    }

    @Test
    fun `Consumer는 정상적으로 종료`() {
        val testConsumer =
            consumerOf<String, String>(
                mapOf(
                    "bootstrap.servers" to KafkaServer.Launcher.kafka.bootstrapServers,
                    "group.id" to "$testGroupId-close-test",
                    "key.deserializer" to org.apache.kafka.common.serialization.StringDeserializer::class.java,
                    "value.deserializer" to org.apache.kafka.common.serialization.StringDeserializer::class.java,
                    "auto.offset.reset" to "earliest",
                ),
            )
        testConsumer.shouldNotBeNull()
        testConsumer.close()
    }
}
