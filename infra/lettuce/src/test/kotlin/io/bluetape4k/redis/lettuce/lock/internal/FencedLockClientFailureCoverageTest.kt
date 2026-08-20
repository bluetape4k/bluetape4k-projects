package io.bluetape4k.redis.lettuce.lock.internal

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRuntime
import io.bluetape4k.redis.lettuce.lock.FencedBootstrapResult
import io.bluetape4k.redis.lettuce.lock.FencedLockConfig
import io.bluetape4k.redis.lettuce.lock.FencedLockHandle
import io.bluetape4k.redis.lettuce.lock.LeasePolicy
import io.bluetape4k.redis.lettuce.lock.LockAcquireResult
import io.bluetape4k.redis.lettuce.lock.LockBackendFailure
import io.bluetape4k.redis.lettuce.lock.LockBackendFailureKind
import io.bluetape4k.redis.lettuce.lock.LockGeneration
import io.bluetape4k.redis.lettuce.lock.LockHandle
import io.bluetape4k.redis.lettuce.lock.LockInspectResult
import io.bluetape4k.redis.lettuce.lock.LockIntegrityFailure
import io.bluetape4k.redis.lettuce.lock.LockIntegrityFailureKind
import io.bluetape4k.redis.lettuce.lock.LockKind
import io.bluetape4k.redis.lettuce.lock.LockMutationResult
import io.bluetape4k.redis.lettuce.lock.LockObservationSink
import io.bluetape4k.redis.lettuce.lock.LockOwnerId
import io.bluetape4k.redis.lettuce.lock.LockReconcileResult
import io.bluetape4k.redis.lettuce.lock.LockRecoveryAction
import io.bluetape4k.redis.lettuce.lock.LockRequestId
import io.lettuce.core.RedisConnectionException
import kotlinx.coroutines.future.await
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CompletableFuture

/** Fenced lock의 세 실행 모델이 backend 및 integrity 실패를 동일하게 분류하는지 검증합니다. */
internal class FencedLockClientFailureCoverageTest {

    @Test
    fun `sync async suspend surfaces classify connection failures`() = runSuspendIO {
        val client = failingClient(RedisConnectionException("connection sentinel"))
        try {
            verifyConnectionBootstrap(client)
            verifyConnectionAcquire(client)
            verifyConnectionOperations(client)
        } finally {
            client.close()
        }
    }

    private suspend fun verifyConnectionBootstrap(client: FencedLockClient) {
        listOf(
            client.bootstrapFencing().shouldBeInstanceOf<FencedBootstrapResult.BackendFailure>().failure,
            client.bootstrapFencingAsync().await()
                .shouldBeInstanceOf<FencedBootstrapResult.BackendFailure>().failure,
            client.bootstrapFencingSuspending().shouldBeInstanceOf<FencedBootstrapResult.BackendFailure>().failure,
        ).forEach { expectBackend(it, LockRecoveryAction.INSPECT_HANDLE) }
    }

    private suspend fun verifyConnectionAcquire(client: FencedLockClient) {
        listOf(
            client.tryAcquire(OWNER, REQUEST, LEASE).shouldBeInstanceOf<LockAcquireResult.Ambiguous>(),
            client.tryAcquireAsync(OWNER, REQUEST, LEASE).await()
                .shouldBeInstanceOf<LockAcquireResult.Ambiguous>(),
            client.tryAcquireSuspending(OWNER, REQUEST, LEASE).shouldBeInstanceOf<LockAcquireResult.Ambiguous>(),
            client.acquire(OWNER, REQUEST, WAIT, LEASE).shouldBeInstanceOf<LockAcquireResult.Ambiguous>(),
            client.acquireAsync(OWNER, REQUEST, WAIT, LEASE).await()
                .shouldBeInstanceOf<LockAcquireResult.Ambiguous>(),
            client.acquireSuspending(OWNER, REQUEST, WAIT, LEASE).shouldBeInstanceOf<LockAcquireResult.Ambiguous>(),
        ).forEach(::expectAmbiguous)
    }

