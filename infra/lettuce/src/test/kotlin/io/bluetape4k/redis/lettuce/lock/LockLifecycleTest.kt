package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRuntime
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRuntimeLimits
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationScheduledHandle
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationScheduler
import io.bluetape4k.redis.lettuce.coordination.internal.MonotonicTicker
import io.bluetape4k.redis.lettuce.lock.internal.DistributedLockClient
import io.bluetape4k.redis.lettuce.lock.internal.DistributedLockKeys
import io.bluetape4k.redis.lettuce.lock.internal.DistributedLockOperation
import io.bluetape4k.redis.lettuce.lock.internal.LockCommandExecutor
import io.bluetape4k.redis.lettuce.lock.internal.deriveDistributedLockKeys
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.async
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration as KotlinDuration
import kotlin.time.Duration.Companion.seconds

class LockLifecycleTest {

    @Test
    fun `close rejects new work after validation and completes pending waits`() = runTest {
        val harness = TestLockHarness()
        harness.executor.acquireContended = true
        val blocking = LettuceDistributedLock(harness.client)
        val suspending = LettuceSuspendDistributedLock(harness.client)
        val owner = LockOwnerId.from("lifecycle-owner")
        val asyncRequest = LockRequestId.from("async-request")
        val suspendRequest = LockRequestId.from("suspend-request")
        val lease = LeasePolicy.Fixed(Duration.ofSeconds(3))

        val pendingFuture = blocking.acquireAsync(
            owner,
            asyncRequest,
            Duration.ofSeconds(1),
            lease,
        )
        harness.runtime.drainDue()
        val pendingCoroutine = async {
            suspending.acquire(
                owner,
                suspendRequest,
                Duration.ofSeconds(1),
                lease,
            )
        }
        runCurrent()

        blocking.close()

        pendingFuture.get() shouldBeEqualTo LockAcquireResult.Closed
        pendingCoroutine.await() shouldBeEqualTo LockAcquireResult.Closed
        blocking.tryAcquire(owner, LockRequestId.from("closed-blocking"), lease) shouldBeEqualTo
            LockAcquireResult.Closed
        blocking.tryAcquireAsync(owner, LockRequestId.from("closed-async"), lease).get() shouldBeEqualTo
            LockAcquireResult.Closed
        suspending.tryAcquire(owner, LockRequestId.from("closed-suspend"), lease) shouldBeEqualTo
            LockAcquireResult.Closed
        assertFailsWith<IllegalArgumentException> {
            blocking.acquire(owner, LockRequestId.from("invalid-after-close"), Duration.ZERO, lease)
        }
        harness.runtime.activeTasks shouldBeEqualTo 0
        harness.runtime.activeWatchdogs shouldBeEqualTo 0
    }

    @Test
    fun `runtime closes on the last object while caller scheduler survives`() {
        val scheduler = ManualCoordinationScheduler()
        val runtime = CoordinationRuntime(scheduler = scheduler)
        val first = runtime.registerObject("first-object")
        val second = runtime.registerObject("second-object")

        first.registerTask(1.seconds) {}
        second.registerTask(1.seconds) {}
        first.close()

        runtime.isClosed.shouldBeFalse()
        runtime.activeObjects shouldBeEqualTo 1
        runtime.activeTasks shouldBeEqualTo 1
        scheduler.isShutdown.shouldBeFalse()

        second.close()

        runtime.isClosed.shouldBeTrue()
        scheduler.isShutdown.shouldBeFalse()

        val ownedRuntime = CoordinationRuntime()
        ownedRuntime.registerObject("owned-object").close()
        ownedRuntime.schedulerShutdown.shouldBeTrue()
    }

    @Test
    fun `connection close completes pending future and ignores late completion`() {
        val harness = TestLockHarness()
        val dispatched = NonCancellableFuture<List<String>>()
        harness.executor.asyncAcquire = { dispatched }
        val lock = LettuceDistributedLock(harness.client)
        val pending = lock.acquireAsync(
            LockOwnerId.from("connection-owner"),
            LockRequestId.from("connection-request"),
            Duration.ofSeconds(1),
            LeasePolicy.Fixed(Duration.ofSeconds(3)),
        )
        harness.runtime.drainDue()

        harness.runtime.connectionClosed()
        dispatched.complete(harness.executor.acquiredReply())

        pending.get() shouldBeEqualTo LockAcquireResult.Closed
        harness.runtime.activeTasks shouldBeEqualTo 0
        harness.runtime.activeWatchdogs shouldBeEqualTo 0
        harness.scheduler.isShutdown.shouldBeFalse()
    }

    @Test
    fun `async and suspend waits report runtime capacity instead of closed`() = runTest {
        val asyncHarness = TestLockHarness(
            limits = CoordinationRuntimeLimits(maxRegistrations = 1),
        )
        asyncHarness.registration.registerTask(1.seconds) {}
        val asyncLock = LettuceDistributedLock(asyncHarness.client)
        asyncLock.acquireAsync(
            LockOwnerId.from("capacity-owner"),
            LockRequestId.from("capacity-async"),
            Duration.ofSeconds(1),
            LeasePolicy.Fixed(Duration.ofSeconds(3)),
        ).get() shouldBeEqualTo LockAcquireResult.CapacityExceeded
        asyncLock.close()

        val suspendHarness = TestLockHarness(
            limits = CoordinationRuntimeLimits(maxRegistrations = 1),
        )
        suspendHarness.executor.acquireContended = true
        suspendHarness.registration.registerTask(1.seconds) {}
        val suspendLock = LettuceSuspendDistributedLock(suspendHarness.client)
        suspendLock.acquire(
            LockOwnerId.from("capacity-owner"),
            LockRequestId.from("capacity-suspend"),
            Duration.ofSeconds(1),
            LeasePolicy.Fixed(Duration.ofSeconds(3)),
        ) shouldBeEqualTo LockAcquireResult.CapacityExceeded
        suspendLock.close()
    }
}

