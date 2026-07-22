package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeLessOrEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.lettuce.core.RedisConnectionException
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

internal class LettuceFencingLeaseCancellationTest : AbstractLettuceTest() {

    @Test
    fun `backend failure before and after apply has operation specific reconciliation`() {
        mutationOperations.forEach { operation ->
            FaultPhase.entries.forEach { phase ->
                withFixture(operation, phase) { fixture ->
                    val context = prepare(operation, fixture.healthy)
                    val executor = OneShotFaultExecutor(fixture.defaultExecutor, operation, phase)
                    val lease = LettuceFencingLease.createForTesting(executor, StringCodec.UTF8, fixture.config)

                    assertBackendFailure(operation, invokeSync(operation, lease, context))
                    reconcile(operation, phase, fixture.healthy, context)
                }
            }
        }
    }

    @Test
    fun `future cancellation preserves cancelled state before and after every mutation`() {
        mutationOperations.forEach { operation ->
            FaultPhase.entries.forEach { phase ->
                withFixture(operation, phase) { fixture ->
                    val context = prepare(operation, fixture.healthy)
                    val executor = ControlledCancellationExecutor(fixture.defaultExecutor, operation, phase)
                    val lease = LettuceFencingLease.createForTesting(executor, StringCodec.UTF8, fixture.config)

                    val result = invokeFuture(operation, lease, context)
                    result.cancel(true).shouldBeTrue()
                    result.isCancelled.shouldBeTrue()
                    executor.pending.isCancelled.shouldBeTrue()
                    reconcile(operation, phase, fixture.healthy, context)
                }
            }
        }
    }

    @Test
    fun `coroutine cancellation remains cancellation before and after every mutation`() = runSuspendIO {
        mutationOperations.forEach { operation ->
            FaultPhase.entries.forEach { phase ->
                withFixture(operation, phase) { fixture ->
                    val context = prepare(operation, fixture.healthy)
                    val executor = ControlledCancellationExecutor(fixture.defaultExecutor, operation, phase)
                    val lease = LettuceSuspendFencingLease.createForTesting(executor, StringCodec.UTF8, fixture.config)
                    val observed = CompletableDeferred<Throwable>()

                    val job = launch {
                        try {
                            invokeSuspending(operation, lease, context)
                        } catch (failure: Throwable) {
                            observed.complete(failure)
                            throw failure
                        }
                    }
                    executor.started.await()
                    job.cancelAndJoin()

                    observed.await().shouldBeInstanceOf<CancellationException>()
                    reconcile(operation, phase, fixture.healthy, context)
                }
            }
        }
    }

    @Test
    fun `release ambiguity does not authorize retry for unsafe inspection results`() {
        withFixture(FencingLeaseOperation.RELEASE, FaultPhase.BEFORE_APPLY) { fixture ->
            fixture.healthy.bootstrap()
            val original = fixture.healthy.acquire(OWNER, INITIAL_LEASE)
                .shouldBeInstanceOf<FencingAcquireResult.Acquired>().token

            fixture.healthy.release(OWNER, original) shouldBeEqualTo FencingReleaseResult.Released
            val contenderToken = fixture.healthy.acquire(CONTENDER, INITIAL_LEASE)
                .shouldBeInstanceOf<FencingAcquireResult.Acquired>().token
            fixture.healthy.inspect(OWNER).shouldBeInstanceOf<FencingInspectResult.Contended>()
            fixture.healthy.release(OWNER, original) shouldBeEqualTo FencingReleaseResult.OwnershipMismatch
            fixture.healthy.inspect(CONTENDER)
                .shouldBeInstanceOf<FencingInspectResult.Owned>().token shouldBeEqualTo contenderToken

            fixture.healthy.release(CONTENDER, contenderToken) shouldBeEqualTo FencingReleaseResult.Released
            val newer = fixture.healthy.acquire(OWNER, INITIAL_LEASE)
                .shouldBeInstanceOf<FencingAcquireResult.Acquired>().token
            newer shouldBeGreaterThan original
            fixture.healthy.inspect(OWNER)
                .shouldBeInstanceOf<FencingInspectResult.Owned>().token shouldBeEqualTo newer
            fixture.healthy.release(OWNER, original) shouldBeEqualTo FencingReleaseResult.OwnershipMismatch
            fixture.healthy.inspect(OWNER)
                .shouldBeInstanceOf<FencingInspectResult.Owned>().token shouldBeEqualTo newer

            commands.set(fixture.keys.lease, "corrupted")
            fixture.healthy.inspect(OWNER).shouldBeInstanceOf<FencingInspectResult.IntegrityFailure>()
            fixture.healthy.release(OWNER, original).shouldBeInstanceOf<FencingReleaseResult.IntegrityFailure>()
            commands.get(fixture.keys.lease) shouldBeEqualTo "corrupted"

            val backendExecutor = OneShotFaultExecutor(
                fixture.defaultExecutor,
                FencingLeaseOperation.INSPECT,
                FaultPhase.BEFORE_APPLY,
            )
            val backendLease = LettuceFencingLease.createForTesting(
                backendExecutor,
                StringCodec.UTF8,
                fixture.config,
            )
            backendLease.inspect(OWNER).shouldBeInstanceOf<FencingInspectResult.BackendFailure>()
            backendExecutor.calls shouldBeEqualTo listOf(FencingLeaseOperation.INSPECT)
        }
    }

