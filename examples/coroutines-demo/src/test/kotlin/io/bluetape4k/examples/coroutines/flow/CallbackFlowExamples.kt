package io.bluetape4k.examples.coroutines.flow

import io.bluetape4k.assertions.coroutines.assertResult
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.codec.Base58
import io.bluetape4k.coroutines.flow.extensions.log
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.mq.KafkaServer
import io.bluetape4k.utils.ShutdownQueue
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import org.apache.kafka.clients.producer.Callback
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.header.internals.RecordHeaders
import org.junit.jupiter.api.Test
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.seconds

class CallbackFlowExamples {

    companion object: KLoggingChannel() {
        private const val KAFKA_IMAGE_REF =
            "confluentinc/cp-kafka@sha256:a5040785528b0bce3b146febe9fcacdcf2b9b5acb450307f75170ef0e60ec130"
    }

    /**
     * Kafka Producer callback을 `callbackFlow`로 변환하고 backpressure·취소·정리 경계를 검증하는 예제다.
     */

    data class Message(val id: Long, val body: String)
    data class Result(val id: Long)

    interface ProduceApi {
        suspend fun produce(message: Message, callback: suspend (Result) -> Unit)
    }

    private class FakeProductApi: ProduceApi {
        override suspend fun produce(message: Message, callback: suspend (Result) -> Unit) {
            delay(100.microseconds)
            val result = Result(message.id)
            log.debug { "Create result. module=callback-flow, recordCount=1, failureKind=none" }
            callback(result)
        }
    }

    // Kafka Producing 에 쓸 수 있다
    private fun flowFrom(api: ProduceApi, message: Flow<Message>) =
        callbackFlow {
            val callback = { result: Result ->
                channel.trySend(result)
                Unit
            }

            message
                .onEach { message -> api.produce(message, callback) }
                .onCompletion { channel.close() }
                .collect()
        }

    private fun Throwable.recoveredOrSelf(): Throwable =
        cause?.takeIf { it::class == this@recoveredOrSelf::class && it.message == message } ?: this

