package io.bluetape4k.vertx.sqlclient

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.mockk.every
import io.mockk.mockk
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import org.junit.jupiter.api.Test
import java.sql.SQLException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class PoolSupportTest: AbstractVertxSqlClientTest() {

    override val schemaFileNames: List<String> = listOf("person.sql")

    @Test
    fun `withSuspendTransaction은 CancellationException을 그대로 전파한다`(
        @Suppress("UNUSED_PARAMETER") unusedVertx: Vertx,
        testContext: VertxTestContext,
    ) = runSuspendIO {
        try {
            val result = runCatching {
                pool.withSuspendTransaction { throw CancellationException("cancel requested") }
            }
            result.isFailure.shouldBeTrue()
            val error = result.exceptionOrNull().shouldNotBeNull()
            error::class shouldBeEqualTo CancellationException::class
            error.message shouldBeEqualTo "cancel requested"
            testContext.completeNow()
        } catch (t: Throwable) {
            testContext.failNow(t)
        }
    }

    @Test
    fun `withSuspendTransaction은 취소된 컨텍스트에서도 rollback과 close 완료를 보장한다`() = runSuspendIO {
        val rollbackCompleted = AtomicBoolean(false)
        val closeCompleted = AtomicBoolean(false)
        val pool = poolWithCleanupFutures(
            rollbackFuture = { delayedSucceededFuture { rollbackCompleted.set(true) } },
            closeFuture = { delayedSucceededFuture { closeCompleted.set(true) } },
        )

        val error = assertFailsWith<CancellationException> {
            runInCancelledChild {
                pool.withSuspendTransaction {
                    val cancellation = CancellationException("cancel requested")
                    currentCoroutineContext().cancel(cancellation)
                    throw cancellation
                }
            }
        }

        error.message shouldBeEqualTo "cancel requested"
        rollbackCompleted.get().shouldBeTrue()
        closeCompleted.get().shouldBeTrue()
    }

    @Test
    fun `withSuspendRollback은 CancellationException을 그대로 전파한다`(
        @Suppress("UNUSED_PARAMETER") unusedVertx: Vertx,
        testContext: VertxTestContext,
    ) = runSuspendIO {
        try {
            val result = runCatching {
                pool.withSuspendRollback { throw CancellationException("cancel requested") }
            }
            result.isFailure.shouldBeTrue()
            val error = result.exceptionOrNull().shouldNotBeNull()
            error::class shouldBeEqualTo CancellationException::class
            error.message shouldBeEqualTo "cancel requested"
            testContext.completeNow()
        } catch (t: Throwable) {
            testContext.failNow(t)
        }
    }

    @Test
    fun `withSuspendRollback은 취소된 컨텍스트에서도 rollback과 close 완료를 보장한다`() = runSuspendIO {
        val rollbackCompleted = AtomicBoolean(false)
        val closeCompleted = AtomicBoolean(false)
        val pool = poolWithCleanupFutures(
            rollbackFuture = { delayedSucceededFuture { rollbackCompleted.set(true) } },
            closeFuture = { delayedSucceededFuture { closeCompleted.set(true) } },
        )

        val error = assertFailsWith<CancellationException> {
            runInCancelledChild {
                pool.withSuspendRollback {
                    val cancellation = CancellationException("cancel requested")
                    currentCoroutineContext().cancel(cancellation)
                    throw cancellation
                }
            }
        }

        error.message shouldBeEqualTo "cancel requested"
        rollbackCompleted.get().shouldBeTrue()
        closeCompleted.get().shouldBeTrue()
    }

    @Test
    fun `cleanup 실패는 CancellationException의 suppressed 예외로 보존한다`() = runSuspendIO {
        val rollbackFailure = IllegalStateException("rollback failed")
        val closeFailure = IllegalArgumentException("close failed")
        val pool = poolWithCleanupFutures(
            rollbackFuture = { Future.failedFuture(rollbackFailure) },
            closeFuture = { Future.failedFuture(closeFailure) },
        )

        val error = assertFailsWith<CancellationException> {
            pool.withSuspendTransaction {
                throw CancellationException("cancel requested")
            }
        }

        error.message shouldBeEqualTo "cancel requested"
        error.suppressed.any { it is IllegalStateException && it.message == rollbackFailure.message }.shouldBeTrue()
        error.suppressed.any { it is IllegalArgumentException && it.message == closeFailure.message }.shouldBeTrue()
    }

    @Test
    fun `withSuspendTransaction은 일반 예외를 SQLException으로 래핑한다`(
        @Suppress("UNUSED_PARAMETER") unusedVertx: Vertx,
        testContext: VertxTestContext,
    ) = runSuspendIO {
        try {
            val result = runCatching {
                pool.withSuspendTransaction { throw IllegalStateException("boom") }
            }
            result.isFailure.shouldBeTrue()
            val error = result.exceptionOrNull().shouldNotBeNull()
            error::class shouldBeEqualTo SQLException::class
            error.cause.shouldNotBeNull()::class shouldBeEqualTo IllegalStateException::class
            testContext.completeNow()
        } catch (t: Throwable) {
            testContext.failNow(t)
        }
    }

    private suspend fun runInCancelledChild(block: suspend () -> Unit) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val deferred = scope.async { block() }
            deferred.await()
        } finally {
            scope.cancel()
        }
    }

    private fun poolWithCleanupFutures(
        rollbackFuture: () -> Future<Void>,
        closeFuture: () -> Future<Void>,
    ): Pool {
        val pool = mockk<Pool>()
        val conn = mockk<SqlConnection>()
        val tx = mockk<Transaction>()

        every { pool.connection } returns Future.succeededFuture(conn)
        every { conn.begin() } returns Future.succeededFuture(tx)
        every { tx.rollback() } answers { rollbackFuture() }
        every { conn.close() } answers { closeFuture() }

        return pool
    }

    private fun delayedSucceededFuture(onComplete: () -> Unit): Future<Void> {
        val promise = Promise.promise<Void>()
        thread(start = true, isDaemon = true, name = "vertx-cleanup-test") {
            Thread.sleep(50)
            onComplete()
            promise.complete()
        }
        return promise.future()
    }
}
