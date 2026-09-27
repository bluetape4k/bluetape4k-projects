package io.bluetape4k.ktor.core

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotContain
import io.bluetape4k.concurrent.await
import io.bluetape4k.concurrent.get
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.Job
import org.junit.jupiter.api.Assertions.assertTimeout
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

class ApplicationResourceRegistryTest {

    companion object: KLoggingChannel()

    @Test
    fun `registry starts open with an empty report`() {
        val registry = ApplicationResourceRegistry()

        registry.closeReport shouldBeEqualTo ApplicationResourceCloseReport(
            state = ApplicationResourceRegistryState.OPEN,
            attempted = 0,
            inFlight = 0,
            closed = 0,
            failures = emptyList()
        )
    }

    @Test
    fun `registry closes resources exactly once in reverse registration order`() {
        val closed = mutableListOf<String>()
        val registry = ApplicationResourceRegistry()

        registry.register { closed += "first" }
        registry.register { closed += "second" }
        registry.register { closed += "third" }

        registry.close()
        registry.close()

        closed shouldBeEqualTo listOf("third", "second", "first")
        registry.closeReport.closed shouldBeEqualTo 3
        registry.closeReport.attempted shouldBeEqualTo 3
    }

    @Test
    fun `registration token closes its resource early and registry excludes it`() {
        val closed = mutableListOf<String>()
        val registry = ApplicationResourceRegistry()
        val early = registry.register { closed += "early" }
        registry.register { closed += "shutdown" }

        early.close()
        early.close()

        registry.closeReport shouldBeEqualTo ApplicationResourceCloseReport(
            state = ApplicationResourceRegistryState.OPEN,
            attempted = 1,
            inFlight = 0,
            closed = 1,
            failures = emptyList()
        )

        registry.close()

        closed shouldBeEqualTo listOf("early", "shutdown")
        registry.closeReport shouldBeEqualTo ApplicationResourceCloseReport(
            state = ApplicationResourceRegistryState.CLOSED,
            attempted = 2,
            inFlight = 0,
            closed = 2,
            failures = emptyList()
        )
    }

    @Test
    fun `duplicate resource and action identities are rejected without information disclosure`() {
        val registry = ApplicationResourceRegistry()
        val resource = NamedResource("credential-secret")
        val action: () -> Unit = { }

        registry.register(resource)
        val resourceFailure = assertFailsWith<IllegalArgumentException> {
            registry.register(resource)
        }

        registry.register(action)
        val actionFailure = assertFailsWith<IllegalArgumentException> {
            registry.register(action)
        }

        resourceFailure.message shouldNotContain "credential-secret"
        resourceFailure.message shouldNotContain "NamedResource"
        actionFailure.message shouldNotContain "Function"
    }

    @Test
    fun `late registration closes immediately and is recorded in the cumulative report`() {
        val closed = mutableListOf<String>()
        val registry = ApplicationResourceRegistry()
        registry.close()

        val late = registry.register { closed += "late" }
        late.close()

        closed shouldBeEqualTo listOf("late")
        registry.closeReport shouldBeEqualTo ApplicationResourceCloseReport(
            state = ApplicationResourceRegistryState.CLOSED,
            attempted = 1,
            inFlight = 0,
            closed = 1,
            failures = emptyList()
        )
    }

    @Test
    fun `early close failure is isolated and reported with the early phase`() {
        val registry = ApplicationResourceRegistry()
        val registration = registry.register { error("credential-secret") }

        registration.close()

        registry.closeReport shouldBeEqualTo ApplicationResourceCloseReport(
            state = ApplicationResourceRegistryState.OPEN,
            attempted = 1,
            inFlight = 0,
            closed = 0,
            failures = listOf(
                ApplicationResourceCloseFailure(
                    registrationId = registration.id,
                    phase = ApplicationResourceClosePhase.EARLY,
                    fatal = false
                )
            )
        )
        registry.close()
    }

    @Test
    fun `late close failure is isolated and reported with the late phase`() {
        val registry = ApplicationResourceRegistry()
        registry.close()

        registry.register { error("credential-secret") }

        val report = registry.closeReport

        report.state shouldBeEqualTo ApplicationResourceRegistryState.CLOSED
        report.attempted shouldBeEqualTo 1
        report.inFlight shouldBeEqualTo 0
        report.closed shouldBeEqualTo 0
        report.failures shouldBeEqualTo listOf(
            ApplicationResourceCloseFailure(
                registrationId = 1,
                phase = ApplicationResourceClosePhase.LATE_REGISTRATION,
                fatal = false
            )
        )
    }

    @Test
    fun `shutdown close continues after ordinary failures and reports its phase`() {
        val closed = mutableListOf<String>()
        val registry = ApplicationResourceRegistry()
        registry.register { closed += "first" }

        val failing = registry.register { error("credential-secret") }
        registry.register { closed += "last" }

        registry.close()

        closed shouldBeEqualTo listOf("last", "first")

        val report = registry.closeReport
        report.state shouldBeEqualTo ApplicationResourceRegistryState.CLOSED
        report.attempted shouldBeEqualTo 3
        report.inFlight shouldBeEqualTo 0
        report.closed shouldBeEqualTo 2
        report.failures shouldBeEqualTo listOf(
            ApplicationResourceCloseFailure(
                registrationId = failing.id,
                phase = ApplicationResourceClosePhase.SHUTDOWN,
                fatal = false
            )
        )
        report.toString() shouldNotContain "credential-secret"
    }

