package io.bluetape4k.r2dbc.pool

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TenantConnectionRegistryTest {

    @Test
    fun `caller-owned factory registry routes configured keys without taking lifecycle ownership`() {
        val tenantA = mockk<ConnectionPool>(relaxed = true)
        val tenantB = mockk<ConnectionFactory>(relaxed = true)
        val registry = TenantConnectionFactoryRegistry(
            mapOf(
                TenantKey("tenant-a") to tenantA,
                TenantKey("tenant-b") to tenantB,
            ),
        )

        registry[TenantKey("tenant-a")] shouldBeSameInstanceAs tenantA
        registry.get(TenantKey("tenant-b")) shouldBeSameInstanceAs tenantB
        registry.configuredKeys shouldBeEqualTo setOf(TenantKey("tenant-a"), TenantKey("tenant-b"))
        registry.asRoutingMap()[TenantKey("tenant-a")] shouldBeSameInstanceAs tenantA
        registry.asRoutingMap { it.id }["tenant-b"] shouldBeSameInstanceAs tenantB

        registry[TenantKey("tenant-a")]
        verify(exactly = 0) { tenantA.dispose() }
    }

    @Test
    fun `unknown tenant lookup fails fast`() {
        val registry = TenantConnectionFactoryRegistry(
            mapOf(TenantKey("tenant-a") to mockk<ConnectionFactory>(relaxed = true)),
        )

        val failure = assertFailsWith<NoSuchElementException> {
            registry[TenantKey("unknown")]
        }

        failure.message.orEmpty().contains("tenant key") shouldBeEqualTo true
    }

    @Test
    fun `routing key mapper 충돌은 무음 덮어쓰기 대신 즉시 실패한다`() {
        val registry = TenantConnectionFactoryRegistry(
            mapOf(
                TenantKey("tenant-a") to mockk<ConnectionFactory>(relaxed = true),
                TenantKey("tenant-b") to mockk<ConnectionFactory>(relaxed = true),
            ),
        )

        val failure = assertFailsWith<IllegalArgumentException> {
            registry.asRoutingMap { "shared-route" }
        }

        failure.message.orEmpty().contains("Duplicate routing key") shouldBeEqualTo true
    }

    @Test
    fun `concurrent tenant lookups remain isolated`() {
        val tenantA = mockk<ConnectionFactory>(relaxed = true)
        val tenantB = mockk<ConnectionFactory>(relaxed = true)
        val registry = TenantConnectionFactoryRegistry(
            mapOf(
                TenantKey("tenant-a") to tenantA,
                TenantKey("tenant-b") to tenantB,
            ),
        )

        MultithreadingTester()
            .workers(2)
            .rounds(256)
            .add { registry[TenantKey("tenant-a")] shouldBeSameInstanceAs tenantA }
            .add { registry[TenantKey("tenant-b")] shouldBeSameInstanceAs tenantB }
            .run()
    }

    @Test
    fun `registry-owned pools are disposed exactly once`() {
        val tenantA = mockk<ConnectionPool>(relaxed = true)
        val tenantB = mockk<ConnectionPool>(relaxed = true)
        val registry = TenantConnectionPoolRegistry(
            mapOf(
                TenantKey("tenant-a") to tenantA,
                TenantKey("tenant-b") to tenantB,
            ),
        )

        registry.close()
        registry.close()

        verify(exactly = 1) { tenantA.dispose() }
        verify(exactly = 1) { tenantB.dispose() }
    }

    @Test
    fun `concurrent close는 첫 종료가 완료될 때까지 기다린다`() {
        val disposeStarted = CountDownLatch(1)
        val allowDispose = CountDownLatch(1)
        val concurrentCloseStarted = CountDownLatch(1)
        val pool = mockk<ConnectionPool>(relaxed = true)
        every { pool.dispose() } answers {
            disposeStarted.countDown()
            allowDispose.await()
        }
        val registry = TenantConnectionPoolRegistry(mapOf(TenantKey("tenant-a") to pool))
        val executor = Executors.newFixedThreadPool(2)

        try {
            val firstClose = executor.submit(registry::close)
            disposeStarted.await(5, TimeUnit.SECONDS) shouldBeEqualTo true
            val concurrentClose = executor.submit {
                concurrentCloseStarted.countDown()
                registry.close()
            }

            concurrentCloseStarted.await(5, TimeUnit.SECONDS) shouldBeEqualTo true
            concurrentClose.isDone shouldBeEqualTo false
            allowDispose.countDown()
            firstClose.get(5, TimeUnit.SECONDS)
            concurrentClose.get(5, TimeUnit.SECONDS)
        } finally {
            allowDispose.countDown()
            executor.shutdownNow()
        }

        verify(exactly = 1) { pool.dispose() }
    }

    @Test
    fun `registry-owned pool은 close 시작 후 조회를 거부한다`() {
        val pool = mockk<ConnectionPool>(relaxed = true)
        val registry = TenantConnectionPoolRegistry(
            mapOf(TenantKey("tenant-a") to pool),
        )

        registry.close()

        assertFailsWith<IllegalStateException> {
            registry[TenantKey("tenant-a")]
        }
        assertFailsWith<IllegalStateException> {
            registry.asRoutingMap()
        }
    }

    @Test
    fun `closeSuspending은 coroutine caller의 blocking 작업을 IO dispatcher로 격리한다`() = runSuspendIO {
        val pool = mockk<ConnectionPool>(relaxed = true)
        val registry = TenantConnectionPoolRegistry(mapOf(TenantKey("tenant-a") to pool))

        registry.closeSuspending()

        verify(exactly = 1) { pool.dispose() }
    }

    @Test
    fun `closeSuspending은 취소된 caller에서도 pool cleanup을 완료하고 취소를 전파한다`() = runSuspendIO {
        val pool = mockk<ConnectionPool>(relaxed = true)
        val registry = TenantConnectionPoolRegistry(mapOf(TenantKey("tenant-a") to pool))

        val caller = launch(start = CoroutineStart.UNDISPATCHED) {
            currentCoroutineContext().cancel()
            registry.closeSuspending()
        }
        caller.join()

        caller.isCancelled shouldBeEqualTo true
        verify(exactly = 1) { pool.dispose() }
    }

    @Test
    fun `registry-owned pool cleanup continues and suppresses later failures`() {
        val tenantA = mockk<ConnectionPool>(relaxed = true)
        val tenantB = mockk<ConnectionPool>(relaxed = true)
        val tenantC = mockk<ConnectionPool>(relaxed = true)
        val firstFailure = IllegalStateException("tenant-a close failed")
        val secondFailure = IllegalArgumentException("tenant-b close failed")
        every { tenantA.dispose() } throws firstFailure
        every { tenantB.dispose() } throws secondFailure

        val registry = TenantConnectionPoolRegistry(
            mapOf(
                TenantKey("tenant-a") to tenantA,
                TenantKey("tenant-b") to tenantB,
                TenantKey("tenant-c") to tenantC,
            ),
        )

        val failure = assertFailsWith<IllegalStateException> { registry.close() }
        val repeatedFailure = assertFailsWith<IllegalStateException> { registry.close() }

        failure shouldBeSameInstanceAs firstFailure
        repeatedFailure shouldBeSameInstanceAs firstFailure
        failure.suppressed.single() shouldBeSameInstanceAs secondFailure
        verify(exactly = 1) { tenantA.dispose() }
        verify(exactly = 1) { tenantB.dispose() }
        verify(exactly = 1) { tenantC.dispose() }
    }

    @Test
    fun `registry-owned pool cleanup은 Error를 suppressed 처리하지 않고 즉시 전파한다`() {
        val tenantA = mockk<ConnectionPool>(relaxed = true)
        val tenantB = mockk<ConnectionPool>(relaxed = true)
        val tenantC = mockk<ConnectionPool>(relaxed = true)
        every { tenantA.dispose() } throws IllegalStateException("ordinary close failure")
        every { tenantB.dispose() } throws AssertionError("fatal close failure")
        val registry = TenantConnectionPoolRegistry(
            mapOf(
                TenantKey("tenant-a") to tenantA,
                TenantKey("tenant-b") to tenantB,
                TenantKey("tenant-c") to tenantC,
            ),
        )

        val failure = assertFailsWith<AssertionError> { registry.close() }

        failure.message shouldBeEqualTo "fatal close failure"
        verify(exactly = 0) { tenantC.dispose() }
    }

    @Test
    fun `same pool shared by tenants is disposed once`() {
        val pool = mockk<ConnectionPool>(relaxed = true)
        val registry = TenantConnectionPoolRegistry(
            mapOf(
                TenantKey("tenant-a") to pool,
                TenantKey("tenant-b") to pool,
            ),
        )

        registry.close()

        verify(exactly = 1) { pool.dispose() }
    }

    private data class TenantKey(val id: String)
}
