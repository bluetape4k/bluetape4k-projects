package io.bluetape4k.resilience4j.bulkhead

import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.coroutines.flow.extensions.toFastList
import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.resilience4j.SuspendHelloWorldService
import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.bulkhead.BulkheadConfig
import io.github.resilience4j.bulkhead.BulkheadFullException
import io.github.resilience4j.kotlin.bulkhead.bulkhead
import io.github.resilience4j.kotlin.bulkhead.decorateSuspendFunction
import io.github.resilience4j.kotlin.bulkhead.executeSuspendFunction
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow

import kotlinx.coroutines.launch
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertFailsWith

class BulkheadCoroutinesTest {

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
    fun `suspend 함수 실행이 제대로 수행되어야합니다`() = runSuspendTest {
        val bulkhead = Bulkhead.ofDefaults("testName").registerEventListener()
        val helloWorldService = SuspendHelloWorldService()

        val result = bulkhead.executeSuspendFunction {
            helloWorldService.returnHelloWorld()
        }

        result shouldBeEqualTo "Hello world"
        permittedEvents shouldBeEqualTo 1
        rejectedEvents shouldBeEqualTo 0
        finishedEvents shouldBeEqualTo 1

        helloWorldService.invocationCount shouldBeEqualTo 1
    }

    @Test
    fun `bulkhead가 꽉차면 함수 실행을 하지 않습니다`() = runSuspendTest {
        val bulkhead = Bulkhead.of("testName") {
            BulkheadConfig.custom()
                .maxConcurrentCalls(1)
                .maxWaitDuration(Duration.ZERO)
                .build()
        }.registerEventListener()
        val results = fastListOf<Int>()

        val sync = Channel<Int>(Channel.RENDEZVOUS)
        val testFlow = flow {
            emit(sync.receive())
            emit(sync.receive())
        }.bulkhead(bulkhead)

        val firstCall = launch {
            testFlow.toFastList(results)
        }

        // Wait until our first coroutine is inside the bulkhead
        sync.send(1)

        permittedEvents shouldBeEqualTo 1
        rejectedEvents shouldBeEqualTo 0
        finishedEvents shouldBeEqualTo 0
        results shouldContainSame listOf(1)

        val helloWorldService = SuspendHelloWorldService()
        assertFailsWith<BulkheadFullException> {
            bulkhead.executeSuspendFunction {
                helloWorldService.returnHelloWorld()
            }
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
        val helloWorldService = SuspendHelloWorldService()

        assertFailsWith<IllegalStateException> {
            bulkhead.executeSuspendFunction {
                helloWorldService.throwException()
            }
        }

        permittedEvents shouldBeEqualTo 1
        rejectedEvents shouldBeEqualTo 0
        finishedEvents shouldBeEqualTo 1

        helloWorldService.invocationCount shouldBeEqualTo 1
    }

    @Test
    fun `suspend 함수 decorate 하기`() = runSuspendTest {
        val bulkhead = Bulkhead.ofDefaults("testName").registerEventListener()
        val helloWorldService = SuspendHelloWorldService()

        val function = bulkhead.decorateSuspendFunction {
            helloWorldService.returnHelloWorld()
        }

        function() shouldBeEqualTo "Hello world"

        permittedEvents shouldBeEqualTo 1
        rejectedEvents shouldBeEqualTo 0
        finishedEvents shouldBeEqualTo 1

        helloWorldService.invocationCount shouldBeEqualTo 1
    }
}
