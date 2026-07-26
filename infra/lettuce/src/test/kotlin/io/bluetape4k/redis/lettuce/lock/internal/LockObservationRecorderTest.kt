package io.bluetape4k.redis.lettuce.lock.internal

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRuntime
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationScheduledHandle
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationScheduler
import io.bluetape4k.redis.lettuce.lock.LeasePolicy
import io.bluetape4k.redis.lettuce.lock.FencedLockConfig
import io.bluetape4k.redis.lettuce.lock.LockCounterName
import io.bluetape4k.redis.lettuce.lock.LockGaugeName
import io.bluetape4k.redis.lettuce.lock.LockGeneration
import io.bluetape4k.redis.lettuce.lock.LockConfig
import io.bluetape4k.redis.lettuce.lock.LockAcquireResult
import io.bluetape4k.redis.lettuce.lock.LockHandle
import io.bluetape4k.redis.lettuce.lock.LockHistogramName
import io.bluetape4k.redis.lettuce.lock.LockKind
import io.bluetape4k.redis.lettuce.lock.LockObservation
import io.bluetape4k.redis.lettuce.lock.LockOperation
import io.bluetape4k.redis.lettuce.lock.LockOutcome
import io.bluetape4k.redis.lettuce.lock.LockOwnerId
import io.bluetape4k.redis.lettuce.lock.LockRequestId
import io.bluetape4k.redis.lettuce.script.RedisScript
import io.lettuce.core.RedisNoScriptException
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.sync.RedisScriptingCommands
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration as KotlinDuration

class LockObservationRecorderTest {

    @Test
    fun `coordination catalog is bridged to public lock observations`() {
        val observations = mutableListOf<LockObservation>()
        val recorder = LockObservationRecorder(
            objectKind = LockKind.DISTRIBUTED,
            sink = observations::add,
        )
        val observer = recorder.asCoordinationObserver()

        REQUIRED_COORDINATION_SIGNALS.forEach { signal ->
            observer.emit(signal, value = 2.0)
        }

        observations.filterIsInstance<LockObservation.Counter>().map { it.name }.toSet() shouldBeEqualTo
            setOf(
                LockCounterName.OPERATION_TOTAL,
                LockCounterName.RECONCILE_TOTAL,
                LockCounterName.STALE_CLEANUP_TOTAL,
                LockCounterName.CLEANUP_PENDING_TOTAL,
                LockCounterName.OWNERSHIP_LOSS_TOTAL,
                LockCounterName.WATCHDOG_LATE_TOTAL,
                LockCounterName.WATCHDOG_MISSED_TOTAL,
                LockCounterName.NOSCRIPT_FALLBACK_TOTAL,
                LockCounterName.INTEGRITY_FAILURE_TOTAL,
                LockCounterName.CAPACITY_REJECTION_TOTAL,
            )
        observations.filterIsInstance<LockObservation.Gauge>().map { it.name }.toSet() shouldBeEqualTo
            LockGaugeName.entries.toSet()
        observations.filterIsInstance<LockObservation.Histogram>().map { it.name }.toSet() shouldBeEqualTo
            setOf(
                LockHistogramName.REDIS_COMMAND_LATENCY_MILLIS,
                LockHistogramName.CALLER_WAIT_LATENCY_MILLIS,
                LockHistogramName.RETRY_COUNT,
                LockHistogramName.CLEANUP_BATCH_SIZE,
            )
        observations.filterIsInstance<LockObservation.Event>().map { it.event.outcome }.toSet() shouldBeEqualTo
            setOf(
                LockOutcome.SUCCEEDED,
                LockOutcome.CONTENDED,
                LockOutcome.OWNERSHIP_LOST,
                LockOutcome.INTEGRITY_FAILED,
                LockOutcome.CAPACITY_REJECTED,
            )
        observations.all { observation ->
            when (observation) {
                is LockObservation.Counter -> observation.dimensions.objectKind == LockKind.DISTRIBUTED
                is LockObservation.Gauge -> observation.dimensions.objectKind == LockKind.DISTRIBUTED
                is LockObservation.Histogram -> observation.dimensions.objectKind == LockKind.DISTRIBUTED
                is LockObservation.Event -> observation.event.objectKind == LockKind.DISTRIBUTED
            }
        } shouldBeEqualTo true
    }