    private fun prepare(
        operation: FencingLeaseOperation,
        healthy: LettuceFencingLease,
    ): OperationContext = when (operation) {
        FencingLeaseOperation.BOOTSTRAP -> OperationContext()
        FencingLeaseOperation.ACQUIRE -> {
            healthy.bootstrap() shouldBeEqualTo FencingBootstrapResult.Initialized
            OperationContext()
        }
        FencingLeaseOperation.RENEW,
        FencingLeaseOperation.RELEASE,
        -> {
            healthy.bootstrap() shouldBeEqualTo FencingBootstrapResult.Initialized
            val token = healthy.acquire(OWNER, INITIAL_LEASE)
                .shouldBeInstanceOf<FencingAcquireResult.Acquired>().token
            OperationContext(token)
        }
        FencingLeaseOperation.INSPECT -> error("Inspect is not a mutation operation.")
    }

    private fun invokeSync(
        operation: FencingLeaseOperation,
        lease: LettuceFencingLease,
        context: OperationContext,
    ): Any = when (operation) {
        FencingLeaseOperation.BOOTSTRAP -> lease.bootstrap()
        FencingLeaseOperation.ACQUIRE -> lease.acquire(OWNER, INITIAL_LEASE)
        FencingLeaseOperation.RENEW -> lease.renew(OWNER, context.requiredToken(), RENEWED_LEASE)
        FencingLeaseOperation.RELEASE -> lease.release(OWNER, context.requiredToken())
        FencingLeaseOperation.INSPECT -> error("Inspect is not a mutation operation.")
    }

    private fun invokeFuture(
        operation: FencingLeaseOperation,
        lease: LettuceFencingLease,
        context: OperationContext,
    ): CompletableFuture<*> = when (operation) {
        FencingLeaseOperation.BOOTSTRAP -> lease.bootstrapAsync()
        FencingLeaseOperation.ACQUIRE -> lease.acquireAsync(OWNER, INITIAL_LEASE)
        FencingLeaseOperation.RENEW -> lease.renewAsync(OWNER, context.requiredToken(), RENEWED_LEASE)
        FencingLeaseOperation.RELEASE -> lease.releaseAsync(OWNER, context.requiredToken())
        FencingLeaseOperation.INSPECT -> error("Inspect is not a mutation operation.")
    }

    private suspend fun invokeSuspending(
        operation: FencingLeaseOperation,
        lease: LettuceSuspendFencingLease,
        context: OperationContext,
    ): Any = when (operation) {
        FencingLeaseOperation.BOOTSTRAP -> lease.bootstrap()
        FencingLeaseOperation.ACQUIRE -> lease.acquire(OWNER, INITIAL_LEASE)
        FencingLeaseOperation.RENEW -> lease.renew(OWNER, context.requiredToken(), RENEWED_LEASE)
        FencingLeaseOperation.RELEASE -> lease.release(OWNER, context.requiredToken())
        FencingLeaseOperation.INSPECT -> error("Inspect is not a mutation operation.")
    }

