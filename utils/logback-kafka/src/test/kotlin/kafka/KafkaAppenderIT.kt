package io.bluetape4k.logback.kafka

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.AppenderBase
import ch.qos.logback.core.status.Status
import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logback.kafka.exporter.DefaultKafkaExporter
import io.bluetape4k.logback.kafka.keyprovider.NullKafkaKeyProvider
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.toUtf8String
import io.bluetape4k.testcontainers.mq.KafkaServer
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.charset.Charset
import java.time.Duration

class KafkaAppenderIT: AbstractKafkaIntegrationTest() {

    companion object: KLoggingChannel() {
        private const val TEST_TOPIC_NAME = "logs.kafka.appender.topic.1"
        private const val TEST_PARTITION = 0
    }

    private val errorCollector = fastListOf<Throwable>()

    private lateinit var kafkaAppender: KafkaAppender<ILoggingEvent>
    private lateinit var loggerContext: LoggerContext

    private val fallbackAppender: Appender<ILoggingEvent> by lazy {
        object: AppenderBase<ILoggingEvent>() {
            override fun append(eventObject: ILoggingEvent?) {
                errorCollector.add(IllegalStateException("Logged to fallback appender: $eventObject"))
            }
        }
    }

    private val fallbackLoggingEvents = fastListOf<ILoggingEvent>()

    @BeforeEach
    fun setup() {
        loggerContext = LoggerContext()
        loggerContext.putProperty("bootstrapServers", kafka.bootstrapServers)
        loggerContext.statusManager.add { status ->
            println(status)
            if (status.effectiveLevel > Status.INFO) {
                status.throwable?.let {
                    errorCollector.add(it)
                } ?: run {
                    errorCollector.add(RuntimeException("StatusManager reported warning: $status"))
                }
            }
        }
        loggerContext.putProperty("HOSTNAME", "localhost")

        val patternLayoutEncoder = PatternLayoutEncoder().apply {
            pattern = "%msg"
            context = loggerContext
            charset = Charset.forName("UTF-8")
            start()
        }

        kafkaAppender = KafkaAppender<ILoggingEvent>().apply {
            context = loggerContext
            name = "TestKafkaAppender"
            addAppender(fallbackAppender)

            bootstrapServers = kafka.bootstrapServers
            topic = TEST_TOPIC_NAME
            partition = TEST_PARTITION
            addProducerConfigValue(ProducerConfig.ACKS_CONFIG, "1")
            addProducerConfigValue(ProducerConfig.MAX_BLOCK_MS_CONFIG, "2000")
            addProducerConfigValue(ProducerConfig.LINGER_MS_CONFIG, "100")
            encoder = patternLayoutEncoder
            keyProvider = NullKafkaKeyProvider()
            exporter = DefaultKafkaExporter()

            addAppender(object: AppenderBase<ILoggingEvent>() {
                override fun append(eventObject: ILoggingEvent) {
                    fallbackLoggingEvents.add(eventObject)
                }
            })
        }
    }

    @Test
    fun `logging to kafka by KafkaAppender`() = runSuspendIO {
        val messageCount = 512
        val messageSize = 256

        val logger = loggerContext.getLogger("ROOT")

        launch {
            //
            // 로그를 Kafka 로 전송한다.
            //
            kafkaAppender.start()
            kafkaAppender.isStarted.shouldBeTrue()

            repeat(messageCount) {
                val message = getMessage(it, messageSize)
                val loggingEvent = LoggingEvent("a.b.c.d", logger, Level.INFO, message, null, null)
                kafkaAppender.doAppend(loggingEvent)
            }

            kafkaAppender.stop()
            kafkaAppender.isStarted.shouldBeFalse()
        }
        yield()

        //
        // 로그 정보를 Kafka 로 부터 읽어온다.
        //
        val topicPartition = TopicPartition(kafkaAppender.topic, kafkaAppender.partition ?: 0)
        val consumer = KafkaServer.Launcher.createBinaryConsumer(kafka).apply {
            assign(listOf(topicPartition))
            seekToBeginning(listOf(topicPartition))
        }
        consumer.position(topicPartition) shouldBeEqualTo 0L

        var readMessagess = 0
        var records = consumer.poll(Duration.ofSeconds(1))
        while (!records.isEmpty) {
            records.forEach { record ->
                val message = record.value()?.toUtf8String().orEmpty()
                val index = message.substringBefore(';').toInt()
                println("received: index=$index, message=$message")
                readMessagess++
            }
            records = consumer.poll(Duration.ofSeconds(1))
        }

        readMessagess shouldBeEqualTo messageCount
        fallbackLoggingEvents.shouldBeEmpty()
    }

    private fun getMessage(index: Int, messageSize: Int): String = buildString {
        append("$index;")
        append(Fakers.fixedString(messageSize))
    }
}
