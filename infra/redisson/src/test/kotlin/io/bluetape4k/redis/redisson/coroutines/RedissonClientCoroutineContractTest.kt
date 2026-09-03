package io.bluetape4k.redis.redisson.coroutines

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.redisson.api.RFuture
import org.redisson.api.RTransaction
import org.redisson.api.RedissonClient
import org.redisson.misc.CompletableFutureWrapper
import java.util.concurrent.CompletableFuture

class RedissonClientCoroutineContractTest {

    @Test
    fun `action 실패가 주 예외이고 rollback 실패는 suppressed로 보존한다`() = runTest {
        val (client, transaction) = transactionFixture()
        val actionFailure = IllegalStateException("action failed")
        val rollbackFailure = IllegalArgumentException("rollback failed")
        every { transaction.rollbackAsync() } returns failedFuture(rollbackFailure)

        val thrown = assertFailsWith<IllegalStateException> {
            client.withSuspendedTransaction {
                throw actionFailure
            }
        }

        thrown shouldBeSameInstanceAs actionFailure
        thrown.suppressed.single()
            .shouldBeInstanceOf<IllegalArgumentException>()
            .message shouldBeEqualTo rollbackFailure.message
        verify(exactly = 0) { transaction.commitAsync() }
        verify(exactly = 1) { transaction.rollbackAsync() }
    }

    @Test
    fun `caller cancellation 동안 rollback 완료를 기다리고 원래 cancellation을 보존한다`() = runTest {
        val (client, transaction) = transactionFixture()
        val callerCancellation = CancellationException("caller cancelled")
        val rollback = CompletableFuture<Void>()
        val observed = CompletableDeferred<Throwable>()
        every { transaction.rollbackAsync() } returns CompletableFutureWrapper(rollback)

        val job = launch {
            try {
                client.withSuspendedTransaction {
                    currentCoroutineContext().cancel(callerCancellation)
                    throw callerCancellation
                }
            } catch (failure: Throwable) {
                observed.complete(failure)
            }
        }

        runCurrent()
        observed.isCompleted.shouldBeFalse()

        rollback.complete(null)
        runCurrent()

        observed.await() shouldBeSameInstanceAs callerCancellation
        job.isCancelled.shouldBeTrue()
        verify(exactly = 1) { transaction.rollbackAsync() }
    }

    @Test
    fun `commit 실패가 주 예외이고 rollback을 수행한다`() = runTest {
        val (client, transaction) = transactionFixture()
        val commitFailure = IllegalStateException("commit failed")
        every { transaction.commitAsync() } returns failedFuture(commitFailure)
        every { transaction.rollbackAsync() } returns completedFuture()

        val thrown = assertFailsWith<IllegalStateException> {
            client.withSuspendedTransaction {}
        }

        thrown shouldBeSameInstanceAs commitFailure
        thrown.suppressed.isEmpty().shouldBeTrue()
        verify(exactly = 1) { transaction.commitAsync() }
        verify(exactly = 1) { transaction.rollbackAsync() }
    }

    @Test
    fun `rollback이 완료되지 않으면 제한 시간 후 주 예외에 timeout을 suppress한다`() = runTest {
        val (client, transaction) = transactionFixture()
        val actionFailure = IllegalStateException("action failed")
        every { transaction.rollbackAsync() } returns CompletableFutureWrapper(CompletableFuture<Void>())

        val thrown = assertFailsWith<IllegalStateException> {
            client.withSuspendedTransaction {
                throw actionFailure
            }
        }

        thrown shouldBeSameInstanceAs actionFailure
        (thrown.suppressed.single() is TimeoutCancellationException).shouldBeTrue()
    }

    private fun transactionFixture(): Pair<RedissonClient, RTransaction> {
        val client = mockk<RedissonClient>()
        val transaction = mockk<RTransaction>()
        every { client.createTransaction(any()) } returns transaction
        return client to transaction
    }

    private fun completedFuture(): RFuture<Void> =
        CompletableFutureWrapper.completedNull()

    private fun failedFuture(failure: Throwable): RFuture<Void> =
        CompletableFutureWrapper(failure)
}
