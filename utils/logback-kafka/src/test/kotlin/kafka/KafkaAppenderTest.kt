package io.bluetape4k.logback.kafka

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.BasicStatusManager
import ch.qos.logback.core.encoder.Encoder
import io.bluetape4k.logback.kafka.exporter.KafkaExporter
import io.bluetape4k.logback.kafka.keyprovider.KafkaKeyProvider
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KafkaAppenderTest {

    companion object: KLoggingChannel()

    private val appender = KafkaAppender<ILoggingEvent>()
    private val ctx = LoggerContext()
    private val encoder = mockk<Encoder<ILoggingEvent>>(relaxed = true)
    private val keyProvider = mockk<KafkaKeyProvider<ILoggingEvent>>(relaxed = true)
    private val exporter = mockk<KafkaExporter>(relaxed = true)

    protected val sampleEvent = LoggingEvent(
        "fqcn",
        ctx.getLogger("logger"),
        Level.TRACE,
        "msg",
        null,
        null
    )

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()

        ctx.name = "test-ctx"
        ctx.statusManager = BasicStatusManager()

        appender.context = ctx
        appender.name = "KafkaAppenderBase"
        appender.encoder = encoder

        appender.bootstrapServers = "localhost:9093"
        appender.topic = "log.topic.test.1"
        appender.keyProvider = keyProvider
        appender.exporter = exporter

        ctx.start()
    }

    @AfterEach
    fun afterEach() {
        ctx.stop()
        appender.stop()
    }

    @Test
    fun `start and stop appender`() {
        appender.isStarted.shouldBeFalse()
        appender.start()
        appender.isStarted.shouldBeTrue()

        appender.stop()
        appender.isStarted.shouldBeFalse()

        // 아무 일도 하지 않았으므로
        val statusList = ctx.statusManager.copyOfStatusList
        statusList.forEach {
            log.debug { "status=$it" }
        }
        statusList shouldHaveSize 2

        confirmVerified(encoder, keyProvider, exporter)
    }

    @Test
    fun `dont start without bootstrap servers`() {
        appender.bootstrapServers = null
        appender.start()
        appender.isStarted.shouldBeFalse()
        ctx.statusManager.copyOfStatusList shouldHaveSize 1
        ctx.statusManager.copyOfStatusList.first().message shouldBeEqualTo "bootstrap.servers is not set"
    }

    @Test
    fun `dont start without topic`() {
        appender.topic = null
        appender.start()
        appender.isStarted.shouldBeFalse()
        ctx.statusManager.copyOfStatusList shouldHaveSize 1
        ctx.statusManager.copyOfStatusList.first().message shouldBeEqualTo "topic is not set"
    }

    @Test
    fun `dont start without encoder`() {
        appender.encoder = null
        appender.start()
        appender.isStarted.shouldBeFalse()
        ctx.statusManager.copyOfStatusList shouldHaveSize 1
        ctx.statusManager.copyOfStatusList.first().message shouldBeEqualTo "encoder is not set"
    }

    @Test
    fun `로그 발송 시 keyProvider로부터 key 값을 얻는다`() {
        every { encoder.encode(any<ILoggingEvent>()) } returns ByteArray(2)
        appender.start()

        appender.doAppend(sampleEvent)

        verify { keyProvider.get(sampleEvent) }
        verify { exporter.export(any<Producer<ByteArray?, ByteArray?>>(), any(), sampleEvent, any()) }
    }

    @Test
    fun `로그 발송 시 이미 지정된 partition이 있다면 그 값을 사용한다`() {
        every { encoder.encode(any<ILoggingEvent>()) } returns ByteArray(2)
        // 참고: https://notwoods.github.io/mockk-guidebook/docs/mockito-migrate/argument-captor/
        val producerRecordCaptor = slot<ProducerRecord<ByteArray?, ByteArray?>>()
        appender.partition = 3
        appender.start()

        appender.doAppend(sampleEvent)

        verify {
            exporter.export(
                any<Producer<ByteArray?, ByteArray?>>(),
                capture(producerRecordCaptor),
                sampleEvent,
                any()
            )
        }

        val record = producerRecordCaptor.captured
        record.partition() shouldBeEqualTo appender.partition
    }

    @Test
    fun `check kafka logger prefix`() {
        KafkaAppender.KAFKA_LOGGER_PREFIX shouldBeEqualTo "org.apache.kafka.clients"
    }

    @Test
    fun `Kafka Client 관련 로그는 지연된 발송이 되어야 한다`() {
        every { encoder.encode(any<ILoggingEvent>()) } returns ByteArray(2)
        appender.start()

        val kafkaClientEvent = LoggingEvent(
            "fqcn",
            ctx.getLogger(KafkaAppender.KAFKA_LOGGER_PREFIX + ".producer"),
            Level.TRACE,
            "kafka client log",
            null,
            null
        )

        // Kafka Client가 발생시킨 로그는 바로 보내지 않고, queue에 담았다가 다음 로그가 발생할 때 drainQueue에서 모아서 보낸다.
        appender.doAppend(kafkaClientEvent)
        verify { exporter wasNot Called }

        appender.doAppend(sampleEvent)

        verify { exporter.export(any<Producer<ByteArray?, ByteArray?>>(), any(), kafkaClientEvent, any()) }
        verify { exporter.export(any<Producer<ByteArray?, ByteArray?>>(), any(), sampleEvent, any()) }
    }
}
