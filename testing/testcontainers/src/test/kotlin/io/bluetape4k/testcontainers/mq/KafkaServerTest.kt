package io.bluetape4k.testcontainers.mq

import io.bluetape4k.LibraryName
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.AbstractContainerTest
import kotlinx.coroutines.launch
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.test.assertFailsWith

class KafkaServerTest: AbstractContainerTest() {

    companion object: KLogging() {
        private const val TOPIC_NAME = "$LibraryName-test-topic-1"
        private const val TOPIC_NAME_CORUTINE = "$LibraryName-test-topic-coroutines-1"
    }

    @Nested
    inner class Docker {
        @AfterAll
        fun afterAll() {
            KafkaServer.Launcher.kafka.close()
        }

        @Test
        fun `launch kafka server`() {
            val kafka = KafkaServer.Launcher.kafka

            log.debug { "bootstrapServers=${kafka.bootstrapServers}" }
            log.debug { "boundPortNumbers=${kafka.boundPortNumbers}" }

            kafka.bootstrapServers.shouldNotBeEmpty()
            kafka.isRunning.shouldBeTrue()
        }

        @Test
        fun `producing and consuming messages`() {
            val producer = KafkaServer.Launcher.createStringProducer()

            val produced = AtomicBoolean(false)
            val record = ProducerRecord(TOPIC_NAME, "message-key", "Hello world")
            producer.send(record) { metadata, exception ->
                exception.shouldBeNull()
                metadata.topic() shouldBeEqualTo TOPIC_NAME
                metadata.partition() shouldBeGreaterOrEqualTo 0
                produced.set(true)
            }
            producer.flush()
            await.until { produced.get() }

            val consumer = KafkaServer.Launcher.createStringConsumer()
            consumer.subscribe(listOf(TOPIC_NAME))
            var consumerRecords: ConsumerRecords<String, String>
            do {
                consumerRecords = consumer.poll(Duration.ofMillis(1000))
                if (!consumerRecords.isEmpty) {
                    log.debug { "consumerRecords=$consumerRecords" }
                }
            } while (consumerRecords.isEmpty)

            consumerRecords.shouldNotBeNull()
            consumerRecords.count() shouldBeGreaterThan 0

            val consumerRecord = consumerRecords.first()
            consumerRecord.topic() shouldBeEqualTo TOPIC_NAME
            consumerRecord.key() shouldBeEqualTo "message-key"
            consumerRecord.value() shouldBeEqualTo "Hello world"

            consumer.commitSync()

            producer.close()
            closeConsumer(consumer)
        }

        @Test
        fun `producing with coroutines`() = runSuspendIO {
            val producer = KafkaServer.Launcher.createStringProducer()

            val producingJob = launch {
                val record = ProducerRecord(
                    TOPIC_NAME_CORUTINE,
                    "coroutine-key",
                    "message in coroutines"
                )
                val metadata = producer.sendSuspending(record)

                metadata.topic() shouldBeEqualTo TOPIC_NAME_CORUTINE
                metadata.partition() shouldBeGreaterOrEqualTo 0
            }

            val consumer = KafkaServer.Launcher.createStringConsumer()
            consumer.subscribe(listOf(TOPIC_NAME_CORUTINE))

            producingJob.join()

            var consumerRecords: ConsumerRecords<String, String>
            do {
                consumerRecords = consumer.poll(Duration.ofMillis(1000))
                if (!consumerRecords.isEmpty) {
                    log.debug { "consumerRecords=$consumerRecords" }
                }
            } while (consumerRecords.isEmpty)

            consumerRecords.shouldNotBeNull()
            consumerRecords.count() shouldBeGreaterThan 0

            val consumerRecord = consumerRecords.first()
            consumerRecord.topic() shouldBeEqualTo TOPIC_NAME_CORUTINE
            consumerRecord.key() shouldBeEqualTo "coroutine-key"
            consumerRecord.value() shouldBeEqualTo "message in coroutines"

            consumer.commitSync()

            producer.close()
            closeConsumer(consumer)
        }

        private suspend fun <K, V> Producer<K, V>.sendSuspending(
            record: ProducerRecord<K, V>,
        ): RecordMetadata = suspendCoroutine { cont ->
            send(record) { metadata, exception ->
                if (exception != null) {
                    cont.resumeWithException(exception)
                } else {
                    cont.resume(metadata)
                }
            }
        }

        private fun closeConsumer(consumer: Consumer<*, *>) {
            runCatching {
                consumer.unsubscribe()
                consumer.wakeup()
                consumer.close(Duration.ofSeconds(3))
            }
        }
    }

    @Nested
    inner class Default {
        @Test
        fun `launch kafka server with default port`() {
            KafkaServer(useDefaultPort = true).use { kafka ->
                kafka.start()

                log.debug { "bootstrapServers=${kafka.bootstrapServers}" }
                log.debug { "boundPortNumbers=${kafka.boundPortNumbers}" }

                kafka.port shouldBeEqualTo KafkaServer.PORT
                kafka.bootstrapServers.shouldNotBeNull()
                kafka.isRunning.shouldBeTrue()
            }
        }
    }

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { KafkaServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { KafkaServer(tag = " ") }
    }

}