    private fun assertBackendFailure(operation: FencingLeaseOperation, result: Any) {
        val failure = when (operation) {
            FencingLeaseOperation.BOOTSTRAP ->
                result.shouldBeInstanceOf<FencingBootstrapResult.BackendFailure>().failure
            FencingLeaseOperation.ACQUIRE ->
                result.shouldBeInstanceOf<FencingAcquireResult.BackendFailure>().failure
            FencingLeaseOperation.RENEW ->
                result.shouldBeInstanceOf<FencingRenewResult.BackendFailure>().failure
            FencingLeaseOperation.RELEASE ->
                result.shouldBeInstanceOf<FencingReleaseResult.BackendFailure>().failure
            FencingLeaseOperation.INSPECT -> error("Inspect is not a mutation operation.")
        }
        failure.kind shouldBeEqualTo FencingBackendFailureKind.CONNECTION
    }

    private fun reconcile(
        operation: FencingLeaseOperation,
        phase: FaultPhase,
        healthy: LettuceFencingLease,
        context: OperationContext,
    ) {
        when (operation) {
            FencingLeaseOperation.BOOTSTRAP -> {
                val expected = if (phase == FaultPhase.BEFORE_APPLY) {
                    FencingBootstrapResult.Initialized
                } else {
                    FencingBootstrapResult.AlreadyInitialized
                }
                healthy.bootstrap() shouldBeEqualTo expected
            }
            FencingLeaseOperation.ACQUIRE -> {
                val result = healthy.acquire(OWNER, INITIAL_LEASE)
                val token = if (phase == FaultPhase.BEFORE_APPLY) {
                    result.shouldBeInstanceOf<FencingAcquireResult.Acquired>().token
                } else {
                    result.shouldBeInstanceOf<FencingAcquireResult.AlreadyOwned>().token
                }
                token shouldBeEqualTo FencingToken(CONFIG_EPOCH, 1)
            }
            FencingLeaseOperation.RENEW -> {
                val owned = healthy.inspect(OWNER).shouldBeInstanceOf<FencingInspectResult.Owned>()
                owned.token shouldBeEqualTo context.requiredToken()
                if (phase == FaultPhase.BEFORE_APPLY) {
                    owned.remainingTtlMillis shouldBeLessOrEqualTo INITIAL_LEASE.toMillis()
                } else {
                    owned.remainingTtlMillis shouldBeGreaterThan RENEWED_LEASE.toMillis() - TTL_TOLERANCE_MILLIS
                }
                healthy.renew(OWNER, context.requiredToken(), RENEWED_LEASE) shouldBeEqualTo
                    FencingRenewResult.Renewed
            }
            FencingLeaseOperation.RELEASE -> {
                if (phase == FaultPhase.BEFORE_APPLY) {
                    val owned = healthy.inspect(OWNER).shouldBeInstanceOf<FencingInspectResult.Owned>()
                    owned.token shouldBeEqualTo context.requiredToken()
                    healthy.release(OWNER, context.requiredToken()) shouldBeEqualTo FencingReleaseResult.Released
                } else {
                    healthy.inspect(OWNER) shouldBeSameInstanceAs FencingInspectResult.Lost
                }
            }
            FencingLeaseOperation.INSPECT -> error("Inspect is not a mutation operation.")
        }
    }

    private inline fun withFixture(
        operation: FencingLeaseOperation,
        phase: FaultPhase,
        block: (Fixture) -> Unit,
    ) {
        val suffix = randomName().substringAfter(':')
        val config = LettuceFencingLeaseConfig(
            "cancellation",
            "${operation.name.lowercase()}-${phase.name.lowercase()}-$suffix",
            CONFIG_EPOCH,
        )
        val keys = deriveFencingLeaseKeys(config, StringCodec.UTF8)
        val defaultExecutor = DefaultFencingScriptExecutor(connection.sync(), connection.async())
        val fixture = Fixture(
            config,
            keys,
            defaultExecutor,
            LettuceFencingLease.createForTesting(defaultExecutor, StringCodec.UTF8, config),
        )
        try {
            block(fixture)
        } finally {
            commands.del(keys.lease, keys.counter)
        }
    }

