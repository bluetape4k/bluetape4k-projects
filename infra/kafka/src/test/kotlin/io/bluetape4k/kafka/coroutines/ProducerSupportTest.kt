package io.bluetape4k.kafka.coroutines

import io.bluetape4k.concurrent.asCompletableFuture
import io.bluetape4k.concurrent.sequence
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.kafka.AbstractKafkaTest
import io.bluetape4k.kafka.getMetricValue
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.mq.KafkaServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.jupiter.api.RepeatedTest
import kotlin.system.measureTimeMillis

@RandomizedTest
class ProducerSupportTest: AbstractKafkaTest() {

    companion object: KLoggingChannel() {
        private const val MESSAGE_SIZE = 10

        fun randomStrings(size: Int = MESSAGE_SIZE): List<String> {
            return List(size) { randomString() }
        }
    }

    private val producer = KafkaServer.Launcher.createStringProducer()

    @RepeatedTest(REPEAT_SIZE)
    fun `send one message with future`(@RandomValue message: String) = runSuspendIO {
        val record = ProducerRecord<String, String>(TEST_TOPIC_NAME, null, message)

        val future = producer.send(record).asCompletableFuture()
        producer.flush()
        val metadata = future.await()
        metadata.verifyRecordMetadata()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `send one message in suspend`(@RandomValue message: String) = runSuspendIO {
        val record = ProducerRecord<String, String>(TEST_TOPIC_NAME, null, message)

        val metadata = producer.suspendSend(record)
        metadata.verifyRecordMetadata()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `send many messages with future`() {
        val messages = randomStrings()

        measureSendRecords(MESSAGE_SIZE) {
            val futures = messages.map { message ->
                val record = ProducerRecord<String, String>(TEST_TOPIC_NAME, null, message)
                producer.send(record).asCompletableFuture()
            }

            val metadatas = futures.sequence().get()
            metadatas.forEach { metadata ->
                metadata.verifyRecordMetadata()
            }
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `send many messages with suspend`() {
        val messages = randomStrings()

        measureSendRecords(MESSAGE_SIZE) {
            val tasks = messages.map { message ->
                val record = ProducerRecord<String, String>(TEST_TOPIC_NAME, null, message)
                async(Dispatchers.IO) {
                    producer.suspendSend(record)
                }
            }

            tasks.awaitAll().forEach { metadata ->
                metadata.verifyRecordMetadata()
            }
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `send flow messages all async`() {
        val messages = randomStrings()

        measureSendRecords(MESSAGE_SIZE) {
            val sendTime = measureTimeMillis {
                val records = messages.asFlow()
                    .map {
                        ProducerRecord<String, String>(TEST_TOPIC_NAME, null, it)
                    }

                val lastResult = producer.sendAsFlow(records).last()
                lastResult.verifyRecordMetadata()
            }
            log.debug { "Send time=$sendTime" }
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `send flow messages as parallel mode`() {
        val messages = randomStrings()

        measureSendRecords(MESSAGE_SIZE) {
            val sendTime = measureTimeMillis {
                val records = messages.asFlow()
                    .map { ProducerRecord<String, String>(TEST_TOPIC_NAME, null, it) }

                val lastResult = producer.sendAsFlowParallel(records)
                lastResult.verifyRecordMetadata()
            }
            log.debug { "Send time=$sendTime" }
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `send and forget flow messages`() {
        val messages = randomStrings()

        runBlocking(Dispatchers.IO) {
            val prevSentTotal = producer.getMetricValue("record-send-total")

            val sendTime = measureTimeMillis {
                val records = messages.asFlow()
                    .map { ProducerRecord<String, String>(TEST_TOPIC_NAME, null, it) }

                producer.sendAndForget(records, true)
            }

            log.debug { "Send time=$sendTime" }
            val currSentTotal = producer.getMetricValue("record-send-total") - prevSentTotal
            log.debug { "Current sent count=$currSentTotal" }
        }
    }

    private fun measureSendRecords(
        expectCount: Int = MESSAGE_SIZE,
        block: suspend CoroutineScope.() -> Unit,
    ) {
        runBlocking(Dispatchers.IO) {
            val prevSentTotal = producer.getMetricValue("record-send-total")

            block()

            val currSentTotal = producer.getMetricValue("record-send-total") - prevSentTotal
            log.debug { "Current sent count=$currSentTotal" }
            currSentTotal.toInt() shouldBeGreaterOrEqualTo expectCount
        }
    }

    private fun RecordMetadata.verifyRecordMetadata() {
        topic() shouldBeEqualTo TEST_TOPIC_NAME
        partition() shouldBeGreaterOrEqualTo 0
        // ACK >= 1 이어야만 유효합니다.
        // offset() shouldBeGreaterOrEqualTo 0
    }
}
