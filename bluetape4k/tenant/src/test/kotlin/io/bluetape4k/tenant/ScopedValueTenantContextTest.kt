package io.bluetape4k.tenant

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.Callable
import java.util.concurrent.StructuredTaskScope
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class ScopedValueTenantContextTest {

    @Test
    fun `public 기본 생성자와 unbound 계약을 제공한다`() {
        val context: TenantContext = ScopedValueTenantContext()

        context.currentOrNull().shouldBeNull()
        val failure = assertFailsWith<MissingTenantContextException> {
            context.requireCurrent()
        }
        failure.message shouldBeEqualTo "Tenant context is not bound"
    }

    @Test
    fun `중첩 binding 성공과 실패 후 lexical tenant를 복원한다`() {
        val context = ScopedValueTenantContext()
        val outer = TenantId("clinic-a")

        context.withTenant(outer) {
            context.requireCurrent() shouldBeEqualTo outer
            context.withTenant(TenantId("clinic-b")) {
                context.requireCurrent() shouldBeEqualTo TenantId("clinic-b")
            }
            context.requireCurrent() shouldBeEqualTo outer

            assertFailsWith<ExpectedFailure> {
                context.withTenant(TenantId("clinic-c")) {
                    throw ExpectedFailure()
                }
            }
            context.requireCurrent() shouldBeEqualTo outer
        }

        context.currentOrNull().shouldBeNull()
    }

    @Test
    fun `서로 다른 carrier instance는 key를 공유하지 않는다`() {
        val first = ScopedValueTenantContext()
        val second = ScopedValueTenantContext()

        first.withTenant(TenantId("clinic-a")) {
            first.requireCurrent() shouldBeEqualTo TenantId("clinic-a")
            second.currentOrNull().shouldBeNull()
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `StructuredTaskScope fork는 lexical tenant를 상속한다`() {
        val context = ScopedValueTenantContext()
        val tenantId = TenantId("clinic-a")

        val observed = context.withTenant(tenantId) {
            StructuredTaskScope.open<TenantId, Void>(StructuredTaskScope.Joiner.awaitAll()).use { scope ->
                val subtask = scope.fork(Callable { context.requireCurrent() })
                scope.join()
                subtask.get()
            }
        }

        observed shouldBeEqualTo tenantId
        context.currentOrNull().shouldBeNull()
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `독립적으로 시작한 virtual thread에는 tenant를 전파하지 않는다`() {
        val context = ScopedValueTenantContext()
        val observed = AtomicReference<TenantId?>()

        context.withTenant(TenantId("clinic-a")) {
            Thread.ofVirtual().start {
                observed.set(context.currentOrNull())
            }.join()
        }

        observed.get().shouldBeNull()
        context.currentOrNull().shouldBeNull()
    }

    private class ExpectedFailure: RuntimeException()
}