internal class TestLockHarness(
    val ticker: TestTicker = TestTicker(),
    val scheduler: ManualCoordinationScheduler = ManualCoordinationScheduler(),
    watchdogDelay: (KotlinDuration) -> KotlinDuration = { it },
    limits: CoordinationRuntimeLimits = CoordinationRuntimeLimits(),
    observationSink: LockObservationSink = LockObservationSink.NOOP,
) {
    val executor = TestLockCommandExecutor()
    val keys: DistributedLockKeys =
        deriveDistributedLockKeys("task4-lock", LockConfig(), StringCodec.UTF8)
    val runtime = CoordinationRuntime(
        ticker = ticker,
        scheduler = scheduler,
        limits = limits,
        watchdogDelay = watchdogDelay,
    )
    val registration = runtime.registerObject(keys.fingerprint)
    val client = DistributedLockClient(
        keys = keys,
        config = LockConfig(),
        executor = executor,
        registration = registration,
        observationSink = observationSink,
    )
}

internal class TestLockCommandExecutor: LockCommandExecutor {
    val calls = CopyOnWriteArrayList<DistributedLockOperation>()
    var acquireContended: Boolean = false
    var asyncAcquire: ((List<String>) -> CompletableFuture<List<String>>)? = null
    var suspendingAcquire: (suspend (List<String>) -> List<String>)? = null
    var asyncRenew: ((List<String>) -> CompletableFuture<List<String>>)? = null
    var releaseRemainingHoldCount: Int = 0
    private val leasePolicies = linkedMapOf<Pair<String, String>, String>()

    override fun run(
        operation: DistributedLockOperation,
        keys: DistributedLockKeys,
        args: List<String>,
    ): List<String> {
        calls += operation
        return response(operation, args)
    }

    override fun runAsync(
        operation: DistributedLockOperation,
        keys: DistributedLockKeys,
        args: List<String>,
    ): CompletableFuture<List<String>> {
        calls += operation
        return when (operation) {
            DistributedLockOperation.ACQUIRE ->
                asyncAcquire?.invoke(args) ?: CompletableFuture.completedFuture(response(operation, args))
            DistributedLockOperation.RENEW ->
                asyncRenew?.invoke(args) ?: CompletableFuture.completedFuture(response(operation, args))
            else -> CompletableFuture.completedFuture(response(operation, args))
        }
    }

    override suspend fun runSuspending(
        operation: DistributedLockOperation,
        keys: DistributedLockKeys,
        args: List<String>,
    ): List<String> {
        calls += operation
        return if (operation == DistributedLockOperation.ACQUIRE && suspendingAcquire != null) {
            suspendingAcquire!!.invoke(args)
        } else {
            when (operation) {
                DistributedLockOperation.ACQUIRE ->
                    asyncAcquire?.invoke(args)?.await() ?: response(operation, args)
                DistributedLockOperation.RENEW ->
                    asyncRenew?.invoke(args)?.await() ?: response(operation, args)
                else -> response(operation, args)
            }
        }
    }

    fun acquiredReply(policy: String = "F:3000"): List<String> =
        listOf("ACQUIRED", "1", "1", "3000", policy)

    private fun response(
        operation: DistributedLockOperation,
        args: List<String>,
    ): List<String> =
        when (operation) {
            DistributedLockOperation.ACQUIRE -> {
                if (acquireContended) {
                    listOf("CONTENDED", "1000")
                } else {
                    leasePolicies[args[0] to args[1]] = args[2]
                    acquiredReply(args[2])
                }
            }
            DistributedLockOperation.INSPECT ->
                listOf("OWNED", args[2], "1", "3000", "F:3000")
            DistributedLockOperation.RECONCILE -> {
                val policy = leasePolicies[args[0] to args[1]] ?: "F:3000"
                listOf("OWNED", "1", "1", "3000", policy)
            }
            DistributedLockOperation.RENEW -> listOf("RENEWED", args.last())
            DistributedLockOperation.RELEASE -> listOf("RELEASED", releaseRemainingHoldCount.toString())
        }
}

internal class TestTicker(
    private var nowNanos: Long = 0L,
): MonotonicTicker {
    override fun readNanos(): Long = nowNanos

    fun advance(duration: KotlinDuration) {
        nowNanos += duration.inWholeNanoseconds
    }
}

internal class ManualCoordinationScheduler: CoordinationScheduler {
    val scheduledDelays = CopyOnWriteArrayList<KotlinDuration>()
    private val shutdownCount = AtomicInteger()
    override val isShutdown: Boolean get() = shutdownCount.get() > 0

    override fun schedule(
        delay: KotlinDuration,
        task: () -> Unit,
    ): CoordinationScheduledHandle {
        scheduledDelays += delay
        return CoordinationScheduledHandle { true }
    }

    override fun shutdown() {
        shutdownCount.incrementAndGet()
    }
}

internal class NonCancellableFuture<T>: CompletableFuture<T>() {
    val cancellationCalls = AtomicInteger()

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        cancellationCalls.incrementAndGet()
        return false
    }
}
