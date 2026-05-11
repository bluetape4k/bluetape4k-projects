package io.bluetape4k.kafka.coroutines

import io.bluetape4k.concurrent.asCompletableFuture
import io.bluetape4k.concurrent.sequence
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.kafka.AbstractKafkaTest
import io.bluetape4k.kafka.getMetricValueOrNull
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.asDouble
import io.bluetape4k.testcontainers.mq.KafkaServer
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.apache.kafka.clients.producer.Callback
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
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

    @Test
    fun `suspendSend propagates callback exception`() = runTest {
        val failure = TimeoutException("send failed")
        val producer = mockk<Producer<String, String>>()

        every { producer.send(any(), any()) } answers {
            secondArg<Callback>().onCompletion(null, failure)
            CompletableFuture<RecordMetadata>().apply {
                completeExceptionally(failure)
            }
        }

        assertFailsWith<TimeoutException> {
            producer.suspendSend(ProducerRecord(TEST_TOPIC_NAME, "key", "value"))
        }
    }

    @Test
    fun `suspendSend cancels kafka future when coroutine is cancelled`() = runTest {
        val future = RecordingFuture<RecordMetadata>()
        val producer = mockk<Producer<String, String>>()

        every { producer.send(any(), any()) } returns future

        val job = launch {
            producer.suspendSend(ProducerRecord(TEST_TOPIC_NAME, "key", "value"))
        }

        yield()
        job.cancelAndJoin()

        future.cancelledWithInterruption.get().shouldBeTrue()
    }

    @Test
    fun `suspendSend remains stable under SuspendedJobTester`() = runSuspendIO {
        val metadata = recordMetadata()
        val producer = mockk<Producer<String, String>>()

        every { producer.send(any(), any()) } answers {
            secondArg<Callback>().onCompletion(metadata, null)
            CompletableFuture.completedFuture(metadata)
        }

        SuspendedJobTester()
            .workers(4)
            .rounds(32)
            .add {
                producer.suspendSend(ProducerRecord(TEST_TOPIC_NAME, "key", "value"))
                    .verifyRecordMetadata()
            }
            .run()
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
            val prevSentTotal = producer.getMetricValueOrNull("record-send-total").asDouble()

            val sendTime = measureTimeMillis {
                val records = messages.asFlow()
                    .map { ProducerRecord<String, String>(TEST_TOPIC_NAME, null, it) }

                producer.sendAndForget(records, true)
            }

            log.debug { "Send time=$sendTime" }
            val currSentTotal = producer.getMetricValueOrNull("record-send-total").asDouble() - prevSentTotal
            log.debug { "Current sent count=$currSentTotal" }
        }
    }

    private fun measureSendRecords(
        expectCount: Int = MESSAGE_SIZE,
        block: suspend CoroutineScope.() -> Unit,
    ) {
        runBlocking(Dispatchers.IO) {
            val prevSentTotal = producer.getMetricValueOrNull("record-send-total").asDouble()

            block()

            val currSentTotal = producer.getMetricValueOrNull("record-send-total").asDouble() - prevSentTotal
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

    private fun recordMetadata(): RecordMetadata =
        mockk {
            every { topic() } returns TEST_TOPIC_NAME
            every { partition() } returns 0
        }

    private class RecordingFuture<T>: CompletableFuture<T>() {
        val cancelledWithInterruption = AtomicBoolean(false)

        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            cancelledWithInterruption.set(mayInterruptIfRunning)
            return super.cancel(mayInterruptIfRunning)
        }
    }
}
