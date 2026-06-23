package io.bluetape4k.mutiny

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.awaitSuspending
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicInteger
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

class CoroutineSupportTest {

    companion object: KLoggingChannel()

    @Test
    fun `suspend 함수를 Uni로 변환하기`() = runTest {
        val expected1 = 42L
        val expected2 = 43L

        val defaultScope = CoroutineScope(Dispatchers.Default)
        val u1: Uni<Long> = defaultScope.asUni {
            delay(100L.milliseconds)
            log.debug { "suspend method 1 실행 in Uni" }
            expected1
        }

        val ioScope = CoroutineScope(Dispatchers.IO)
        val u2: Uni<Long> = ioScope.asUni {
            delay(100L.milliseconds)
            log.debug { "suspend method 2 실행 in Uni" }
            expected2
        }
        log.debug { "Await ..." }
        yield()

        u1.awaitSuspending() shouldBeEqualTo expected1
        u2.awaitSuspending() shouldBeEqualTo expected2
        log.debug { "Done" }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `asUni does not start suspend block before subscription`() = runTest {
        val executions = AtomicInteger()
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        val uni = scope.asUni {
            executions.incrementAndGet()
            42L
        }

        executions.get() shouldBeEqualTo 0
        uni.awaitSuspending() shouldBeEqualTo 42L
        executions.get() shouldBeEqualTo 1
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `asUni cancellation cancels running coroutine`() = runTest {
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        val cancellable = scope.asUni {
            started.complete(Unit)
            try {
                awaitCancellation()
            } finally {
                cancelled.complete(Unit)
            }
        }.subscribe().with(
            { error("Unexpected item: $it") },
            { error("Unexpected failure: $it") },
        )

        started.await()
        cancellable.cancel()

        cancelled.await()
        cancelled.isCompleted shouldBeEqualTo true
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `asUni propagates failure and cancellation exceptions`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        assertFailsWith<IllegalStateException> {
            scope.asUni<Long> {
                throw IllegalStateException("boom")
            }.awaitSuspending()
        }

        assertFailsWith<CancellationException> {
            scope.asUni<Long> {
                throw CancellationException("cancelled")
            }.awaitSuspending()
        }
    }
}