    private suspend fun verifyConnectionOperations(client: FencedLockClient) {
        listOf(
            client.inspect(HANDLE).shouldBeInstanceOf<LockInspectResult.BackendFailure>().failure to
                LockRecoveryAction.INSPECT_HANDLE,
            client.inspectAsync(HANDLE).await().shouldBeInstanceOf<LockInspectResult.BackendFailure>().failure to
                LockRecoveryAction.INSPECT_HANDLE,
            client.inspectSuspending(HANDLE).shouldBeInstanceOf<LockInspectResult.BackendFailure>().failure to
                LockRecoveryAction.INSPECT_HANDLE,
            client.reconcile(OWNER, REQUEST).shouldBeInstanceOf<LockReconcileResult.BackendFailure>().failure to
                LockRecoveryAction.RECONCILE_REQUEST,
            client.reconcileAsync(OWNER, REQUEST).await()
                .shouldBeInstanceOf<LockReconcileResult.BackendFailure>().failure to
                LockRecoveryAction.RECONCILE_REQUEST,
            client.reconcileSuspending(OWNER, REQUEST)
                .shouldBeInstanceOf<LockReconcileResult.BackendFailure>().failure to
                LockRecoveryAction.RECONCILE_REQUEST,
            client.renew(HANDLE, EXTENSION).shouldBeInstanceOf<LockMutationResult.BackendFailure>().failure to
                LockRecoveryAction.RETRY_SAME_HANDLE,
            client.renewAsync(HANDLE, EXTENSION).await()
                .shouldBeInstanceOf<LockMutationResult.BackendFailure>().failure to
                LockRecoveryAction.RETRY_SAME_HANDLE,
            client.renewSuspending(HANDLE, EXTENSION)
                .shouldBeInstanceOf<LockMutationResult.BackendFailure>().failure to
                LockRecoveryAction.RETRY_SAME_HANDLE,
            client.release(HANDLE).shouldBeInstanceOf<LockMutationResult.BackendFailure>().failure to
                LockRecoveryAction.RETRY_SAME_HANDLE,
            client.releaseAsync(HANDLE).await()
                .shouldBeInstanceOf<LockMutationResult.BackendFailure>().failure to
                LockRecoveryAction.RETRY_SAME_HANDLE,
            client.releaseSuspending(HANDLE)
                .shouldBeInstanceOf<LockMutationResult.BackendFailure>().failure to
                LockRecoveryAction.RETRY_SAME_HANDLE,
        ).forEach { (failure, action) -> expectBackend(failure, action) }
    }

