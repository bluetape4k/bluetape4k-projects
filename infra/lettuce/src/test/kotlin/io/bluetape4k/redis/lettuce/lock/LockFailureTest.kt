package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldNotContain
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRuntime
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationFailureClassification
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationProtocolException
import io.bluetape4k.redis.lettuce.lock.internal.DISTRIBUTED_LOCK_SCRIPT
import io.bluetape4k.redis.lettuce.lock.internal.DefaultLockCommandExecutor
import io.bluetape4k.redis.lettuce.lock.internal.DistributedLockClient
import io.bluetape4k.redis.lettuce.lock.internal.DistributedLockKeys
import io.bluetape4k.redis.lettuce.lock.internal.DistributedLockOperation
import io.bluetape4k.redis.lettuce.lock.internal.LockCommandExecutor
import io.bluetape4k.redis.lettuce.lock.internal.acquireBackendResult
import io.bluetape4k.redis.lettuce.lock.internal.decodeAcquire
import io.bluetape4k.redis.lettuce.lock.internal.deriveDistributedLockKeys
import io.lettuce.core.RedisCommandTimeoutException
import io.lettuce.core.RedisNoScriptException
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.async.RedisScriptingAsyncCommands
import io.lettuce.core.api.sync.RedisScriptingCommands
import io.lettuce.core.codec.StringCodec
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList

internal class LockFailureTest {

    @Test
    fun `unknown arity oversized and secret-bearing replies fail closed without disclosure`() {
        val secret = "customer-42-lock-secret"
        val malformed = listOf(
            listOf("UNKNOWN"),
            listOf("ACQUIRED"),
            listOf("CONTENDED", "1", "extra"),
            listOf("CONTENDED", secret.repeat(40)),
            listOf("ACQUIRED") + List(16) { "x" },
        )

        malformed.forEach { raw ->
            val failure = assertFailsWith<CoordinationProtocolException> {
                decodeAcquire(raw, KEYS, OWNER, REQUEST)
            }
            failure.classification shouldBeEqualTo CoordinationFailureClassification.INTEGRITY
            failure.message.orEmpty().contains(secret).shouldBeFalse()
        }
    }

    @Test
    fun `NOSCRIPT cold path uses evalsha then eval while warm path uses one evalsha`() {
        val sync = mockk<RedisScriptingCommands<String, String>>()
        val async = mockk<RedisScriptingAsyncCommands<String, String>>(relaxed = true)
        val executor = DefaultLockCommandExecutor(sync, async)
        val arguments = arrayOf(DistributedLockOperation.INSPECT.wireValue, "argument")
        val response = listOf("EXPIRED")

        every {
            sync.evalsha<List<String>>(
                DISTRIBUTED_LOCK_SCRIPT.sha1,
                ScriptOutputType.MULTI,
                KEYS.all,
                *arguments,
            )
        } throws RedisNoScriptException("NOSCRIPT") andThen response
        every {
            sync.eval<List<String>>(
                DISTRIBUTED_LOCK_SCRIPT.source,
                ScriptOutputType.MULTI,
                KEYS.all,
                *arguments,
            )
        } returns response

        executor.run(DistributedLockOperation.INSPECT, KEYS, listOf("argument")) shouldBeEqualTo response
        executor.run(DistributedLockOperation.INSPECT, KEYS, listOf("argument")) shouldBeEqualTo response

        verify(exactly = 2) {
            sync.evalsha<List<String>>(
                DISTRIBUTED_LOCK_SCRIPT.sha1,
                ScriptOutputType.MULTI,
                KEYS.all,
                *arguments,
            )
        }
        verify(exactly = 1) {
            sync.eval<List<String>>(
                DISTRIBUTED_LOCK_SCRIPT.source,
                ScriptOutputType.MULTI,
                KEYS.all,
                *arguments,
            )
        }
        confirmVerified(sync)
    }

    @Test
    fun `post-dispatch timeout is ambiguous and preserves identity across all acquire facades`() = runSuspendIO {
        val expected = LockAcquireResult.Ambiguous(
            OWNER,
            REQUEST,
            LockRecoveryAction.RECONCILE_REQUEST,
        )

        timeoutHarness().use { harness ->
            harness.client.tryAcquire(OWNER, REQUEST, LEASE) shouldBeEqualTo expected
            harness.executor.calls shouldBeEqualTo listOf(DistributedLockOperation.ACQUIRE)
        }
        timeoutHarness().use { harness ->
            harness.client.tryAcquireAsync(OWNER, REQUEST, LEASE).get() shouldBeEqualTo expected
            harness.executor.calls shouldBeEqualTo listOf(DistributedLockOperation.ACQUIRE)
        }
        timeoutHarness().use { harness ->
            harness.client.tryAcquireSuspending(OWNER, REQUEST, LEASE) shouldBeEqualTo expected
            harness.executor.calls shouldBeEqualTo listOf(DistributedLockOperation.ACQUIRE)
        }
    }

