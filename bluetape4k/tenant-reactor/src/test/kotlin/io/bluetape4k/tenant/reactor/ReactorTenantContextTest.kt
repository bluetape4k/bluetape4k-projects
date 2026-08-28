package io.bluetape4k.tenant.reactor

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeSameInstanceAs
import io.bluetape4k.tenant.MissingTenantContextException
import io.bluetape4k.tenant.TenantId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.test.StepVerifier
import reactor.util.context.Context
import java.time.Duration
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class ReactorTenantContextTest {

    @Test
    fun `private key로 원본 Context를 변경하지 않고 tenant를 binding한다`() {
        val original = Context.of("tenantId", TenantId("collision"))
        val tenantId = TenantId("clinic-a")

        val derived = ReactorTenantContext.withTenant(original, tenantId)

        derived shouldNotBeSameInstanceAs original
        ReactorTenantContext.currentOrNull(original).shouldBeNull()
        ReactorTenantContext.requireCurrent(derived) shouldBeEqualTo tenantId
    }

    @Test
    fun `nested derived Context는 outer Context를 변경하지 않는다`() {
        val outer = ReactorTenantContext.withTenant(Context.empty(), TenantId("clinic-a"))
        val inner = ReactorTenantContext.withTenant(outer, TenantId("clinic-b"))

        ReactorTenantContext.requireCurrent(outer) shouldBeEqualTo TenantId("clinic-a")
        ReactorTenantContext.requireCurrent(inner) shouldBeEqualTo TenantId("clinic-b")
    }

    @Test
    fun `missing context는 공통 예외로 즉시 실패한다`() {
        val failure = assertFailsWith<MissingTenantContextException> {
            ReactorTenantContext.requireCurrent(Context.empty())
        }

        failure.message shouldBeEqualTo "Tenant context is not bound"
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    fun `single scheduler의 overlapping subscription은 자기 tenant만 읽는다`() {
        val scheduler = Schedulers.newSingle("tenant-context-test")
        val executor = Executors.newFixedThreadPool(2)
        val start = CyclicBarrier(2)
        val first = TenantId("clinic-a")
        val second = TenantId("clinic-b")

        try {
            val firstResult = executor.submit<List<TenantId>> {
                start.await(5, TimeUnit.SECONDS)
                tenantSequence(first, scheduler).collectList().block(Duration.ofSeconds(10))!!
            }
            val secondResult = executor.submit<List<TenantId>> {
                start.await(5, TimeUnit.SECONDS)
                tenantSequence(second, scheduler).collectList().block(Duration.ofSeconds(10))!!
            }

            firstResult.get(15, TimeUnit.SECONDS).all { it == first }.shouldBeTrue()
            secondResult.get(15, TimeUnit.SECONDS).all { it == second }.shouldBeTrue()
        } finally {
            executor.shutdownNow()
            executor.awaitTermination(5, TimeUnit.SECONDS).shouldBeTrue()
            scheduler.dispose()
        }
        scheduler.isDisposed.shouldBeTrue()
    }

    @Test
    fun `subscription 취소 후 외부 Context는 unbound다`() {
        val cancelled = AtomicBoolean()
        val tenantId = TenantId("clinic-a")
        val publisher = Flux.deferContextual { context ->
            Flux.concat(
                Mono.just(ReactorTenantContext.requireCurrent(context)),
                Flux.never<TenantId>(),
            )
        }
            .doOnCancel { cancelled.set(true) }
            .contextWrite { ReactorTenantContext.withTenant(it, tenantId) }

        StepVerifier.create(publisher)
            .expectNext(tenantId)
            .thenCancel()
            .verify(Duration.ofSeconds(5))

        cancelled.get().shouldBeTrue()
        ReactorTenantContext.currentOrNull(Context.empty()).shouldBeNull()
    }

    @Test
    fun `tenant binding은 subscription boundary에서 한 번만 수행한다`() {
        val bindingCount = AtomicInteger()
        val tenantId = TenantId("clinic-a")

        val observed = Flux.deferContextual { context ->
            Flux.range(0, 100).map { ReactorTenantContext.requireCurrent(context) }
        }.contextWrite {
            bindingCount.incrementAndGet()
            ReactorTenantContext.withTenant(it, tenantId)
        }.collectList().block(Duration.ofSeconds(5))!!

        bindingCount.get() shouldBeEqualTo 1
        observed.all { it == tenantId }.shouldBeTrue()
        ReactorTenantContext.currentOrNull(Context.empty()).shouldBeNull()
    }

    private fun tenantSequence(
        tenantId: TenantId,
        scheduler: reactor.core.scheduler.Scheduler,
    ): Flux<TenantId> =
        Flux.deferContextual { context ->
            Flux.range(0, 32)
                .delayElements(Duration.ofMillis(1), scheduler)
                .map { ReactorTenantContext.requireCurrent(context) }
        }.contextWrite { ReactorTenantContext.withTenant(it, tenantId) }
}