    private fun producerResults(
        records: Flow<ProducerRecord<String, String>>,
        producerFactory: () -> Producer<String, String>,
        channelCapacity: Int = 16,
        maxInFlight: Int = 16,
    ): Flow<RecordMetadata> {
        require(channelCapacity in 1..16) { "channelCapacity must be between 1 and 16" }
        require(maxInFlight in 1..16) { "maxInFlight must be between 1 and 16" }

        return callbackFlow {
            var producer: Producer<String, String>? = null
            val terminalCause = AtomicReference<Throwable?>(null)
            val downstreamCancelled = AtomicBoolean()
            val permits = Semaphore(maxInFlight)

            class SendState {
                val future = AtomicReference<Future<RecordMetadata>?>(null)
                val completed = AtomicBoolean()
            }

            val inFlight = ConcurrentHashMap.newKeySet<SendState>()
            val upstreamJobRef = AtomicReference<Job?>()

            fun cancelInFlight() {
                inFlight.toList().forEach { state ->
                    state.future.get()?.cancel(false)
                    if (state.completed.compareAndSet(false, true)) {
                        inFlight.remove(state)
                        permits.release()
                    }
                }
            }

            fun failOnce(cause: Throwable) {
                if (downstreamCancelled.get()) return
                if (terminalCause.compareAndSet(null, cause)) {
                    cancelInFlight()
                    upstreamJobRef.get()?.cancel(CancellationException("producer terminal failure", cause))
                    close(cause)
                } else {
                    log.debug { "late Kafka callback ignored; failureKind=${cause::class.simpleName}" }
                }
            }

            fun complete(state: SendState, metadata: RecordMetadata?, cause: Exception?) {
                if (!state.completed.compareAndSet(false, true)) return
                try {
                    if (cause != null) {
                        failOnce(cause.recoveredOrSelf())
                    } else if (metadata != null) {
                        val result = trySend(metadata)
                        if (result.isFailure && !result.isClosed && !downstreamCancelled.get()) {
                            failOnce(IllegalStateException("callback buffer is full"))
                        }
                    }
                } finally {
                    inFlight.remove(state)
                    permits.release()
                }
            }

            fun callbackFor(state: SendState): Callback = Callback { metadata, cause ->
                complete(state, metadata, cause)
            }

            val upstreamJob = launch(context = Dispatchers.IO, start = CoroutineStart.LAZY) {
                try {
                    val activeProducer = producerFactory()
                    producer = activeProducer
                    records.collect { record ->
                        permits.acquire()
                        val state = SendState()
                        inFlight += state
                        try {
                            val future = activeProducer.send(record, callbackFor(state))
                            state.future.set(future)
                            if (terminalCause.get() != null || downstreamCancelled.get()) {
                                future.cancel(false)
                            }
                        } catch (cause: CancellationException) {
                            if (state.completed.compareAndSet(false, true)) {
                                inFlight.remove(state)
                                permits.release()
                            }
                            if (!downstreamCancelled.get()) failOnce(cause.recoveredOrSelf())
                            throw cause
                        } catch (cause: Throwable) {
                            if (state.completed.compareAndSet(false, true)) {
                                inFlight.remove(state)
                                permits.release()
                            }
                            failOnce(cause.recoveredOrSelf())
                            throw cause
                        }
                        ensureActive()
                    }
                } catch (cause: CancellationException) {
                    if (!downstreamCancelled.get()) failOnce(cause.recoveredOrSelf())
                    throw cause
                } catch (cause: Throwable) {
                    failOnce(cause.recoveredOrSelf())
                    throw cause
                } finally {
                    producer?.let { activeProducer ->
                        withContext(NonCancellable + Dispatchers.IO) {
                            var cleanupFailure: Throwable? = null
                            var cleanupCancellation: CancellationException? = null
                            try {
                                withTimeout(30.seconds) {
                                    while (inFlight.isNotEmpty()) delay(10)
                                    runInterruptible { activeProducer.flush() }
                                }
                            } catch (cause: TimeoutCancellationException) {
                                cleanupFailure = cause.recoveredOrSelf()
                                cancelInFlight()
                                if (!downstreamCancelled.get()) failOnce(cause.recoveredOrSelf())
                            } catch (cause: CancellationException) {
                                cleanupFailure = cause.recoveredOrSelf()
                                cleanupCancellation = cause.recoveredOrSelf() as? CancellationException
                                cancelInFlight()
                                if (!downstreamCancelled.get()) failOnce(cause.recoveredOrSelf())
                            } catch (cause: Throwable) {
                                cleanupFailure = cause.recoveredOrSelf()
                                cancelInFlight()
                                if (!downstreamCancelled.get()) failOnce(cause.recoveredOrSelf())
                            }

                            var closeFailure: Throwable? = null
                            var closeCancellation: CancellationException? = null
                            try {
                                withTimeout(5.seconds) {
                                    runInterruptible { activeProducer.close(Duration.ofSeconds(5)) }
                                }
                            } catch (cause: CancellationException) {
                                closeFailure = cause.recoveredOrSelf()
                                closeCancellation = cause.recoveredOrSelf() as? CancellationException
                            } catch (cause: Throwable) {
                                closeFailure = cause.recoveredOrSelf()
                            }

                            val first = terminalCause.get()
                            if (first != null && cleanupFailure != null && first !== cleanupFailure) {
                                first.addSuppressed(cleanupFailure)
                            }
                            if (first != null && closeFailure != null && first !== closeFailure) {
                                first.addSuppressed(closeFailure)
                            }
                            if (first == null && closeFailure != null && !downstreamCancelled.get()) {
                                failOnce(closeFailure)
                            }
                            if (terminalCause.get() == null && !downstreamCancelled.get()) close()
                            if (cleanupCancellation != null && terminalCause.get() == null) {
                                throw cleanupCancellation
                            }
                            if (closeCancellation != null && terminalCause.get() == null) {
                                throw closeCancellation
                            }
                        }
                    }
                }
            }
            upstreamJobRef.set(upstreamJob)
            upstreamJob.start()
            awaitClose {
                downstreamCancelled.set(true)
                cancelInFlight()
                upstreamJob.cancel(CancellationException("collector cancelled"))
            }
        }.buffer(channelCapacity, onBufferOverflow = BufferOverflow.SUSPEND)
            .catch { cause ->
                throw cause.recoveredOrSelf()
            }
    }