    @Test
    fun `ambiguous diagnostics redact identity and observations expose only allowlisted dimensions`() {
        val owner = LockOwnerId.from("customer-owner-secret")
        val request = LockRequestId.from("customer-request-secret")
        val rendered = LockAcquireResult.Ambiguous(
            owner,
            request,
            LockRecoveryAction.RECONCILE_REQUEST,
        ).toString()

        rendered shouldNotContain "customer-owner-secret"
        rendered shouldNotContain "customer-request-secret"
        LockDimensions::class.java.declaredFields
            .filterNot { it.isSynthetic }
            .map { it.name }
            .sorted() shouldBeEqualTo
            listOf("failureKind", "leasePolicy", "objectKind", "operation", "outcome")
    }

    @Test
    fun `all lock family acquire result types share conservative transport ambiguity`() {
        val expected = LockAcquireResult.Ambiguous(
            OWNER,
            REQUEST,
            LockRecoveryAction.RECONCILE_REQUEST,
        )
        val timeout = LockBackendFailure(LockBackendFailureKind.TIMEOUT, LockRecoveryAction.RECONCILE_REQUEST)
        val connection = LockBackendFailure(LockBackendFailureKind.CONNECTION, LockRecoveryAction.RECONCILE_REQUEST)
        val command = LockBackendFailure(LockBackendFailureKind.COMMAND, LockRecoveryAction.RECONCILE_REQUEST)

        listOf(
            acquireBackendResult<LockHandle>(OWNER, REQUEST, timeout),
            acquireBackendResult<FencedLockHandle>(OWNER, REQUEST, connection),
            acquireBackendResult<ReadLockHandle>(OWNER, REQUEST, timeout),
            acquireBackendResult<WriteLockHandle>(OWNER, REQUEST, connection),
            acquireBackendResult<MultiLockHandle>(OWNER, REQUEST, timeout),
        ).forEach { it shouldBeEqualTo expected }
        acquireBackendResult<LockHandle>(OWNER, REQUEST, command) shouldBeEqualTo
            LockAcquireResult.BackendFailure(command)
    }

    private fun timeoutHarness(): TimeoutHarness {
        val runtime = CoordinationRuntime()
        val executor = TimeoutAfterDispatchExecutor()
        val client = DistributedLockClient(
            keys = KEYS,
            config = LockConfig(hashTag = "failure"),
            executor = executor,
            registration = runtime.registerObject(KEYS.fingerprint),
            observationSink = LockObservationSink.NOOP,
        )
        return TimeoutHarness(client, executor)
    }

    private data class TimeoutHarness(
        val client: DistributedLockClient,
        val executor: TimeoutAfterDispatchExecutor,
    ): AutoCloseable {
        override fun close() = client.close()
    }

    private class TimeoutAfterDispatchExecutor: LockCommandExecutor {
        val calls = CopyOnWriteArrayList<DistributedLockOperation>()

        override fun run(
            operation: DistributedLockOperation,
            keys: DistributedLockKeys,
            args: List<String>,
        ): List<String> {
            calls += operation
            throw RedisCommandTimeoutException("post-dispatch timeout")
        }

        override fun runAsync(
            operation: DistributedLockOperation,
            keys: DistributedLockKeys,
            args: List<String>,
        ): CompletableFuture<List<String>> {
            calls += operation
            return CompletableFuture.failedFuture(RedisCommandTimeoutException("post-dispatch timeout"))
        }

        override suspend fun runSuspending(
            operation: DistributedLockOperation,
            keys: DistributedLockKeys,
            args: List<String>,
        ): List<String> {
            calls += operation
            throw RedisCommandTimeoutException("post-dispatch timeout")
        }
    }

    private companion object {
        val OWNER = LockOwnerId.from("failure-owner")
        val REQUEST = LockRequestId.from("failure-request")
        val LEASE: LeasePolicy = LeasePolicy.Fixed(Duration.ofSeconds(3))
        val KEYS = deriveDistributedLockKeys(
            "failure-resource",
            LockConfig(hashTag = "failure"),
            StringCodec.UTF8,
        )
    }
}