    @Test
    fun `script runner records Redis latency and NOSCRIPT fallback without duplicating mutation`() {
        val commands = mockk<RedisScriptingCommands<String, String>>()
        val script = RedisScript("return ARGV[1]")
        val keys = arrayOf("bt4k:coord:v1:{inventory}:lock:inventory:state")
        val observations = mutableListOf<LockObservation>()
        val mutationCount = AtomicInteger()
        val recorder = LockObservationRecorder(
            objectKind = LockKind.DISTRIBUTED,
            sink = observations::add,
        )

        every {
            commands.evalsha<String>(script.sha1, ScriptOutputType.VALUE, keys, "value")
        } throws RedisNoScriptException("NOSCRIPT")
        every {
            commands.eval<String>(script.source, ScriptOutputType.VALUE, keys, "value")
        } answers {
            mutationCount.incrementAndGet()
            "value"
        }

        recorder.runScript<String>(
            commands = commands,
            script = script,
            outputType = ScriptOutputType.VALUE,
            keys = keys,
            operation = LockOperation.ACQUIRE,
            args = arrayOf("value"),
        ) shouldBeEqualTo "value"

        mutationCount.get() shouldBeEqualTo 1
        observations.filterIsInstance<LockObservation.Counter>()
            .single { it.name == LockCounterName.NOSCRIPT_FALLBACK_TOTAL }
            .dimensions.operation shouldBeEqualTo LockOperation.ACQUIRE
        observations.filterIsInstance<LockObservation.Histogram>()
            .single { it.name == LockHistogramName.REDIS_COMMAND_LATENCY_MILLIS }
            .value.shouldBeInstanceOf<Double>()
        verify(exactly = 1) {
            commands.evalsha<String>(script.sha1, ScriptOutputType.VALUE, keys, "value")
            commands.eval<String>(script.source, ScriptOutputType.VALUE, keys, "value")
        }
        confirmVerified(commands)
    }

    @Test
    fun `same connection lock registrations keep observations isolated by sink`() {
        val firstObservations = mutableListOf<LockObservation>()
        val secondObservations = mutableListOf<LockObservation>()
        val connection = Any()
        val runtime = CoordinationRuntime.forConnection(connection)
        val firstRecorder = LockObservationRecorder(LockKind.DISTRIBUTED, firstObservations::add)
        val secondRecorder = LockObservationRecorder(LockKind.DISTRIBUTED, secondObservations::add)

        val first = runtime.registerObject("lock-a", firstRecorder.asCoordinationObserver())
        firstObservations.clear()
        val second = CoordinationRuntime.forConnection(connection)
            .registerObject("lock-b", secondRecorder.asCoordinationObserver())

        firstObservations.size shouldBeEqualTo 0
        secondObservations.any { it is LockObservation.Gauge && it.name == LockGaugeName.COORDINATION_OBJECTS }
            .shouldBeEqualTo(true)
        first.close()
        second.close()
    }

    @Test
    fun `invalid wait time does not leak active request gauges`() = runSuspendIO {
        val observations = mutableListOf<LockObservation>()
        val recorder = LockObservationRecorder(LockKind.SPIN, observations::add)
        val runtime = CoordinationRuntime()
        val registration = runtime.registerObject("wait-validation")
        val support = LockWaitSupport(
            registration = registration,
            isClosed = { false },
            waitObservation = LockWaitObservation(recorder),
        )
        val handle = LockHandle(
            objectFingerprint = "fingerprint",
            ownerId = LockOwnerId.from("owner"),
            generation = LockGeneration(1),
            requestId = LockRequestId.from("request"),
            leasePolicy = LeasePolicy.Fixed(Duration.ofSeconds(1)),
            kind = LockKind.SPIN,
        )

        try {
            assertFailsWith<IllegalArgumentException> {
                support.acquire(Duration.ZERO) { LockAcquireResult.Acquired(handle) }
            }
            assertFailsWith<IllegalArgumentException> {
                support.acquireSuspending(Duration.ZERO) { LockAcquireResult.Acquired(handle) }
            }

            support.acquire(Duration.ofMillis(1)) {
                LockAcquireResult.Acquired(handle)
            } shouldBeEqualTo LockAcquireResult.Acquired(handle)
            support.acquireSuspending(Duration.ofMillis(1)) {
                LockAcquireResult.Acquired(handle)
            } shouldBeEqualTo LockAcquireResult.Acquired(handle)

            observations.filterIsInstance<LockObservation.Gauge>()
                .filter { it.name == LockGaugeName.ACTIVE_REQUEST_HOLDS }
                .map { it.value } shouldBeEqualTo listOf(1L, 0L, 1L, 0L)
        } finally {
            support.close()
            registration.close()
        }
    }

    @Test
    fun `late non cancellable acquire after close dispatches raw release`() {
        val scheduler = ManualScheduler()
        val runtime = CoordinationRuntime(scheduler = scheduler)
        val executor = RecordingLockCommandExecutor()
        val client = DistributedLockClient(
            keys = DistributedLockKeys(
                state = "state",
                generation = "generation",
                holds = "holds",
                terminal = "terminal",
                fingerprint = "fingerprint",
            ),
            config = LockConfig(),
            executor = executor,
            registration = runtime.registerObject("fingerprint"),
            observationSink = {},
        )

        val result = client.acquireAsync(
            LockOwnerId.from("owner"),
            LockRequestId.from("request"),
            Duration.ofSeconds(1),
            LeasePolicy.Fixed(Duration.ofSeconds(1)),
        )
        scheduler.runNext()

        client.close()
        result.join() shouldBeEqualTo LockAcquireResult.Closed
        executor.acquire.complete(listOf("ACQUIRED", "1", "1", "1000", "F:1000"))

        executor.releaseDispatches.get() shouldBeEqualTo 1
    }

