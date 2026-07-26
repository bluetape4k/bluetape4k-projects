package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRuntime
import io.bluetape4k.redis.lettuce.lock.internal.DefaultLockCommandExecutor
import io.bluetape4k.redis.lettuce.lock.internal.DistributedLockClient
import io.bluetape4k.redis.lettuce.lock.internal.DistributedLockKeys
import io.bluetape4k.redis.lettuce.lock.internal.DistributedLockOperation
import io.bluetape4k.redis.lettuce.lock.internal.LockCommandExecutor
import io.bluetape4k.redis.lettuce.lock.internal.deriveDistributedLockKeys
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class LockCancellationTest: AbstractLettuceTest() {

    @Test
    fun `future cancellation before dispatch removes the scheduled attempt`() {
        val harness = TestLockHarness()
        val lock = LettuceDistributedLock(harness.client)
        val result = lock.acquireAsync(
            LockOwnerId.from("before-owner"),
            LockRequestId.from("before-request"),
            Duration.ofSeconds(1),
            LeasePolicy.Fixed(Duration.ofSeconds(3)),
        )

        result.cancel(false).shouldBeTrue()
        harness.runtime.drainDue()

        result.isCancelled.shouldBeTrue()
        harness.executor.calls.count { it == DistributedLockOperation.ACQUIRE } shouldBeEqualTo 0
        harness.runtime.activeTasks shouldBeEqualTo 0
        lock.close()
    }

    @Test
    fun `future cancellation during wait removes retry registration`() {
        val harness = TestLockHarness()
        harness.executor.acquireContended = true
        val lock = LettuceDistributedLock(harness.client)
        val result = lock.acquireAsync(
            LockOwnerId.from("wait-owner"),
            LockRequestId.from("wait-request"),
            Duration.ofSeconds(1),
            LeasePolicy.Fixed(Duration.ofSeconds(3)),
        )
        harness.runtime.drainDue()

        harness.runtime.activeTasks shouldBeEqualTo 1
        result.cancel(false).shouldBeTrue()

        result.isCancelled.shouldBeTrue()
        harness.runtime.activeTasks shouldBeEqualTo 0
        lock.close()
    }

    @Test
    fun `post-dispatch future cancellation propagates best effort and same request reconciles`() {
        val harness = TestLockHarness()
        val dispatched = NonCancellableFuture<List<String>>()
        harness.executor.asyncAcquire = { dispatched }
        val lock = LettuceDistributedLock(harness.client)
        val owner = LockOwnerId.from("ambiguous-owner")
        val request = LockRequestId.from("ambiguous-request")
        val result = lock.acquireAsync(
            owner,
            request,
            Duration.ofSeconds(1),
            LeasePolicy.Fixed(Duration.ofSeconds(3)),
        )
        harness.runtime.drainDue()

        result.cancel(false).shouldBeTrue()

        result.isCancelled.shouldBeTrue()
        dispatched.cancellationCalls.get() shouldBeEqualTo 1
        lock.reconcile(owner, request)
            .shouldBeInstanceOf<LockReconcileResult.Owned<LockHandle>>()
            .handle.requestId shouldBeEqualTo request
        lock.close()
    }

    @Test
    fun `post-dispatch cancellation reconciles the authoritative Redis mutation`() {
        val connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        val name = "cancel-${randomName().substringAfter(':')}"
        val keys = deriveDistributedLockKeys(name, LockConfig(), StringCodec.UTF8)
        val commands = connection.sync()
        commands.del(*keys.all)
        val runtime = CoordinationRuntime()
        val executor = AppliedButPendingExecutor(
            DefaultLockCommandExecutor(connection.sync(), connection.async()),
        )
        val distributedClient = DistributedLockClient(
            keys = keys,
            config = LockConfig(),
            executor = executor,
            registration = runtime.registerObject(keys.fingerprint),
            observationSink = LockObservationSink.NOOP,
        )
        val lock = LettuceDistributedLock(distributedClient)
        val owner = LockOwnerId.from("redis-ambiguous-owner")
        val request = LockRequestId.from("redis-ambiguous-request")

        try {
            val pending = lock.tryAcquireAsync(
                owner,
                request,
                LeasePolicy.Fixed(Duration.ofSeconds(30)),
            )
            executor.applied.get(5, TimeUnit.SECONDS)
            pending.cancel(false).shouldBeTrue()

            val reconciled = lock.reconcile(owner, request)
                .shouldBeInstanceOf<LockReconcileResult.Owned<LockHandle>>()
            reconciled.handle.ownerId shouldBeEqualTo owner
            reconciled.handle.requestId shouldBeEqualTo request
            commands.exists(keys.state, keys.holds) shouldBeEqualTo 2L
            lock.release(reconciled.handle) shouldBeEqualTo LockMutationResult.Released(0)
        } finally {
            lock.close()
            commands.del(*keys.all)
            connection.close()
        }
    }

    @Test
    fun `suspend cancellation preserves the original identity and request for reconciliation`() = runTest {
        val harness = TestLockHarness()
        harness.executor.suspendingAcquire = {
            suspendCancellableCoroutine { }
        }
        val lock = LettuceSuspendDistributedLock(harness.client)
        val owner = LockOwnerId.from("suspend-owner")
        val request = LockRequestId.from("suspend-request")
        val observed = CompletableDeferred<Throwable>()
        val pending = async {
            try {
                lock.acquire(
                    owner,
                    request,
                    Duration.ofSeconds(1),
                    LeasePolicy.Fixed(Duration.ofSeconds(3)),
                )
            } catch (error: Throwable) {
                observed.complete(error)
                throw error
            }
        }
        runCurrent()
        val cancellation = CancellationException("cancel distributed lock acquisition")

        pending.cancel(cancellation)
        assertFailsWith<CancellationException> {
            pending.await()
        }

        observed.await().shouldBeInstanceOf<CancellationException>()
        LettuceDistributedLock(harness.client).reconcile(owner, request)
            .shouldBeInstanceOf<LockReconcileResult.Owned<LockHandle>>()
            .handle.requestId shouldBeEqualTo request

        val exactCancellation = CancellationException("upstream cancellation")
        harness.executor.suspendingAcquire = { throw exactCancellation }
        val exactThrown = assertFailsWith<CancellationException> {
            lock.tryAcquire(
                owner,
                LockRequestId.from("exact-cancellation"),
                LeasePolicy.Fixed(Duration.ofSeconds(3)),
            )
        }
        exactThrown.shouldBeSameInstanceAs(exactCancellation)
        lock.close()
    }
}

private class AppliedButPendingExecutor(
    private val delegate: LockCommandExecutor,
): LockCommandExecutor {
    val applied = CompletableFuture<Unit>()

    override fun run(
        operation: DistributedLockOperation,
        keys: DistributedLockKeys,
        args: List<String>,
    ): List<String> =
        delegate.run(operation, keys, args)

    override fun runAsync(
        operation: DistributedLockOperation,
        keys: DistributedLockKeys,
        args: List<String>,
    ): CompletableFuture<List<String>> {
        if (operation != DistributedLockOperation.ACQUIRE) {
            return delegate.runAsync(operation, keys, args)
        }
        val pending = NonCancellableFuture<List<String>>()
        delegate.runAsync(operation, keys, args).whenComplete { _, error ->
            if (error == null) {
                applied.complete(Unit)
            } else {
                applied.completeExceptionally(error)
            }
        }
        return pending
    }

    override suspend fun runSuspending(
        operation: DistributedLockOperation,
        keys: DistributedLockKeys,
        args: List<String>,
    ): List<String> =
        delegate.runSuspending(operation, keys, args)
}