    private data class Fixture(
        val config: LettuceFencingLeaseConfig,
        val keys: FencingLeaseKeys,
        val defaultExecutor: DefaultFencingScriptExecutor,
        val healthy: LettuceFencingLease,
    )

    private data class OperationContext(
        val token: FencingToken? = null,
    ) {
        fun requiredToken(): FencingToken = requireNotNull(token)
    }

    private enum class FaultPhase {
        BEFORE_APPLY,
        AFTER_APPLY_BEFORE_REPLY,
    }

    private class OneShotFaultExecutor(
        private val delegate: FencingScriptExecutor,
        private val target: FencingLeaseOperation,
        private val phase: FaultPhase,
    ): FencingScriptExecutor {
        private val armed = AtomicBoolean(true)
        val calls = mutableListOf<FencingLeaseOperation>()

        override fun run(
            operation: FencingLeaseOperation,
            keys: FencingLeaseKeys,
            args: List<String>,
        ): List<String> {
            calls += operation
            if (operation != target || !armed.compareAndSet(true, false)) {
                return delegate.run(operation, keys, args)
            }
            if (phase == FaultPhase.BEFORE_APPLY) {
                throw backendFailure()
            }
            delegate.run(operation, keys, args)
            throw backendFailure()
        }

        override fun runAsync(
            operation: FencingLeaseOperation,
            keys: FencingLeaseKeys,
            args: List<String>,
        ): CompletableFuture<List<String>> = delegate.runAsync(operation, keys, args)

        override suspend fun runSuspending(
            operation: FencingLeaseOperation,
            keys: FencingLeaseKeys,
            args: List<String>,
        ): List<String> = delegate.runSuspending(operation, keys, args)

        private fun backendFailure(): RedisConnectionException =
            RedisConnectionException("fault injection sentinel")
    }

    private class ControlledCancellationExecutor(
        private val delegate: FencingScriptExecutor,
        private val target: FencingLeaseOperation,
        private val phase: FaultPhase,
    ): FencingScriptExecutor {
        val pending = CompletableFuture<List<String>>()
        val started = CompletableDeferred<Unit>()

        override fun run(
            operation: FencingLeaseOperation,
            keys: FencingLeaseKeys,
            args: List<String>,
        ): List<String> = delegate.run(operation, keys, args)

        override fun runAsync(
            operation: FencingLeaseOperation,
            keys: FencingLeaseKeys,
            args: List<String>,
        ): CompletableFuture<List<String>> {
            require(operation == target)
            applyIfRequired(operation, keys, args)
            started.complete(Unit)
            return pending
        }

        override suspend fun runSuspending(
            operation: FencingLeaseOperation,
            keys: FencingLeaseKeys,
            args: List<String>,
        ): List<String> {
            require(operation == target)
            applyIfRequired(operation, keys, args)
            started.complete(Unit)
            return suspendCancellableCoroutine { }
        }

        private fun applyIfRequired(
            operation: FencingLeaseOperation,
            keys: FencingLeaseKeys,
            args: List<String>,
        ) {
            if (phase == FaultPhase.AFTER_APPLY_BEFORE_REPLY) {
                delegate.run(operation, keys, args)
            }
        }
    }

    private companion object {
        const val CONFIG_EPOCH: Long = 17
        const val TTL_TOLERANCE_MILLIS: Long = 5_000
        val INITIAL_LEASE: Duration = Duration.ofSeconds(30)
        val RENEWED_LEASE: Duration = Duration.ofMinutes(5)
        val OWNER: FencingOwnerId = FencingOwnerId.from("cancellation-owner")
        val CONTENDER: FencingOwnerId = FencingOwnerId.from("cancellation-contender")
        val mutationOperations = listOf(
            FencingLeaseOperation.BOOTSTRAP,
            FencingLeaseOperation.ACQUIRE,
            FencingLeaseOperation.RENEW,
            FencingLeaseOperation.RELEASE,
        )

        val connection by lazy { LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8) }
        val commands by lazy { connection.sync() }
    }
}
