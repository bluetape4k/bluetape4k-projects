package io.bluetape4k.kafka.logback

import ch.qos.logback.classic.spi.ILoggingEvent
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.support.toUtf8String
import io.bluetape4k.support.trimWhitespace
import io.bluetape4k.testcontainers.mq.KafkaServer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class LogbackIntegrationTest: AbstractKafkaIntegrationTest() {

    companion object: KLoggingChannel() {
        // logback-test.xml 에 KafkaAppender의 topic 속성과 같아야 한다 
        private const val TOPIC = "logs"
    }

    private lateinit var logger: org.slf4j.Logger

    @BeforeAll
    fun beforeAll() {
        // kafka 시작 후 실제 bootstrapServers로 logback-test.xml의 KafkaAppender를 업데이트
        // logback XML은 JVM startup 시 파싱되므로 localhost:9093이 하드코딩됨
        // producer는 lazy라서 아직 생성 전 — 여기서 업데이트하면 실제 포트를 사용
        log.info { "Kafka Server: ${kafka.bootstrapServers}" }

        val loggerContext = LoggerFactory.getILoggerFactory() as ch.qos.logback.classic.LoggerContext
        val logbackLogger = loggerContext.getLogger("LogbackIntegrationTest") as ch.qos.logback.classic.Logger
        @Suppress("UNCHECKED_CAST")
        val kafkaAppender = logbackLogger.getAppender("Kafka") as? KafkaAppender<ILoggingEvent>
        if (kafkaAppender != null) {
            kafkaAppender.bootstrapServers = kafka.bootstrapServers
            log.info { "KafkaAppender bootstrapServers updated: ${kafka.bootstrapServers}" }
        }

        logger = LoggerFactory.getLogger("LogbackIntegrationTest")
    }

    @Test
    fun `export log to kafka and consume`() = runSuspendIO {

        val logSize = 100
        val job = launch {
            repeat(logSize) {
                logger.info("test message $it")
            }
        }
        delay(10.milliseconds)
        job.join()

        val logTopicPartition = TopicPartition(TOPIC, 0)
        val consumer = KafkaServer.Launcher.createBinaryConsumer(kafka)
        consumer.assign(listOf(logTopicPartition))
        consumer.seekToEnd(listOf(logTopicPartition))
        consumer.seekToBeginning(listOf(logTopicPartition))

        var receivedCount = 0
        var records = consumer.poll(Duration.ofSeconds(1))
        while (!records.isEmpty) {
            records.forEach { record ->
                val message = record.value()?.toUtf8String()?.trimWhitespace()
                log.debug { "received from topic=${record.topic()}, partition=${record.partition()}, message: `$message`" }
                message.shouldNotBeNull() shouldContain "test message $receivedCount"
                receivedCount++
            }
            records = consumer.poll(Duration.ofSeconds(1))
        }

        receivedCount shouldBeEqualTo logSize
    }
}