    @Test
    fun `report invariant remains valid while shutdown action is in flight`() {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val registry = ApplicationResourceRegistry()

        registry.register {
            started.countDown()
            release.await(5.seconds).shouldBeTrue()
        }
        val executor = Executors.newSingleThreadExecutor()

        try {
            val closeFuture = executor.submit { registry.close() }
            started.await(5.seconds).shouldBeTrue()

            val report = registry.closeReport
            report.state shouldBeEqualTo ApplicationResourceRegistryState.DRAINING
            report.attempted shouldBeEqualTo 1
            report.inFlight shouldBeEqualTo 1
            report.closed shouldBeEqualTo 0
            report.failures.shouldBeEmpty()

            release.countDown()
            closeFuture.get(5.seconds)
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `reentrant close and late registration do not deadlock`() {
        val closed = mutableListOf<String>()
        val registry = ApplicationResourceRegistry()

        registry.register {
            closed += "shutdown"
            registry.close()
            registry.register { closed += "late" }
        }

        registry.close()

        closed shouldBeEqualTo listOf("shutdown", "late")
        registry.closeReport.closed shouldBeEqualTo 2
    }

    @Test
    fun `fatal failure is sanitized after remaining resources are closed`() {
        val closed = mutableListOf<String>()
        val registry = ApplicationResourceRegistry()

        registry.register { closed += "first" }
        registry.register { throw AssertionError("credential-secret") }
        registry.register { closed += "last" }

        val marker = assertFailsWith<Error> { registry.close() }

        closed shouldBeEqualTo listOf("last", "first")
        marker.message shouldNotContain "credential-secret"
        marker.cause.shouldBeNull()
        registry.closeReport.failures.single().fatal.shouldBeTrue()
    }

    @Test
    fun `job cancellation can be adapted through a synchronous close action`() {
        val job = Job()
        val registry = ApplicationResourceRegistry()
        registry.register { job.cancel() }

        registry.close()

        job.isCancelled.shouldBeTrue()
    }

    @Test
    fun `close action executes on the caller thread`() {
        val registry = ApplicationResourceRegistry()
        val caller = Thread.currentThread()
        var closer: Thread? = null

        registry.register { closer = Thread.currentThread() }

        assertTimeout(Duration.ofSeconds(1)) {
            registry.close()
        }

        closer shouldBeSameInstanceAs caller
    }

    @Test
    fun `token and registry close race still closes each entry exactly once`() {
        repeat(8) {
            val closeCounts = List(8) { AtomicInteger() }
            val registry = ApplicationResourceRegistry()
            val registrations = closeCounts.map { count ->
                registry.register { count.incrementAndGet() }
            }
            val barrier = CyclicBarrier(registrations.size + 1)
            val executor = Executors.newFixedThreadPool(registrations.size + 1)
            try {
                val futures = registrations.map { registration ->
                    executor.submit {
                        barrier.await(5.seconds)
                        registration.close()
                    }
                }
                val closeFuture = executor.submit {
                    barrier.await(5.seconds)
                    registry.close()
                }
                futures.forEach { it.get(5.seconds) }
                closeFuture.get(5.seconds)
            } finally {
                executor.shutdownNow()
            }

            closeCounts.all { it.get() == 1 }.shouldBeTrue()
            registry.closeReport.closed shouldBeEqualTo closeCounts.size
            registry.closeReport.inFlight shouldBeEqualTo 0
        }
    }

    @Test
    fun `owned identity remains unavailable while draining and after close`() {
        val closeStarted = CountDownLatch(1)
        val allowClose = CountDownLatch(1)
        val registry = ApplicationResourceRegistry()
        val resource = AutoCloseable {
            closeStarted.countDown()
            allowClose.await(5.seconds).shouldBeTrue()
        }
        registry.register(resource)
        val executor = Executors.newSingleThreadExecutor()
        try {
            val closeFuture = executor.submit { registry.close() }
            closeStarted.await(5.seconds).shouldBeTrue()

            registry.closeReport.state shouldBeEqualTo ApplicationResourceRegistryState.DRAINING

            assertFailsWith<IllegalArgumentException> {
                registry.register(resource)
            }
            allowClose.countDown()
            closeFuture.get(5.seconds)
            assertFailsWith<IllegalArgumentException> {
                registry.register(resource)
            }
        } finally {
            allowClose.countDown()
            executor.shutdownNow()
        }

        registry.closeReport.state shouldBeEqualTo ApplicationResourceRegistryState.CLOSED
        registry.closeReport.inFlight shouldBeEqualTo 0
        registry.closeReport.failures.shouldBeEmpty()
        registry.closeReport.closed shouldBeEqualTo 1
        registry.closeReport.attempted shouldBeEqualTo 1
        registry.closeReport.failures.none { it.fatal }.shouldBeTrue()
    }

    @Test
    fun `late registration report remains valid while its close action is in flight`() {
        val closeStarted = CountDownLatch(1)
        val allowClose = CountDownLatch(1)
        val registry = ApplicationResourceRegistry()
        registry.close()

        val executor = Executors.newSingleThreadExecutor()

        try {
            val registration = executor.submit {
                registry.register {
                    closeStarted.countDown()
                    allowClose.await(5.seconds).shouldBeTrue()
                }
            }
            closeStarted.await(5.seconds).shouldBeTrue()

            registry.closeReport shouldBeEqualTo ApplicationResourceCloseReport(
                state = ApplicationResourceRegistryState.CLOSED,
                attempted = 1,
                inFlight = 1,
                closed = 0,
                failures = emptyList()
            )

            allowClose.countDown()
            registration.get(5.seconds)
        } finally {
            allowClose.countDown()
            executor.shutdownNow()
        }

        registry.closeReport.inFlight shouldBeEqualTo 0
        registry.closeReport.closed shouldBeEqualTo 1
    }

    private class NamedResource(
        private val name: String,
    ): AutoCloseable {
        override fun close() = Unit

        override fun toString(): String = name
    }
}
