package io.bluetape4k.logback.kafka

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.support.toUtf8String
import io.bluetape4k.support.trimWhitespace
import io.bluetape4k.testcontainers.mq.KafkaServer
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.Duration

class LogbackIntegrationTest: AbstractKafkaIntegrationTest() {

    companion object: KLoggingChannel() {
        // logback-test.xml 에 KafkaAppender의 topic 속성과 같아야 한다 
        private const val TOPIC = "logs"
    }

    private lateinit var logger: org.slf4j.Logger

    @BeforeAll
    fun beforeAll() {
        logger = LoggerFactory.getLogger("LogbackIntegrationTest")
        log.info { "Create Kafka Server: ${kafka.bootstrapServers}" }
    }

    @Test
    fun `export log to kafka and consume`() {
        val logSize = 100
        repeat(logSize) {
            logger.info("test message $it")
        }
        Thread.sleep(10)

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
