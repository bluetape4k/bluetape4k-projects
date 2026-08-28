package io.bluetape4k.tenant

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

@Tag("tenant-retention-stress")
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class TenantContextRetentionStressTest {

    @Test
    fun `platform과 virtual thread 반복 실행 후 tenant가 남지 않는다`() {
        verifyPlatformThreadIsolation()
        verifyVirtualThreadIsolation()
    }

    @Test
    fun `binding 종료 후 tenant sentinel을 보존하지 않는다`() {
        val probes = listOf(
            createRetentionProbe(ThreadLocalTenantContext()),
            createRetentionProbe(ScopedValueTenantContext()),
        )

        probes.forEachIndexed { index, probe ->
            val collected = awaitCollection(probe, RETENTION_TIMEOUT)
            if (!collected) {
                writeRetentionDiagnostics("probe-$index")
            }
            collected.shouldBeTrue()
        }
    }

    private fun verifyPlatformThreadIsolation() {
        val context = ThreadLocalTenantContext()
        val executor = Executors.newFixedThreadPool(PLATFORM_WORKERS)
        val start = CountDownLatch(1)

        try {
            val expected = List(TENANT_COUNT) { TenantId("platform-$it") }
            val futures = expected.mapIndexed { index, tenantId ->
                executor.submit(Callable {
                    check(start.await(5, TimeUnit.SECONDS)) { "Platform thread start gate timed out" }
                    if (index % EXCEPTION_SAMPLE_INTERVAL == 0) {
                        verifyExceptionalCleanup(context, tenantId)
                    }
                    val observed = context.withTenant(tenantId) {
                        Thread.yield()
                        context.requireCurrent()
                    }
                    context.currentOrNull().shouldBeNull()
                    observed
                })
            }
            start.countDown()

            collect(futures, Duration.ofSeconds(20)) shouldBeEqualTo expected
        } finally {
            shutdown(executor)
        }
        context.currentOrNull().shouldBeNull()
    }

    private fun verifyVirtualThreadIsolation() {
        val context = ScopedValueTenantContext()
        val executor = Executors.newVirtualThreadPerTaskExecutor()
        val start = CountDownLatch(1)

        try {
            val expected = List(VIRTUAL_TASKS) { TenantId("virtual-$it") }
            val futures = expected.mapIndexed { index, tenantId ->
                executor.submit(Callable {
                    check(start.await(10, TimeUnit.SECONDS)) { "Virtual thread start gate timed out" }
                    if (index % EXCEPTION_SAMPLE_INTERVAL == 0) {
                        verifyExceptionalCleanup(context, tenantId)
                    }
                    val observed = context.withTenant(tenantId) {
                        Thread.yield()
                        context.requireCurrent()
                    }
                    context.currentOrNull().shouldBeNull()
                    observed
                })
            }
            start.countDown()

            collect(futures, Duration.ofSeconds(30)) shouldBeEqualTo expected
        } finally {
            shutdown(executor)
        }
        context.currentOrNull().shouldBeNull()
    }

    private fun createRetentionProbe(context: TenantContext): RetentionProbe {
        val queue = ReferenceQueue<String>()
        val sentinel = String("tenant-retention-${System.nanoTime()}".toCharArray())
        val reference = WeakReference(sentinel, queue)

        context.withTenant(TenantId(sentinel)) {
            context.requireCurrent().value shouldBeEqualTo sentinel
        }
        context.currentOrNull().shouldBeNull()

        return RetentionProbe(reference, queue)
    }

    private fun awaitCollection(probe: RetentionProbe, timeout: Duration): Boolean {
        val deadline = System.nanoTime() + timeout.toNanos()
        val pressure = ArrayDeque<ByteArray>()
        while (System.nanoTime() < deadline) {
            if (probe.queue.poll() === probe.reference || probe.reference.get() == null) {
                return true
            }
            System.gc()
            pressure.addLast(ByteArray(GC_PRESSURE_BYTES))
            if (pressure.size > RETAINED_PRESSURE_BLOCKS) {
                pressure.removeFirst()
            }
            Thread.sleep(GC_POLL_MILLIS)
        }
        return probe.queue.poll() === probe.reference || probe.reference.get() == null
    }

    private fun verifyExceptionalCleanup(context: TenantContext, tenantId: TenantId) {
        val failure = runCatching<Unit> {
            context.withTenant(tenantId) {
                throw ExpectedTenantFailure()
            }
        }
        check(failure.exceptionOrNull() is ExpectedTenantFailure) {
            "TenantContext did not propagate the expected failure"
        }
        context.currentOrNull().shouldBeNull()
    }

    private fun <T> collect(futures: List<Future<T>>, timeout: Duration): List<T> {
        val deadline = System.nanoTime() + timeout.toNanos()
        return futures.map { future ->
            val remaining = deadline - System.nanoTime()
            check(remaining > 0) { "TenantContext stress timed out" }
            future.get(remaining, TimeUnit.NANOSECONDS)
        }
    }

    private fun shutdown(executor: ExecutorService) {
        executor.shutdownNow()
        check(executor.awaitTermination(5, TimeUnit.SECONDS)) {
            "TenantContext stress executor did not terminate"
        }
    }

    private fun writeRetentionDiagnostics(probeName: String) {
        val reportDir = Path.of(
            System.getProperty("tenant.retention.reportDir", "build/reports/tenant-retention")
        )
        Files.createDirectories(reportDir)
        Files.writeString(
            reportDir.resolve("retention-failure-$probeName.txt"),
            "TenantContext retention probe was not collected within ${RETENTION_TIMEOUT.seconds} seconds.\n",
        )

        val jcmd = Path.of(System.getProperty("java.home"), "bin", "jcmd")
        if (Files.isExecutable(jcmd)) {
            val histogram = reportDir.resolve("class-histogram-$probeName.txt").toFile()
            val process = ProcessBuilder(
                jcmd.toString(),
                ProcessHandle.current().pid().toString(),
                "GC.class_histogram",
            ).redirectErrorStream(true).redirectOutput(histogram).start()
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly()
            }
        }
    }

    private data class RetentionProbe(
        val reference: WeakReference<String>,
        val queue: ReferenceQueue<String>,
    )

    private class ExpectedTenantFailure: RuntimeException()

    companion object {
        private const val PLATFORM_WORKERS = 8
        private const val TENANT_COUNT = 100
        private const val VIRTUAL_TASKS = 10_000
        private const val EXCEPTION_SAMPLE_INTERVAL = 10
        private const val GC_PRESSURE_BYTES = 4 * 1024 * 1024
        private const val RETAINED_PRESSURE_BLOCKS = 16
        private const val GC_POLL_MILLIS = 50L
        private val RETENTION_TIMEOUT: Duration = Duration.ofSeconds(30)
    }
}
