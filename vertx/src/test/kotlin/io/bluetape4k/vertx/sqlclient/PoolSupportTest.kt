package io.bluetape4k.vertx.sqlclient

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import kotlinx.coroutines.CancellationException
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.sql.SQLException

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
}