    @Test
    fun `sync async suspend surfaces fail closed on malformed replies`() = runSuspendIO {
        val client = malformedClient()
        try {
            expectIntegrity(
                client.bootstrapFencing().shouldBeInstanceOf<FencedBootstrapResult.IntegrityFailure>().failure,
                LockIntegrityFailureKind.MALFORMED_REPLY,
            )
            expectIntegrity(
                client.bootstrapFencingAsync().await()
                    .shouldBeInstanceOf<FencedBootstrapResult.IntegrityFailure>().failure,
                LockIntegrityFailureKind.MALFORMED_REPLY,
            )
            expectIntegrity(
                client.bootstrapFencingSuspending()
                    .shouldBeInstanceOf<FencedBootstrapResult.IntegrityFailure>().failure,
                LockIntegrityFailureKind.MALFORMED_REPLY,
            )

            listOf(
                client.tryAcquire(OWNER, REQUEST, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.IntegrityFailure>().failure,
                client.tryAcquireAsync(OWNER, REQUEST, LEASE).await()
                    .shouldBeInstanceOf<LockAcquireResult.IntegrityFailure>().failure,
                client.tryAcquireSuspending(OWNER, REQUEST, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.IntegrityFailure>().failure,
                client.acquire(OWNER, REQUEST, WAIT, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.IntegrityFailure>().failure,
                client.acquireAsync(OWNER, REQUEST, WAIT, LEASE).await()
                    .shouldBeInstanceOf<LockAcquireResult.IntegrityFailure>().failure,
                client.acquireSuspending(OWNER, REQUEST, WAIT, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.IntegrityFailure>().failure,
            ).forEach { expectIntegrity(it, LockIntegrityFailureKind.MALFORMED_REPLY) }

            listOf(
                client.inspect(HANDLE).shouldBeInstanceOf<LockInspectResult.IntegrityFailure>().failure,
                client.inspectAsync(HANDLE).await().shouldBeInstanceOf<LockInspectResult.IntegrityFailure>().failure,
                client.inspectSuspending(HANDLE).shouldBeInstanceOf<LockInspectResult.IntegrityFailure>().failure,
                client.reconcile(OWNER, REQUEST).shouldBeInstanceOf<LockReconcileResult.IntegrityFailure>().failure,
                client.reconcileAsync(OWNER, REQUEST).await()
                    .shouldBeInstanceOf<LockReconcileResult.IntegrityFailure>().failure,
                client.reconcileSuspending(OWNER, REQUEST)
                    .shouldBeInstanceOf<LockReconcileResult.IntegrityFailure>().failure,
                client.renew(HANDLE, EXTENSION).shouldBeInstanceOf<LockMutationResult.IntegrityFailure>().failure,
                client.renewAsync(HANDLE, EXTENSION).await()
                    .shouldBeInstanceOf<LockMutationResult.IntegrityFailure>().failure,
                client.renewSuspending(HANDLE, EXTENSION)
                    .shouldBeInstanceOf<LockMutationResult.IntegrityFailure>().failure,
                client.release(HANDLE).shouldBeInstanceOf<LockMutationResult.IntegrityFailure>().failure,
                client.releaseAsync(HANDLE).await()
                    .shouldBeInstanceOf<LockMutationResult.IntegrityFailure>().failure,
                client.releaseSuspending(HANDLE)
                    .shouldBeInstanceOf<LockMutationResult.IntegrityFailure>().failure,
            ).forEach { expectIntegrity(it, LockIntegrityFailureKind.MALFORMED_REPLY) }
        } finally {
            client.close()
        }
    }

    @Test
    fun `terminal protocol replies retain their distinct public results`() {
        val executor = MutableResponseExecutor()
        val client = client(executor)
        try {
            verifyTerminalBootstrap(executor, client)
            verifyTerminalAcquire(executor, client)
            verifyTerminalInspect(executor, client)
            verifyTerminalReconcile(executor, client)
            verifyTerminalMutation(executor, client)
        } finally {
            client.close()
        }
    }

    private fun verifyTerminalBootstrap(executor: MutableResponseExecutor, client: FencedLockClient) {
        executor.response = listOf("COUNTER_REGRESSION")
        expectIntegrity(
            client.bootstrapFencing().shouldBeInstanceOf<FencedBootstrapResult.IntegrityFailure>().failure,
            LockIntegrityFailureKind.COUNTER_REGRESSION,
        )
        executor.response = listOf("INTEGRITY")
        expectIntegrity(
            client.bootstrapFencing().shouldBeInstanceOf<FencedBootstrapResult.IntegrityFailure>().failure,
            LockIntegrityFailureKind.INVALID_STATE,
        )
    }

    private fun verifyTerminalAcquire(executor: MutableResponseExecutor, client: FencedLockClient) {
        executor.response = listOf("REPLAY", "1", "2", "1000", "F:1000", CONFIG.epoch.toString(), "2")
        client.tryAcquire(OWNER, REQUEST, LEASE).shouldBeInstanceOf<LockAcquireResult.Reentered<FencedLockHandle>>()
        executor.response = listOf("CONTENDED", "20")
        client.tryAcquire(OWNER, REQUEST, LEASE).shouldBeEqualTo(LockAcquireResult.Contended(20))
        executor.response = listOf("CAPACITY")
        client.tryAcquire(OWNER, REQUEST, LEASE) shouldBeEqualTo LockAcquireResult.CapacityExceeded
        executor.response = listOf("COUNTER_REGRESSION")
        expectIntegrity(
            client.tryAcquire(OWNER, REQUEST, LEASE).shouldBeInstanceOf<LockAcquireResult.IntegrityFailure>().failure,
            LockIntegrityFailureKind.COUNTER_REGRESSION,
        )
        executor.response = listOf("INTEGRITY")
        expectIntegrity(
            client.tryAcquire(OWNER, REQUEST, LEASE).shouldBeInstanceOf<LockAcquireResult.IntegrityFailure>().failure,
            LockIntegrityFailureKind.INVALID_STATE,
        )
        executor.response = listOf("ACQUIRED", "0", "1", "1000", "F:1000", CONFIG.epoch.toString(), "2")
        expectIntegrity(
            client.tryAcquire(OWNER, REQUEST, LEASE).shouldBeInstanceOf<LockAcquireResult.IntegrityFailure>().failure,
            LockIntegrityFailureKind.MALFORMED_REPLY,
        )
    }

    private fun verifyTerminalInspect(executor: MutableResponseExecutor, client: FencedLockClient) {
        listOf(
            "RELEASED" to LockInspectResult.Released,
            "EXPIRED" to LockInspectResult.Expired,
            "STALE" to LockInspectResult.StaleGeneration,
            "LOST" to LockInspectResult.OwnershipLost,
        ).forEach { (tag, expected) ->
            executor.response = listOf(tag)
            client.inspect(HANDLE) shouldBeEqualTo expected
        }
        executor.response = listOf("COUNTER_REGRESSION")
        expectIntegrity(
            client.inspect(HANDLE).shouldBeInstanceOf<LockInspectResult.IntegrityFailure>().failure,
            LockIntegrityFailureKind.COUNTER_REGRESSION,
        )
        executor.response = listOf("INTEGRITY")
        expectIntegrity(
            client.inspect(HANDLE).shouldBeInstanceOf<LockInspectResult.IntegrityFailure>().failure,
            LockIntegrityFailureKind.INVALID_STATE,
        )
    }

    private fun verifyTerminalReconcile(executor: MutableResponseExecutor, client: FencedLockClient) {
        executor.response = listOf("RELEASED")
        client.reconcile(OWNER, REQUEST) shouldBeEqualTo LockReconcileResult.Released
        executor.response = listOf("NOT_FOUND")
        client.reconcile(OWNER, REQUEST) shouldBeEqualTo LockReconcileResult.NotFound
        executor.response = listOf("COUNTER_REGRESSION")
        expectIntegrity(
            client.reconcile(OWNER, REQUEST).shouldBeInstanceOf<LockReconcileResult.IntegrityFailure>().failure,
            LockIntegrityFailureKind.COUNTER_REGRESSION,
        )
        executor.response = listOf("INTEGRITY")
        expectIntegrity(
            client.reconcile(OWNER, REQUEST).shouldBeInstanceOf<LockReconcileResult.IntegrityFailure>().failure,
            LockIntegrityFailureKind.INVALID_STATE,
        )
    }

    private fun verifyTerminalMutation(executor: MutableResponseExecutor, client: FencedLockClient) {
        terminalMutationReplies().forEach { (tag, expected) ->
            executor.response = listOf(tag)
            client.renew(HANDLE, EXTENSION) shouldBeEqualTo expected
            client.release(HANDLE) shouldBeEqualTo expected
        }
        executor.response = listOf("COUNTER_REGRESSION")
        expectIntegrity(
            client.renew(HANDLE, EXTENSION).shouldBeInstanceOf<LockMutationResult.IntegrityFailure>().failure,
            LockIntegrityFailureKind.COUNTER_REGRESSION,
        )
        expectIntegrity(
            client.release(HANDLE).shouldBeInstanceOf<LockMutationResult.IntegrityFailure>().failure,
            LockIntegrityFailureKind.COUNTER_REGRESSION,
        )
        executor.response = listOf("INTEGRITY")
        expectIntegrity(
            client.renew(HANDLE, EXTENSION).shouldBeInstanceOf<LockMutationResult.IntegrityFailure>().failure,
            LockIntegrityFailureKind.INVALID_STATE,
        )
        expectIntegrity(
            client.release(HANDLE).shouldBeInstanceOf<LockMutationResult.IntegrityFailure>().failure,
            LockIntegrityFailureKind.INVALID_STATE,
        )
    }

    private fun expectBackend(failure: LockBackendFailure, action: LockRecoveryAction) {
        failure.kind shouldBeEqualTo LockBackendFailureKind.CONNECTION
        failure.recoveryAction shouldBeEqualTo action
    }

    private fun expectIntegrity(failure: LockIntegrityFailure, kind: LockIntegrityFailureKind) {
        failure.kind shouldBeEqualTo kind
    }

    private fun expectAmbiguous(result: LockAcquireResult.Ambiguous) {
        result.ownerId shouldBeEqualTo OWNER
        result.requestId shouldBeEqualTo REQUEST
        result.recoveryAction shouldBeEqualTo LockRecoveryAction.RECONCILE_REQUEST
    }

    private fun failingClient(failure: Throwable): FencedLockClient =
        client(FailureExecutor(failure = failure))

    private fun malformedClient(): FencedLockClient =
        client(FailureExecutor(response = listOf("UNKNOWN")))

    private fun client(executor: FencedLockCommandExecutor): FencedLockClient {
        val runtime = CoordinationRuntime()
        return FencedLockClient(
            keys = KEYS,
            config = CONFIG,
            executor = executor,
            registration = runtime.registerObject(KEYS.fingerprint),
            observationSink = LockObservationSink.NOOP,
        )
    }

    private class FailureExecutor(
        private val response: List<String>? = null,
        private val failure: Throwable? = null,
    ): FencedLockCommandExecutor {
        override fun run(
            operation: FencedLockOperation,
            keys: FencedLockKeys,
            args: List<String>,
        ): List<String> = failure?.let { throw it } ?: requireNotNull(response)

        override fun runAsync(
            operation: FencedLockOperation,
            keys: FencedLockKeys,
            args: List<String>,
        ): CompletableFuture<List<String>> =
            failure?.let(CompletableFuture<List<String>>::failedFuture)
                ?: CompletableFuture.completedFuture(requireNotNull(response))

        override suspend fun runSuspending(
            operation: FencedLockOperation,
            keys: FencedLockKeys,
            args: List<String>,
        ): List<String> = run(operation, keys, args)
    }

    private class MutableResponseExecutor: FencedLockCommandExecutor {
        var response: List<String> = listOf("UNKNOWN")

        override fun run(
            operation: FencedLockOperation,
            keys: FencedLockKeys,
            args: List<String>,
        ): List<String> = response

        override fun runAsync(
            operation: FencedLockOperation,
            keys: FencedLockKeys,
            args: List<String>,
        ): CompletableFuture<List<String>> = CompletableFuture.completedFuture(response)

        override suspend fun runSuspending(
            operation: FencedLockOperation,
            keys: FencedLockKeys,
            args: List<String>,
        ): List<String> = response
    }

    private fun terminalMutationReplies(): List<Pair<String, LockMutationResult<FencedLockHandle>>> =
        listOf(
            "ALREADY_RELEASED" to LockMutationResult.AlreadyReleased,
            "EXPIRED" to LockMutationResult.Expired,
            "STALE" to LockMutationResult.StaleGeneration,
            "LOST" to LockMutationResult.OwnershipLost,
        )

    private companion object {
        val CONFIG = FencedLockConfig(epoch = 81)
        val KEYS = FencedLockKeys(
            state = "state",
            generation = "generation",
            holds = "holds",
            terminal = "terminal",
            counter = "counter",
            fingerprint = "fingerprint",
        )
        val OWNER = LockOwnerId.from("failure-owner")
        val REQUEST = LockRequestId.from("failure-request")
        val LEASE: LeasePolicy = LeasePolicy.Fixed(Duration.ofSeconds(3))
        val WAIT: Duration = Duration.ofMillis(20)
        val EXTENSION: Duration = Duration.ofSeconds(1)
        val HANDLE = FencedLockHandle(
            lock = LockHandle(
                objectFingerprint = KEYS.fingerprint,
                ownerId = OWNER,
                generation = LockGeneration(1),
                requestId = REQUEST,
                leasePolicy = LEASE,
                kind = LockKind.FENCED,
            ),
            epoch = CONFIG.epoch,
            fencingToken = 1,
        )
    }
}
