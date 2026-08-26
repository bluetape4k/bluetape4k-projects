package io.bluetape4k.examples.coroutines.flow

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.coroutines.assertResult
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.codec.Base58
import io.bluetape4k.coroutines.flow.extensions.log
import io.bluetape4k.junit5.awaitility.untilSuspending
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
import kotlinx.coroutines.currentCoroutineContext
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
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
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

    /**
     * kotlinx.coroutines는 suspend 경계에서 호출자 stack을 복원하려고 예외를 복사할 수 있다.
     * 이 형태만 unwrap하고 임의의 same-type cause는 원래 identity와 진단 정보를 유지한다.
     */
    private fun Throwable.unwrapRecoveredCoroutineCause(): Throwable {
        val nested = cause ?: return this
        val recoveredAtCoroutineBoundary = stackTrace.any { it.className == "_COROUTINE._BOUNDARY._" }
        if (!recoveredAtCoroutineBoundary || nested.javaClass != javaClass || nested.message != message) {
            return this
        }
        suppressed.forEach { suppressedCause ->
            if (suppressedCause !== nested && nested.suppressed.none { it === suppressedCause }) {
                nested.addSuppressed(suppressedCause)
            }
        }
        return nested
    }

    private fun producerResults(
        records: Flow<ProducerRecord<String, String>>,
        producerFactory: () -> Producer<String, String>,
        channelCapacity: Int = 16,
        maxInFlight: Int = 16,
        beforeRegister: (() -> Unit)? = null,
    ): Flow<RecordMetadata> {
        require(channelCapacity in 1..16) { "channelCapacity must be between 1 and 16" }
        require(maxInFlight in 1..16) { "maxInFlight must be between 1 and 16" }

        return callbackFlow {
            var producer: Producer<String, String>? = null
            class DownstreamCancellation(val cause: CancellationException)

            val terminalState = AtomicReference<Any?>(null)
            val permits = Semaphore(maxInFlight)
            val callbackFlowJob = currentCoroutineContext()[Job]

            fun isDownstreamCancelled(): Boolean = terminalState.get() is DownstreamCancellation

            fun terminalCause(): Throwable? = when (val terminal = terminalState.get()) {
                is DownstreamCancellation -> terminal.cause
                is Throwable -> terminal
                else -> null
            }

            class SendState {
                val future = AtomicReference<Future<RecordMetadata>?>(null)
                val completed = AtomicBoolean()
            }

            val inFlight = ConcurrentHashMap.newKeySet<SendState>()
            val upstreamJobRef = AtomicReference<Job?>()

            fun cancelState(state: SendState) {
                state.future.get()?.cancel(false)
                if (state.completed.compareAndSet(false, true)) {
                    inFlight.remove(state)
                    permits.release()
                }
            }

            fun cancelInFlight() {
                inFlight.toList().forEach(::cancelState)
            }

            fun failOnce(cause: Throwable) {
                if (terminalState.compareAndSet(null, cause)) {
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
                        failOnce(cause.unwrapRecoveredCoroutineCause())
                    } else if (metadata != null) {
                        val result = trySend(metadata)
                        if (result.isFailure && !result.isClosed && !isDownstreamCancelled()) {
                            failOnce(IllegalStateException("callback buffer is full"))
                        }
                    } else {
                        failOnce(IllegalStateException("Kafka callback returned neither metadata nor failure"))
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
                        beforeRegister?.invoke()
                        if (terminalCause() != null || isDownstreamCancelled()) {
                            cancelState(state)
                            ensureActive()
                            return@collect
                        }
                        inFlight += state
                        if (terminalCause() != null || isDownstreamCancelled()) {
                            cancelState(state)
                            ensureActive()
                            return@collect
                        }
                        try {
                            val future = activeProducer.send(record, callbackFor(state))
                            state.future.set(future)
                            if (terminalCause() != null || isDownstreamCancelled()) {
                                cancelState(state)
                            }
                        } catch (cause: CancellationException) {
                            cancelState(state)
                            if (!isDownstreamCancelled()) failOnce(cause.unwrapRecoveredCoroutineCause())
                            throw cause
                        } catch (cause: Throwable) {
                            cancelState(state)
                            failOnce(cause.unwrapRecoveredCoroutineCause())
                            throw cause
                        }
                        ensureActive()
                    }
                } catch (cause: CancellationException) {
                    if (!isDownstreamCancelled()) failOnce(cause.unwrapRecoveredCoroutineCause())
                    throw cause
                } catch (cause: Throwable) {
                    failOnce(cause.unwrapRecoveredCoroutineCause())
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
                                cleanupFailure = cause.unwrapRecoveredCoroutineCause()
                                cancelInFlight()
                                if (!isDownstreamCancelled()) failOnce(cause.unwrapRecoveredCoroutineCause())
                            } catch (cause: CancellationException) {
                                cleanupFailure = cause.unwrapRecoveredCoroutineCause()
                                cleanupCancellation = cause.unwrapRecoveredCoroutineCause() as? CancellationException
                                cancelInFlight()
                                if (!isDownstreamCancelled()) failOnce(cause.unwrapRecoveredCoroutineCause())
                            } catch (cause: Throwable) {
                                cleanupFailure = cause.unwrapRecoveredCoroutineCause()
                                cancelInFlight()
                                if (!isDownstreamCancelled()) failOnce(cause.unwrapRecoveredCoroutineCause())
                            }

                            var closeFailure: Throwable? = null
                            var closeCancellation: CancellationException? = null
                            try {
                                withTimeout(5.seconds) {
                                    runInterruptible { activeProducer.close(Duration.ofSeconds(5)) }
                                }
                            } catch (cause: CancellationException) {
                                closeFailure = cause.unwrapRecoveredCoroutineCause()
                                closeCancellation = cause.unwrapRecoveredCoroutineCause() as? CancellationException
                            } catch (cause: Throwable) {
                                closeFailure = cause.unwrapRecoveredCoroutineCause()
                            }

                            val first = terminalCause()
                            if (first != null && cleanupFailure != null && first !== cleanupFailure) {
                                first.addSuppressed(cleanupFailure)
                            }
                            if (first != null && closeFailure != null && first !== closeFailure) {
                                first.addSuppressed(closeFailure)
                            }
                            if (first == null && closeFailure != null && !isDownstreamCancelled()) {
                                failOnce(closeFailure)
                            }
                            if (terminalCause() == null && !isDownstreamCancelled()) close()
                            if (cleanupCancellation != null && terminalCause() == null) {
                                throw cleanupCancellation
                            }
                            if (closeCancellation != null && terminalCause() == null) {
                                throw closeCancellation
                            }
                        }
                    }
                }
            }
            upstreamJobRef.set(upstreamJob)
            upstreamJob.start()
            awaitClose {
                val cancellation = callbackFlowJob
                    ?.takeIf { it.isCancelled }
                    ?.getCancellationException()
                    ?: CancellationException("collector cancelled")
                terminalState.compareAndSet(null, DownstreamCancellation(cancellation))
                cancelInFlight()
                upstreamJob.cancel(cancellation)
            }
        }.buffer(channelCapacity, onBufferOverflow = BufferOverflow.SUSPEND)
            .catch { cause ->
                throw cause.unwrapRecoveredCoroutineCause()
            }
    }

    @Test
    fun `callback failure preserves first cause and closes producer once`() = runSuspendIO {
        val failure = IllegalStateException("callback failure")
        val producer = TrackingProducer(callbackError = failure)

        val error = assertFailsWith<IllegalStateException> {
            producerResults(flowOf(record("failure")), { producer.producer }).toList()
        }

        error.assertIdentityOrDirectCause(failure)
        producer.closeCount.get() shouldBeEqualTo 1
        producer.callbackCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `same type callback wrapper without coroutine boundary preserves wrapper identity`() = runSuspendIO {
        val original = IllegalStateException("callback root")
        val wrapper = IllegalStateException("callback wrapper", original)
        val producer = TrackingProducer(callbackError = wrapper)

        val error = assertFailsWith<IllegalStateException> {
            producerResults(flowOf(record("wrapper")), { producer.producer }).toList()
        }

        error.assertIdentityOrDirectCause(wrapper)
        error.cause?.let { it.cause ?: it } shouldBeSameInstanceAs original
        producer.closeCount.get() shouldBeEqualTo 1
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
        await.atMost(Duration.ofSeconds(5)) untilSuspending { producer.cancelledPendingSends() > 0 }
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
    fun `collector cancellation wins over a concurrent callback failure`() = runSuspendIO {
        val callbackFailure = IllegalStateException("late callback failure")
        val callbackStarted = CountDownLatch(1)
        val callbackGate = CountDownLatch(1)
        val producer = TrackingProducer(
            callbackError = callbackFailure,
            holdCallbacks = true,
            callbackStarted = callbackStarted,
            callbackGate = callbackGate,
        )
        val task = async {
            producerResults(flowOf(record("concurrent-cancel")), { producer.producer }).toList()
        }

        withTimeout(5.seconds) { producer.sendStarted.await() }
        val callback = async(Dispatchers.IO) { producer.fireCallback() }
        callbackStarted.await(5, TimeUnit.SECONDS).shouldBeTrue()
        task.cancel()
        await.atMost(Duration.ofSeconds(5)) untilSuspending { producer.closeCount.get() > 0 }
        callbackGate.countDown()
        callback.await()

        assertFailsWith<CancellationException> { task.await() }
        producer.closeCount.get() shouldBeEqualTo 1
        producer.callbackCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `collector cancellation cancels every in-flight send`() = runSuspendIO {
        val producer = TrackingProducer(holdCallbacks = true)
        val task = async {
            producerResults(
                records = flowOf(record("cancel-one"), record("cancel-two")),
                producerFactory = { producer.producer },
                maxInFlight = 2,
            ).toList()
        }

        withTimeout(5.seconds) { producer.twoSendsStarted.await() }
        task.cancel()
        await.atMost(Duration.ofSeconds(5)) untilSuspending { producer.cancelledPendingSends() >= 2 }
        assertFailsWith<CancellationException> { task.await() }

        producer.closeCount.get() shouldBeEqualTo 1
        producer.cancelledPendingSends() shouldBeEqualTo 2
    }

    @Test
    fun `collector cancellation during registration handoff releases the send state`() = runSuspendIO {
        val producer = TrackingProducer(holdCallbacks = true)
        val registrationStarted = CountDownLatch(1)
        val releaseRegistration = CountDownLatch(1)
        val task = async {
            producerResults(
                records = flowOf(record("cancel-registration")),
                producerFactory = { producer.producer },
                beforeRegister = {
                    registrationStarted.countDown()
                    releaseRegistration.await(5, TimeUnit.SECONDS)
                },
            ).toList()
        }

        registrationStarted.await(5, TimeUnit.SECONDS).shouldBeTrue()
        task.cancel()
        releaseRegistration.countDown()

        assertFailsWith<CancellationException> { task.await() }
        await.atMost(Duration.ofSeconds(5)) untilSuspending { producer.closeCount.get() > 0 }
        producer.closeCount.get() shouldBeEqualTo 1
        producer.sendCount.get() shouldBeEqualTo producer.cancelledPendingSends()
        producer.pendingSends.all { it.isCancelled }.shouldBeTrue()
    }

    @Test
    fun `collector cancellation keeps its outcome when close fails`() = runSuspendIO {
        val closeFailure = IllegalStateException("close after cancellation")
        val producer = TrackingProducer(holdCallbacks = true, closeError = closeFailure)
        val cancellation = CancellationException("collector cancellation")
        val task = async {
            producerResults(flowOf(record("cancel-close-failure")), { producer.producer }).toList()
        }

        withTimeout(5.seconds) { producer.sendStarted.await() }
        task.cancel(cancellation)
        val observed = assertFailsWith<CancellationException> { task.await() }

        producer.closeCount.get() shouldBeEqualTo 1
        observed.message shouldBeEqualTo cancellation.message
        val cancellationCleanup = sequenceOf(observed, observed.cause)
            .filterNotNull()
            .flatMap { it.suppressed.asSequence() }
        cancellationCleanup.single().cause shouldBeSameInstanceAs closeFailure
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
        factoryError.assertIdentityOrDirectCause(factoryFailure)

        val sendFailure = IllegalStateException("send failure")
        val sendProducer = TrackingProducer(sendError = sendFailure)
        val sendError = assertFailsWith<IllegalStateException> {
            producerResults(flowOf(record("send")), { sendProducer.producer }).toList()
        }
        sendError.assertIdentityOrDirectCause(sendFailure)
        sendProducer.closeCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `malformed callback without metadata or failure is terminal`() = runSuspendIO {
        val producer = TrackingProducer(callbackWithoutMetadata = true)

        val error = assertFailsWith<IllegalStateException> {
            producerResults(flowOf(record("malformed-callback")), { producer.producer }).toList()
        }

        error.message shouldBeEqualTo "Kafka callback returned neither metadata nor failure"
        producer.closeCount.get() shouldBeEqualTo 1
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

        error.assertIdentityOrDirectCause(flushFailure)
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

        error.assertIdentityOrDirectCause(upstreamFailure)
        upstreamFailure.suppressed.single().cause shouldBeSameInstanceAs closeFailure
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
                    runInterruptible { consumer.close(Duration.ofSeconds(5)) }
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

    private fun Throwable.assertIdentityOrDirectCause(expected: Throwable) {
        if (this !== expected) {
            cause shouldBeSameInstanceAs expected
        }
    }

    private class TrackingProducer(
        private val callbackError: Exception? = null,
        private val sendError: Exception? = null,
        private val flushError: Exception? = null,
        val closeError: Exception? = null,
        private val callbackWithoutMetadata: Boolean = false,
        private val holdCallbacks: Boolean = false,
        private val heldSendIndexes: Set<Int> = emptySet(),
        private val callbackStarted: CountDownLatch? = null,
        private val callbackGate: CountDownLatch? = null,
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
                    callback.onCompletion(if (callbackWithoutMetadata) null else metadata, callbackError)
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
                callbackStarted?.countDown()
                callbackGate?.await(5, TimeUnit.SECONDS)
                pending.callback.onCompletion(metadata, callbackError)
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
