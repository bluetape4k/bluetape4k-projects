package io.bluetape4k.tenant

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ThreadLocalTenantContextTest {

    @Test
    fun `public 기본 생성자와 unbound 계약을 제공한다`() {
        val context: TenantContext = ThreadLocalTenantContext()

        context.currentOrNull().shouldBeNull()
        val failure = assertFailsWith<MissingTenantContextException> {
            context.requireCurrent()
        }
        failure.message shouldBeEqualTo "Tenant context is not bound"
    }

    @Test
    fun `중첩 binding 성공 후 outer tenant를 복원한다`() {
        val context = ThreadLocalTenantContext()
        val outer = TenantId("clinic-a")
        val inner = TenantId("clinic-b")

        context.withTenant(outer) {
            context.requireCurrent() shouldBeEqualTo outer
            context.withTenant(inner) {
                context.requireCurrent() shouldBeEqualTo inner
            }
            context.requireCurrent() shouldBeEqualTo outer
        }

        context.currentOrNull().shouldBeNull()
    }

    @Test
    fun `중첩 binding 실패 후에도 outer tenant를 복원한다`() {
        val context = ThreadLocalTenantContext()
        val outer = TenantId("clinic-a")

        context.withTenant(outer) {
            assertFailsWith<ExpectedFailure> {
                context.withTenant(TenantId("clinic-b")) {
                    throw ExpectedFailure()
                }
            }
            context.requireCurrent() shouldBeEqualTo outer
        }

        context.currentOrNull().shouldBeNull()
    }

    @Test
    fun `top-level binding 실패 후 tenant를 제거한다`() {
        val context = ThreadLocalTenantContext()

        assertFailsWith<ExpectedFailure> {
            context.withTenant(TenantId("clinic-a")) {
                throw ExpectedFailure()
            }
        }

        context.currentOrNull().shouldBeNull()
    }

    @Test
    fun `서로 다른 carrier instance는 값을 공유하지 않는다`() {
        val first = ThreadLocalTenantContext()
        val second = ThreadLocalTenantContext()

        first.withTenant(TenantId("clinic-a")) {
            first.requireCurrent() shouldBeEqualTo TenantId("clinic-a")
            second.currentOrNull().shouldBeNull()
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    fun `고정 thread pool 재사용 중 tenant가 누수되지 않는다`() {
        val context = ThreadLocalTenantContext()
        val barrier = CyclicBarrier(WORKERS)
        val tenantSequence = AtomicInteger()

        MultithreadingTester()
            .workers(WORKERS)
            .rounds(ROUNDS)
            .add {
                context.currentOrNull().shouldBeNull()
                val tenantId = TenantId("clinic-${tenantSequence.getAndIncrement() % TENANTS}")

                context.withTenant(tenantId) {
                    barrier.await(5, TimeUnit.SECONDS)
                    context.requireCurrent() shouldBeEqualTo tenantId
                }

                context.currentOrNull().shouldBeNull()
            }
            .run()
    }

    private class ExpectedFailure: RuntimeException()

    companion object {
        private const val WORKERS = 8
        private const val ROUNDS = 13
        private const val TENANTS = 100
    }
}
