package io.bluetape4k.resilience4j.bulkhead

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContainSame
import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.resilience4j.SuspendHelloWorldService
import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.bulkhead.BulkheadConfig
import io.github.resilience4j.bulkhead.BulkheadFullException
import io.github.resilience4j.kotlin.bulkhead.bulkhead
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class BulkheadFlowTest {

    companion object: KLoggingChannel()

    private var permittedEvents = 0
    private var rejectedEvents = 0
    private var finishedEvents = 0

    private fun Bulkhead.registerEventListener(): Bulkhead = apply {
        eventPublisher.apply {
            onCallPermitted { permittedEvents++ }
            onCallRejected { rejectedEvents++ }
            onCallFinished { finishedEvents++ }
        }
    }

    @BeforeEach
    fun setup() {
        permittedEvents = 0
        rejectedEvents = 0
        finishedEvents = 0
    }

    @Test
    fun `성공할 함수를 실행압니다`() = runSuspendTest {
        val bulkhead = Bulkhead.ofDefaults("testName").registerEventListener()

        val results = flow {
            repeat(3) {
                emit(it)
            }
        }
            .bulkhead(bulkhead)
            .toList()

        results shouldContainSame listOf(0, 1, 2)

        permittedEvents shouldBeEqualTo 1
        rejectedEvents shouldBeEqualTo 0
        finishedEvents shouldBeEqualTo 1
    }

    @Test
    fun `bulkhead가 꽉차면 함수 실행을 하지 않습니다`() = runSuspendTest {
        val bulkhead = Bulkhead.of("testName") {
            BulkheadConfig.custom()
                .maxConcurrentCalls(1)
                .maxWaitDuration(Duration.ZERO)
                .build()
        }.registerEventListener()
        val results = mutableListOf<Int>()

        val sync = Channel<Int>(Channel.RENDEZVOUS)
        val testFlow = flow {
            emit(sync.receive())
            emit(sync.receive())
        }.bulkhead(bulkhead)

        val firstCall = launch {
            testFlow.toList(results)
        }

        // Wait until our first coroutine is inside the bulkhead
        sync.send(1)

        permittedEvents shouldBeEqualTo 1
        rejectedEvents shouldBeEqualTo 0
        finishedEvents shouldBeEqualTo 0
        results shouldContainSame listOf(1)

        val helloWorldService = SuspendHelloWorldService()

        assertFailsWith<BulkheadFullException> {
            flow { emit(helloWorldService.returnHelloWorld()) }
                .bulkhead(bulkhead)
                .single()
        }

        permittedEvents shouldBeEqualTo 1
        rejectedEvents shouldBeEqualTo 1
        finishedEvents shouldBeEqualTo 0

        // allow our first call to complete, and then wait for it
        sync.send(2)
        firstCall.join()

        permittedEvents shouldBeEqualTo 1
        rejectedEvents shouldBeEqualTo 1
        finishedEvents shouldBeEqualTo 1
        results shouldContainSame listOf(1, 2)
        helloWorldService.invocationCount shouldBeEqualTo 0
    }

    @Test
    fun `예외가 발생해도 bulkhead는 됩니다`() = runSuspendTest {
        val bulkhead = Bulkhead.ofDefaults("testName").registerEventListener()
        val results = mutableListOf<Int>()

        assertFailsWith<IllegalStateException> {
            flow {
                repeat(3) {
                    emit(it)
                }
                // flow 종료 시 예외를 발생시킨다
                error("failed")
            }
                .bulkhead(bulkhead)
                .toList(results)
        }

        results shouldContainSame listOf(0, 1, 2)

        permittedEvents shouldBeEqualTo 1
        rejectedEvents shouldBeEqualTo 0
        finishedEvents shouldBeEqualTo 1
    }

    @Test
    fun `실행이 취소되었을 경우 bulkhead는 완료로 기록되지 않습니다`() = runSuspendTest(timeout = 10.seconds) {
        val started = CompletableDeferred<Unit>()
        var flowCompleted = false
        val bulkhead = Bulkhead.of("testName") {
            BulkheadConfig.custom()
                .maxConcurrentCalls(1)
                .maxWaitDuration(Duration.ZERO)
                .build()
        }.registerEventListener()

        val job = launch(start = CoroutineStart.ATOMIC) {
            flow {
                started.complete(Unit)
                delay(5000L.milliseconds)
                emit(1)
                flowCompleted = true
            }
                .bulkhead(bulkhead)
                .first()
        }

        started.await()
        job.cancelAndJoin()

        job.isCompleted.shouldBeTrue()
        job.isCancelled.shouldBeTrue()
        flowCompleted.shouldBeFalse()

        permittedEvents shouldBeEqualTo 1
        rejectedEvents shouldBeEqualTo 0
        finishedEvents shouldBeEqualTo 0
    }

    @Test
    fun `작업이 예외로 인한 취소가 되었을 경우 bulkhead는 완료로 기록되지 않습니다`() = runSuspendTest(timeout = 10.seconds) {
        val started = CompletableDeferred<Unit>()
        val parentJob = Job()
        var flowCompleted = false

        val bulkhead = Bulkhead.of("testName") {
            BulkheadConfig.custom()
                .maxConcurrentCalls(1)
                .maxWaitDuration(Duration.ZERO)
                .build()
        }.registerEventListener()

        val parentScope = CoroutineScope(parentJob)
        val job = parentScope.launch {
            launch(start = CoroutineStart.ATOMIC) {
                flow {
                    started.complete(Unit)
                    delay(5000L.milliseconds)
                    emit(1)
                    flowCompleted = true
                }
                    .bulkhead(bulkhead)
                    .first()
            }
            error("exceptional cancellation")
        }

        started.await()
        parentJob.runCatching { join() }

        job.isCompleted.shouldBeTrue()
        job.isCancelled.shouldBeTrue()
        flowCompleted.shouldBeFalse()

        permittedEvents shouldBeEqualTo 1
        rejectedEvents shouldBeEqualTo 0
        finishedEvents shouldBeEqualTo 0
    }
}