    @Test
    fun `callback failure preserves first cause and closes producer once`() = runSuspendIO {
        val failure = IllegalStateException("callback failure")
        val producer = TrackingProducer(callbackError = failure)

        val error = assertFailsWith<IllegalStateException> {
            producerResults(flowOf(record("failure")), { producer.producer }).toList()
        }

        error shouldBeEqualTo failure
        producer.closeCount.get() shouldBeEqualTo 1
        producer.callbackCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `backpressure fails without dropping callback and closes producer once`() = runSuspendIO {
        val producer = TrackingProducer(holdCallbacks = true)
        val collectorGate = CompletableDeferred<Unit>()
        val firstItemReceived = CompletableDeferred<Unit>()
        supervisorScope {
            val collection = async {
                var received = 0
                producerResults(
                    records = flowOf(record("one"), record("two"), record("three")),
                    producerFactory = { producer.producer },
                    channelCapacity = 1,
                    maxInFlight = 2,
                ).collect {
                    val index = received
                    received += 1
                    if (index == 0) {
                        firstItemReceived.complete(Unit)
                        collectorGate.await()
                    }
                }
            }

            withTimeout(5.seconds) { producer.twoSendsStarted.await() }
            producer.pendingSends.distinct().size shouldBeEqualTo 2
            producer.fireCallback()
            withTimeout(5.seconds) { firstItemReceived.await() }
            withTimeout(5.seconds) { producer.allSendsStarted.await() }
            producer.pendingSends.distinct().size shouldBeEqualTo 3
            producer.fireCallback()
            producer.fireCallback()
            collectorGate.complete(Unit)
            val error = assertFailsWith<IllegalStateException> {
                withTimeout(10.seconds) { collection.await() }
            }

            error.message shouldBeEqualTo "callback buffer is full"
            producer.closeCount.get() shouldBeEqualTo 1
            producer.callbackCount.get() shouldBeEqualTo 3
        }
    }

    @Test
    fun `in-flight permit is released after callback`() = runSuspendIO {
        val producer = TrackingProducer()

        producerResults(
            records = flowOf(record("permit-one"), record("permit-two")),
            producerFactory = { producer.producer },
            channelCapacity = 2,
            maxInFlight = 1,
        ).toList()

        producer.callbackCount.get() shouldBeEqualTo 2
    }

    @Test
    fun `collector cancellation rethrows CancellationException and closes producer once`() = runSuspendIO {
        val producer = TrackingProducer(holdCallbacks = true)
        val task = async {
            producerResults(flowOf(record("cancel")), { producer.producer }).toList()
        }

        withTimeout(5.seconds) { producer.sendStarted.await() }
        task.cancel()
        withTimeout(5.seconds) {
            while (producer.cancelledPendingSends() == 0) delay(10)
        }
        val cancellation = assertFailsWith<CancellationException> { task.await() }

        producer.closeCount.get() shouldBeEqualTo 1
        producer.pendingSend.isCancelled shouldBeEqualTo true
        producer.cancelledPendingSends() shouldBeEqualTo 1
        producer.fireLateCallback()
        producer.callbackCount.get() shouldBeEqualTo 0
        producer.lateCallbackCount.get() shouldBeEqualTo 1
        val afterLate = assertFailsWith<CancellationException> { task.await() }
        afterLate::class shouldBeEqualTo cancellation::class
        afterLate.message shouldBeEqualTo cancellation.message
    }

    @Test
    fun `cancellation immediately after producer creation closes it once`() = runSuspendIO {
        val producer = TrackingProducer(holdCallbacks = true)
        val factoryStarted = CompletableDeferred<Unit>()
        val task = async {
            producerResults(
                flowOf(record("immediate-cancel")),
                {
                    factoryStarted.complete(Unit)
                    producer.producer
                },
            ).toList()
        }

        withTimeout(5.seconds) { factoryStarted.await() }
        task.cancel()
        assertFailsWith<CancellationException> { task.await() }

        producer.closeCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `factory and synchronous send failures are terminal causes`() = runSuspendIO {
        val factoryFailure = IllegalArgumentException("factory failure")
        val factoryError = assertFailsWith<IllegalArgumentException> {
            producerResults(flowOf(record("factory")), { throw factoryFailure }).toList()
        }
        factoryError shouldBeEqualTo factoryFailure

        val sendFailure = IllegalStateException("send failure")
        val sendProducer = TrackingProducer(sendError = sendFailure)
        val sendError = assertFailsWith<IllegalStateException> {
            producerResults(flowOf(record("send")), { sendProducer.producer }).toList()
        }
        sendError shouldBeEqualTo sendFailure
        sendProducer.closeCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `producer factory is invoked once for each collection`() = runSuspendIO {
        val producer = TrackingProducer()
        val factoryCalls = AtomicInteger()

        producerResults(
            records = flowOf(record("factory-once"), record("factory-twice")),
            producerFactory = {
                factoryCalls.incrementAndGet()
                producer.producer
            },
        ).toList()

        factoryCalls.get() shouldBeEqualTo 1
        producer.closeCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `normal completion drains callbacks flushes and closes once`() = runSuspendIO {
        val producer = TrackingProducer()

        producerResults(flowOf(record("normal")), { producer.producer }).toList()

        producer.flushCount.get() shouldBeEqualTo 1
        producer.closeCount.get() shouldBeEqualTo 1
        producer.callbackCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `mixed synchronous and asynchronous callbacks drain before close`() = runSuspendIO {
        val producer = TrackingProducer(heldSendIndexes = setOf(2))
        val collection = async {
            producerResults(
                flowOf(record("sync"), record("async")),
                { producer.producer },
            ).toList()
        }

        withTimeout(5.seconds) { producer.twoSendsStarted.await() }
        producer.fireCallback()
        collection.await()

        producer.callbackCount.get() shouldBeEqualTo 2
        producer.flushCount.get() shouldBeEqualTo 1
        producer.closeCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `flush failure becomes the terminal cause after callback drain`() = runSuspendIO {
        val flushFailure = IllegalStateException("flush failure")
        val producer = TrackingProducer(flushError = flushFailure)

        val error = assertFailsWith<IllegalStateException> {
            producerResults(flowOf(record("flush-failure")), { producer.producer }).toList()
        }

        error shouldBeEqualTo flushFailure
        producer.closeCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `upstream exception remains primary and cleanup failure is suppressed`() = runSuspendIO {
        val upstreamFailure = IllegalArgumentException("upstream failure")
        val closeFailure = IllegalStateException("close failure")
        val producer = TrackingProducer(closeError = closeFailure)

        val error = assertFailsWith<IllegalArgumentException> {
            producerResults(
                flow {
                    emit(record("before-upstream-failure"))
                    throw upstreamFailure
                },
                { producer.producer },
            ).toList()
        }

        error shouldBeEqualTo upstreamFailure
        error.suppressed.single() shouldBeEqualTo closeFailure
        producer.closeCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `channel and in flight bounds reject invalid values`() = runSuspendIO {
        assertFailsWith<IllegalArgumentException> {
            producerResults(
                flowOf(record("invalid-capacity")),
                { TrackingProducer().producer },
                channelCapacity = 0,
            ).toList()
        }
        assertFailsWith<IllegalArgumentException> {
            producerResults(
                flowOf(record("invalid-in-flight")),
                { TrackingProducer().producer },
                maxInFlight = 17,
            ).toList()
        }
    }

    @Test
    fun `callback drain timeout cancels pending sends and preserves timeout cause`() = runSuspendIO(timeout = 40.seconds) {
        val producer = TrackingProducer(holdCallbacks = true)

        val error = assertFailsWith<TimeoutCancellationException> {
            producerResults(flowOf(record("timeout")), { producer.producer }).toList()
        }

        error::class shouldBeEqualTo TimeoutCancellationException::class
        producer.closeCount.get() shouldBeEqualTo 1
        producer.pendingSend.isCancelled shouldBeEqualTo true
        producer.cancelledPendingSends() shouldBeEqualTo 1
    }

    @Test
    fun `real Kafka producer callbacks become metadata flow`() = runSuspendIO(timeout = 120.seconds) {
        val topic = "epic-1422-callback-${Base58.randomString(8)}"
        val records = (0 until 8).map { index ->
            ProducerRecord(topic, "key-$index", "value-$index")
        }

        val broker = KafkaServer(DockerImageName.parse(KAFKA_IMAGE_REF)).apply {
            start()
            ShutdownQueue.register(this)
        }
        val consumer = KafkaServer.Launcher.createStringConsumer(broker)
        try {
            consumer.subscribe(listOf(topic))
            val metadata = producerResults(
                records = records.asFlow(),
                producerFactory = { KafkaServer.Launcher.createStringProducer(broker) },
            ).toList()

            metadata shouldHaveSize records.size
            val polled = withTimeout(10.seconds) {
                buildList {
                    while (size < records.size) {
                        addAll(consumer.poll(Duration.ofMillis(250)).toList())
                    }
                }
            }
            polled shouldHaveSize records.size
        } finally {
            withContext(NonCancellable + Dispatchers.IO) {
                withTimeout(5.seconds) {
                    consumer.close(Duration.ofSeconds(5))
                }
            }
        }
    }

    private fun record(value: String): ProducerRecord<String, String> {
        val topic = "epic-1422-${Base58.randomString(8)}"
        val key = "key"
        val headerName = "x-epic-1422"
        val headerValue = "example"
        require(topic.length <= 128)
        require(key.length <= 128)
        require(value.toByteArray(Charsets.UTF_8).size <= 1024)
        require(headerName.toByteArray(Charsets.UTF_8).size <= 256)
        require(headerValue.toByteArray(Charsets.UTF_8).size <= 256)
        val headers = RecordHeaders().add(headerName, headerValue.toByteArray(Charsets.UTF_8))
        return ProducerRecord(topic, null, null, key, value, headers)
    }

    private class TrackingProducer(
        private val callbackError: Exception? = null,
        private val sendError: Exception? = null,
        private val flushError: Exception? = null,
        val closeError: Exception? = null,
        private val holdCallbacks: Boolean = false,
        private val heldSendIndexes: Set<Int> = emptySet(),
    ) {
        val producer: Producer<String, String> = mockk(relaxed = true)
        val callbackCount = AtomicInteger()
        val closeCount = AtomicInteger()
        val flushCount = AtomicInteger()
        val lateCallbackCount = AtomicInteger()
        val sendStarted = CompletableDeferred<Unit>()
        val twoSendsStarted = CompletableDeferred<Unit>()
        val allSendsStarted = CompletableDeferred<Unit>()
        val sendCount = AtomicInteger()
        data class Pending(val callback: Callback, val future: CompletableFuture<RecordMetadata>)
        val pendingCallbacks = ConcurrentLinkedQueue<Pending>()
        val pendingSends = ConcurrentLinkedQueue<CompletableFuture<RecordMetadata>>()
        val pendingSend: CompletableFuture<RecordMetadata>
            get() = pendingSends.peek() ?: error("no pending Kafka send")
        private val closed = AtomicBoolean()
        private val metadata = mockk<RecordMetadata>(relaxed = true)

        init {
            every { producer.send(any<ProducerRecord<String, String>>(), any()) } answers {
                if (sendError != null) throw sendError
                val callback = secondArg<Callback>()
                val index = sendCount.incrementAndGet()
                val hold = holdCallbacks || index in heldSendIndexes
                val resultFuture = if (hold) {
                    val future = CompletableFuture<RecordMetadata>()
                    pendingSends += future
                    pendingCallbacks += Pending(callback, future)
                    sendStarted.complete(Unit)
                    future
                } else {
                    callbackCount.incrementAndGet()
                    callback.onCompletion(metadata, callbackError)
                    sendStarted.complete(Unit)
                    CompletableFuture.completedFuture(metadata)
                }
                when (index) {
                    2 -> twoSendsStarted.complete(Unit)
                    3 -> allSendsStarted.complete(Unit)
                }
                resultFuture
            }
            every { producer.flush() } answers {
                flushCount.incrementAndGet()
                flushError?.let { throw it }
            }
            every { producer.close(any<Duration>()) } answers {
                closeCount.incrementAndGet()
                closed.set(true)
                closeError?.let { throw it }
            }
        }

        fun fireLateCallback() {
            pendingCallbacks.poll()?.let { pending ->
                if (closed.get()) lateCallbackCount.incrementAndGet()
                pending.callback.onCompletion(metadata, null)
                pending.future.complete(metadata)
            }
        }

        fun fireCallback() {
            pendingCallbacks.poll()?.let { pending ->
                callbackCount.incrementAndGet()
                pending.callback.onCompletion(metadata, null)
                pending.future.complete(metadata)
            }
        }

        fun cancelledPendingSends(): Int = pendingSends.count { it.isCancelled }
    }

    @Test
    fun `get messages by callback flow`() = runTest {
        val api = FakeProductApi()

        val messages = flowOf(
            Message(1, "Message 1"),
            Message(2, "Message 2"),
            Message(3, "Message 3"),
        ).log("M")

        val results = flowFrom(api, messages).log("results")

        results
            .map { it.id }
            .assertResult(1, 2, 3)
    }
}
