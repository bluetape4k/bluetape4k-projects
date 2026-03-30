package io.bluetape4k.kafka.spring.core

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.kafka.AbstractKafkaTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.mq.KafkaServer
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldBeTrue
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.springframework.kafka.support.converter.RecordMessageConverter
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import kotlin.test.assertFailsWith

/**
 * [SuspendKafkaProducerTemplate]에 대한 테스트 클래스입니다.
 */
class SuspendKafkaProducerTemplateTest: AbstractKafkaTest() {
    companion object: KLoggingChannel()

    private lateinit var producerTemplate: SuspendKafkaProducerTemplate<String, String>
    private val sender = mockk<KafkaSender<String, String>>(relaxUnitFun = true)
    private val converter = mockk<RecordMessageConverter>()

    @BeforeEach
    fun setup() {
        clearMocks(sender, converter)
        val senderOptions =
            SenderOptions.create<String, String>(
                mapOf(
                    "bootstrap.servers" to KafkaServer.Launcher.kafka.bootstrapServers,
                    "key.serializer" to org.apache.kafka.common.serialization.StringSerializer::class.java,
                    "value.serializer" to org.apache.kafka.common.serialization.StringSerializer::class.java,
                    "acks" to "all",
                    "retries" to 3,
                ),
            )
        producerTemplate = SuspendKafkaProducerTemplate(senderOptions)
    }

    @AfterEach
    fun tearDown() {
        producerTemplate.close()
    }

    @Test
    fun `ProducerTemplate 생성`() {
        producerTemplate.shouldNotBeNull()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `토픽과 값으로 메시지 발송`() =
        runSuspendIO {
            val value = "test-value-${System.currentTimeMillis()}"

            val result = producerTemplate.send(TEST_TOPIC_NAME, value)

            result.shouldNotBeNull()
            result.recordMetadata().topic() shouldBeEqualTo TEST_TOPIC_NAME
        }

    @RepeatedTest(REPEAT_SIZE)
    fun `토픽과 키, 값으로 메시지 발송`() =
        runSuspendIO {
            val key = "test-key"
            val value = "test-value-${System.currentTimeMillis()}"

            val result = producerTemplate.send(TEST_TOPIC_NAME, key, value)

            result.shouldNotBeNull()
            result.recordMetadata().topic() shouldBeEqualTo TEST_TOPIC_NAME
        }

    @RepeatedTest(REPEAT_SIZE)
    fun `파티션 지정하여 메시지 발송`() =
        runSuspendIO {
            val partition = 0
            val key = "test-key"
            val value = "test-value-${System.currentTimeMillis()}"

            val result = producerTemplate.send(TEST_TOPIC_NAME, partition, key, value)

            result.shouldNotBeNull()
            result.recordMetadata().topic() shouldBeEqualTo TEST_TOPIC_NAME
            result.recordMetadata().partition() shouldBeEqualTo partition
        }

    @RepeatedTest(REPEAT_SIZE)
    fun `ProducerRecord로 메시지 발송`() =
        runSuspendIO {
            val record = ProducerRecord(TEST_TOPIC_NAME, "test-key", "test-value-${System.currentTimeMillis()}")

            val result = producerTemplate.send(record)

            result.shouldNotBeNull()
            result.recordMetadata().topic() shouldBeEqualTo TEST_TOPIC_NAME
        }

    @Test
    fun `파티션 정보 조회`() =
        runSuspendIO {
            val partitions = producerTemplate.partitionsFromProducerFor(TEST_TOPIC_NAME)

            partitions.shouldNotBeNull()
            partitions.size shouldBeGreaterOrEqualTo 1
        }

    @Test
    fun `메트릭 정보 조회`() =
        runSuspendIO {
            val metrics = producerTemplate.metricsFromProducer()

            metrics.shouldNotBeNull()
        }

    @Test
    fun `템플릿 종료 시 내부 CoroutineScope 를 취소한다`() = runTest {
        val template = SuspendKafkaProducerTemplate(sender)
        val blocker = CompletableDeferred<Unit>()
        lateinit var launchedJob: Job

        launchedJob = template.launch {
            blocker.await()
        }

        template.close()
        launchedJob.cancelAndJoin()

        (template.coroutineContext[Job]?.isCancelled ?: false).shouldBeTrue()
    }

    @Test
    fun `메시지 변환 결과가 ProducerRecord 가 아니면 명확한 예외를 던진다`() = runTest {
        val message: Message<String> = MessageBuilder.withPayload("payload").build()

        every { converter.fromMessage(message, TEST_TOPIC_NAME) } returns null

        val template = SuspendKafkaProducerTemplate(sender, converter)

        val error = assertFailsWith<IllegalArgumentException> {
            template.send(TEST_TOPIC_NAME, message)
        }

        (error.message?.contains("RecordMessageConverter returned") ?: false).shouldBeTrue()
    }
}
