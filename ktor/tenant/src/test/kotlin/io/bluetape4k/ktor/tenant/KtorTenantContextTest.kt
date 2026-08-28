package io.bluetape4k.ktor.tenant

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.tenant.MissingTenantContextException
import io.bluetape4k.tenant.TenantId
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.util.AttributeKey
import io.ktor.util.Attributes
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class KtorTenantContextTest {

    @Test
    fun `unbound call은 공통 missing 예외를 던진다`() {
        val call = newCall()

        KtorTenantContext.currentOrNull(call).shouldBeNull()
        val failure = assertFailsWith<MissingTenantContextException> {
            KtorTenantContext.requireCurrent(call)
        }
        failure.message shouldBeEqualTo "Tenant context is not bound"
    }

    @Test
    fun `같은 이름의 외부 TenantId key와 충돌하지 않는다`() {
        val call = newCall()
        val externalKey = AttributeKey<TenantId>(BINDING_KEY_NAME)
        val externalTenant = TenantId("external")
        val boundTenant = TenantId("clinic-a")
        call.attributes.put(externalKey, externalTenant)

        KtorTenantContext.currentOrNull(call).shouldBeNull()
        KtorTenantContext.bindTenant(call, boundTenant)

        call.attributes[externalKey] shouldBeEqualTo externalTenant
        KtorTenantContext.requireCurrent(call) shouldBeEqualTo boundTenant
    }

    @Test
    fun `두 번째 binding은 기존 tenant를 덮어쓰지 않는다`() {
        val call = newCall()
        KtorTenantContext.bindTenant(call, TenantId("clinic-a"))

        val failure = assertFailsWith<TenantAlreadyBoundException> {
            KtorTenantContext.bindTenant(call, TenantId("clinic-b"))
        }

        failure.message shouldBeEqualTo "Tenant context is already bound to this call"
        KtorTenantContext.requireCurrent(call) shouldBeEqualTo TenantId("clinic-a")
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun `동시 binding은 정확히 한 tenant만 선형화한다`() {
        val executor = Executors.newFixedThreadPool(CONTENDERS)

        try {
            repeat(CONCURRENT_ROUNDS) { round ->
                val call = newCall()
                val start = CyclicBarrier(CONTENDERS)
                val attempts = (0 until CONTENDERS).map { contender ->
                    val tenantId = TenantId("round-$round-contender-$contender")
                    executor.submit(Callable {
                        start.await(5, TimeUnit.SECONDS)
                        runCatching {
                            KtorTenantContext.bindTenant(call, tenantId)
                            tenantId
                        }
                    })
                }.map { it.get(10, TimeUnit.SECONDS) }

                val winners = attempts.mapNotNull { it.getOrNull() }
                val failures = attempts.mapNotNull { it.exceptionOrNull() }
                winners.size shouldBeEqualTo 1
                failures.size shouldBeEqualTo CONTENDERS - 1
                failures.all {
                    it is TenantAlreadyBoundException &&
                        it.message == "Tenant context is already bound to this call"
                }.shouldBeTrue()
                KtorTenantContext.requireCurrent(call) shouldBeEqualTo winners.single()
            }
        } finally {
            shutdown(executor)
        }
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    fun `dispatcher hop과 exception cancellation 뒤 새 call은 격리된다`() = testApplication {
        val dispatcherTenant = AtomicReference<TenantId?>()
        val exceptionTenant = AtomicReference<TenantId?>()
        val cancellationTenant = AtomicReference<TenantId?>()
        val newCallTenant = AtomicReference<TenantId?>()

        application {
            routing {
                get("/dispatcher") {
                    KtorTenantContext.bindTenant(call, TenantId("clinic-dispatcher"))
                    dispatcherTenant.set(withContext(Dispatchers.Default) {
                        KtorTenantContext.requireCurrent(call)
                    })
                    call.respondText("ok")
                }
                get("/exception") {
                    KtorTenantContext.bindTenant(call, TenantId("clinic-exception"))
                    runCatching {
                        withContext(Dispatchers.Default) { throw ExpectedFailure() }
                    }
                    exceptionTenant.set(KtorTenantContext.requireCurrent(call))
                    call.respondText("ok")
                }
                get("/cancellation") {
                    KtorTenantContext.bindTenant(call, TenantId("clinic-cancellation"))
                    launch { awaitCancellation() }.cancelAndJoin()
                    cancellationTenant.set(KtorTenantContext.requireCurrent(call))
                    call.respondText("ok")
                }
                get("/new-call") {
                    newCallTenant.set(KtorTenantContext.currentOrNull(call))
                    call.respondText("ok")
                }
            }
        }

        client.get("/dispatcher").status shouldBeEqualTo HttpStatusCode.OK
        client.get("/exception").status shouldBeEqualTo HttpStatusCode.OK
        client.get("/cancellation").status shouldBeEqualTo HttpStatusCode.OK
        client.get("/new-call").status shouldBeEqualTo HttpStatusCode.OK

        dispatcherTenant.get() shouldBeEqualTo TenantId("clinic-dispatcher")
        exceptionTenant.get() shouldBeEqualTo TenantId("clinic-exception")
        cancellationTenant.get() shouldBeEqualTo TenantId("clinic-cancellation")
        newCallTenant.get().shouldBeNull()
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    fun `cancelled call을 adapter가 보존하지 않는다`() {
        val probe = createCancelledCallProbe()

        awaitCollection(probe, Duration.ofSeconds(10)).shouldBeTrue()
    }

    private fun newCall(): ApplicationCall {
        val attributes = Attributes(concurrent = false)
        return mockk<ApplicationCall>().also { call ->
            every { call.attributes } returns attributes
        }
    }

    private fun createCancelledCallProbe(): CallRetentionProbe {
        val queue = ReferenceQueue<ApplicationCall>()
        var reference: WeakReference<ApplicationCall>? = null

        testApplication {
            application {
                routing {
                    get("/cancel") {
                        KtorTenantContext.bindTenant(call, TenantId("clinic-cancelled"))
                        reference = WeakReference(call, queue)
                        throw java.util.concurrent.CancellationException("cancelled request")
                    }
                }
            }
            runCatching { client.get("/cancel") }
        }

        return CallRetentionProbe(checkNotNull(reference), queue)
    }

    private fun awaitCollection(probe: CallRetentionProbe, timeout: Duration): Boolean {
        val deadline = System.nanoTime() + timeout.toNanos()
        while (System.nanoTime() < deadline) {
            if (probe.queue.poll() === probe.reference || probe.reference.get() == null) {
                return true
            }
            System.gc()
            ByteArray(1024 * 1024)
            Thread.sleep(50)
        }
        return probe.queue.poll() === probe.reference || probe.reference.get() == null
    }

    private fun shutdown(executor: ExecutorService) {
        executor.shutdownNow()
        executor.awaitTermination(5, TimeUnit.SECONDS).shouldBeTrue()
    }

    private data class CallRetentionProbe(
        val reference: WeakReference<ApplicationCall>,
        val queue: ReferenceQueue<ApplicationCall>,
    )

    private class ExpectedFailure: RuntimeException()

    companion object {
        private const val BINDING_KEY_NAME = "io.bluetape4k.ktor.tenant.binding.v1"
        private const val CONTENDERS = 8
        private const val CONCURRENT_ROUNDS = 100
    }
}