    @Test
    fun `late non cancellable fenced acquire after close dispatches raw release`() {
        val scheduler = ManualScheduler()
        val runtime = CoordinationRuntime(scheduler = scheduler)
        val executor = RecordingFencedLockCommandExecutor()
        val client = FencedLockClient(
            keys = FencedLockKeys(
                state = "state",
                generation = "generation",
                holds = "holds",
                terminal = "terminal",
                counter = "counter",
                fingerprint = "fingerprint",
            ),
            config = FencedLockConfig(epoch = 1L),
            executor = executor,
            registration = runtime.registerObject("fingerprint"),
            observationSink = {},
        )

        val result = client.acquireAsync(
            LockOwnerId.from("owner"),
            LockRequestId.from("request"),
            Duration.ofSeconds(1),
            LeasePolicy.Fixed(Duration.ofSeconds(1)),
        )
        scheduler.runNext()

        client.close()
        result.join() shouldBeEqualTo LockAcquireResult.Closed
        executor.acquire.complete(listOf("ACQUIRED", "1", "1", "1000", "F:1000", "1", "10"))

        executor.releaseDispatches.get() shouldBeEqualTo 1
    }

    @Test
    fun `cancelled public read write mapping releases a late acquired handle`() {
        val source = object: CompletableFuture<LockAcquireResult<LockHandle>>() {
            override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false
        }
        val releases = AtomicInteger()
        val handle = LockHandle(
            objectFingerprint = "fingerprint",
            ownerId = LockOwnerId.from("owner"),
            generation = LockGeneration(1),
            requestId = LockRequestId.from("request"),
            leasePolicy = LeasePolicy.Fixed(Duration.ofSeconds(1)),
            kind = LockKind.READ,
        )
        val mapped = mapHandleResultAsync(
            source = source,
            acquiredHandle = {
                when (it) {
                    is LockAcquireResult.Acquired -> it.handle
                    is LockAcquireResult.Reentered -> it.handle
                    else -> null
                }
            },
            transform = { it },
            releaseAbandoned = { releases.incrementAndGet() },
        )

        mapped.cancel(false)
        source.complete(LockAcquireResult.Acquired(handle))

        releases.get() shouldBeEqualTo 1
    }

    private class RecordingLockCommandExecutor: LockCommandExecutor {
        val acquire = object: CompletableFuture<List<String>>() {
            override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false
        }
        val releaseDispatches = AtomicInteger()

        override fun run(
            operation: DistributedLockOperation,
            keys: DistributedLockKeys,
            args: List<String>,
        ): List<String> = error("Synchronous commands are not used by this test.")

        override fun runAsync(
            operation: DistributedLockOperation,
            keys: DistributedLockKeys,
            args: List<String>,
        ): CompletableFuture<List<String>> =
            when (operation) {
                DistributedLockOperation.ACQUIRE -> acquire
                DistributedLockOperation.RELEASE -> {
                    releaseDispatches.incrementAndGet()
                    CompletableFuture.completedFuture(listOf("RELEASED", "0"))
                }
                else -> error("Unexpected operation: $operation")
            }

        override suspend fun runSuspending(
            operation: DistributedLockOperation,
            keys: DistributedLockKeys,
            args: List<String>,
        ): List<String> = error("Suspending commands are not used by this test.")
    }

    private class ManualScheduler: CoordinationScheduler {
        private val scheduled = CopyOnWriteArrayList<() -> Unit>()
        override val isShutdown: Boolean = false

        override fun schedule(delay: KotlinDuration, task: () -> Unit): CoordinationScheduledHandle {
            scheduled += task
            return CoordinationScheduledHandle { scheduled.remove(task) }
        }

        fun runNext() {
            scheduled.removeFirst().invoke()
        }

        override fun shutdown() = Unit
    }

    private class RecordingFencedLockCommandExecutor: FencedLockCommandExecutor {
        val acquire = object: CompletableFuture<List<String>>() {
            override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false
        }
        val releaseDispatches = AtomicInteger()

        override fun run(
            operation: FencedLockOperation,
            keys: FencedLockKeys,
            args: List<String>,
        ): List<String> = error("Synchronous commands are not used by this test.")

        override fun runAsync(
            operation: FencedLockOperation,
            keys: FencedLockKeys,
            args: List<String>,
        ): CompletableFuture<List<String>> =
            when (operation) {
                FencedLockOperation.ACQUIRE -> acquire
                FencedLockOperation.RELEASE -> {
                    releaseDispatches.incrementAndGet()
                    CompletableFuture.completedFuture(listOf("RELEASED", "0"))
                }
                else -> error("Unexpected operation: $operation")
            }

        override suspend fun runSuspending(
            operation: FencedLockOperation,
            keys: FencedLockKeys,
            args: List<String>,
        ): List<String> = error("Suspending commands are not used by this test.")
    }
}
