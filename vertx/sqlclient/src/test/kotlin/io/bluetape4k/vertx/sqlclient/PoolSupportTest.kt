package io.bluetape4k.vertx.sqlclient

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import kotlinx.coroutines.CancellationException
import org.junit.jupiter.api.Test
import java.sql.SQLException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class PoolSupportTest: AbstractVertxSqlClientTest() {

    override val schemaFileNames: List<String> = listOf("person.sql")

    @Test
    fun `withSuspendTransaction은 CancellationException을 그대로 전파한다`(
        @Suppress("UNUSED_PARAMETER") unusedVertx: Vertx,
        testContext: VertxTestContext,
    ) = runSuspendIO {
        try {
            val error = assertFailsWith<CancellationException> {
                pool.withSuspendTransaction { throw CancellationException("cancel requested") }
            }
            assertEquals("cancel requested", error.message)
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
            val error = assertFailsWith<CancellationException> {
                pool.withSuspendRollback { throw CancellationException("cancel requested") }
            }
            assertEquals("cancel requested", error.message)
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
            val error = assertFailsWith<SQLException> {
                pool.withSuspendTransaction { throw IllegalStateException("boom") }
            }
            assertIs<IllegalStateException>(error.cause)
            testContext.completeNow()
        } catch (t: Throwable) {
            testContext.failNow(t)
        }
    }
}
