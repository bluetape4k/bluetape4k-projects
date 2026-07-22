package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeInstanceOf
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.lettuce.core.RedisCommandTimeoutException
import io.lettuce.core.RedisConnectionException
import io.lettuce.core.RedisException
import io.lettuce.core.RedisFuture
import io.lettuce.core.RedisNoScriptException
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.async.RedisScriptingAsyncCommands
import io.lettuce.core.api.sync.RedisScriptingCommands
import io.lettuce.core.codec.StringCodec
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.io.Serializable
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

internal class LettuceFencingLeaseFailureTest {

    @Test
    fun `execution facades are not serializable`() {
        lease(mockk()).shouldNotBeInstanceOf<Serializable>()
        suspendLease(mockk<RedisScriptingAsyncCommands<String, String>>())
            .shouldNotBeInstanceOf<Serializable>()
    }

    @Test
    fun `sync future and suspend facades classify the same backend failures`() = runSuspendIO {
        val failures = listOf(
            RedisConnectionException("connection sentinel") to FencingBackendFailureKind.CONNECTION,
            RedisCommandTimeoutException("timeout sentinel") to FencingBackendFailureKind.TIMEOUT,
            RedisException("command sentinel") to FencingBackendFailureKind.COMMAND,
        )

        failures.forEach { (error, expectedKind) ->
            syncLease(error).bootstrap()
                .shouldBeInstanceOf<FencingBootstrapResult.BackendFailure>()
                .failure.kind shouldBeEqualTo expectedKind
            futureLease(error).bootstrapAsync().get()
                .shouldBeInstanceOf<FencingBootstrapResult.BackendFailure>()
                .failure.kind shouldBeEqualTo expectedKind
            suspendLease(error).bootstrap()
                .shouldBeInstanceOf<FencingBootstrapResult.BackendFailure>()
                .failure.kind shouldBeEqualTo expectedKind
        }
    }

    @Test
    fun `decoder and unknown failures remain exceptions for every facade`() = runSuspendIO {
        val unknown = IllegalStateException("unknown sentinel")

        assertFailsWith<IllegalStateException> { syncLease(unknown).bootstrap() } shouldBeSameInstanceAs unknown
        assertFailsWith<ExecutionException> { futureLease(unknown).bootstrapAsync().get() }
            .cause shouldBeSameInstanceAs unknown
        assertFailsWith<IllegalStateException> { suspendLease(unknown).bootstrap() } shouldBeSameInstanceAs unknown

        assertFailsWith<FencingLeaseProtocolException> { syncLease(frame = malformedFrame).bootstrap() }
        assertFailsWith<ExecutionException> { futureLease(frame = malformedFrame).bootstrapAsync().get() }
            .cause.shouldBeInstanceOf<FencingLeaseProtocolException>()
        assertFailsWith<FencingLeaseProtocolException> { suspendLease(frame = malformedFrame).bootstrap() }
    }

    @Test
    fun `future completes exceptionally when an unknown error escapes classification`() {
        val error = AssertionError("error sentinel")

        assertFailsWith<ExecutionException> {
            futureLease(error).bootstrapAsync().get(1, TimeUnit.SECONDS)
        }.cause shouldBeSameInstanceAs error
    }

    @Test
    fun `invalid inputs fail before every facade dispatches Redis commands`() = runSuspendIO {
        val syncCommands = mockk<RedisScriptingCommands<String, String>>()
        val futureCommands = mockk<RedisScriptingAsyncCommands<String, String>>()
        val suspendCommands = mockk<RedisScriptingAsyncCommands<String, String>>()
        val lease = LettuceFencingLease.createForTesting(
            DefaultFencingScriptExecutor(syncCommands, futureCommands),
            StringCodec.UTF8,
            config,
        )
        val suspendLease = LettuceSuspendFencingLease.createForTesting(
            DefaultFencingScriptExecutor(mockk(), suspendCommands),
            StringCodec.UTF8,
            config,
        )
        val ownerId = FencingOwnerId.from("validation-owner")
        val otherEpoch = FencingToken(config.epoch + 1, 1)
        val invalidLeaseTimes = listOf(
            Duration.ZERO,
            Duration.ofNanos(1),
            Duration.ofMillis(MAX_EXACT_REDIS_LEASE_TIME_MILLIS + 1),
        )

        invalidLeaseTimes.forEach { leaseTime ->
            assertFailsWith<IllegalArgumentException> { lease.acquire(ownerId, leaseTime) }
            assertFailsWith<IllegalArgumentException> { lease.acquireAsync(ownerId, leaseTime) }
            assertFailsWith<IllegalArgumentException> { suspendLease.acquire(ownerId, leaseTime) }
        }
        assertFailsWith<IllegalArgumentException> { lease.renew(ownerId, otherEpoch, Duration.ofSeconds(1)) }
        assertFailsWith<IllegalArgumentException> { lease.renewAsync(ownerId, otherEpoch, Duration.ofSeconds(1)) }
        assertFailsWith<IllegalArgumentException> { suspendLease.renew(ownerId, otherEpoch, Duration.ofSeconds(1)) }
        assertFailsWith<IllegalArgumentException> { lease.release(ownerId, otherEpoch) }
        assertFailsWith<IllegalArgumentException> { lease.releaseAsync(ownerId, otherEpoch) }
        assertFailsWith<IllegalArgumentException> { suspendLease.release(ownerId, otherEpoch) }

        verify { syncCommands wasNot Called }
        verify { futureCommands wasNot Called }
        verify { suspendCommands wasNot Called }
    }

    @Test
    fun `public future cancellation reaches evalsha and noscript eval`() {
        val evalsha = TestRedisFuture<List<String>>()
        val commands = asyncCommands(evalsha)
        val result = lease(commands).bootstrapAsync()

        result.cancel(true).shouldBeTrue()
        result.isCancelled.shouldBeTrue()
        evalsha.isCancelled.shouldBeTrue()

        val noScript = failedRedisFuture<List<String>>(RedisNoScriptException("NOSCRIPT"))
        val fallback = TestRedisFuture<List<String>>()
        val fallbackCommands = asyncCommands(noScript, fallback)
        val fallbackResult = lease(fallbackCommands).bootstrapAsync()

        fallbackResult.cancel(true).shouldBeTrue()
        fallbackResult.isCancelled.shouldBeTrue()
        fallback.isCancelled.shouldBeTrue()
    }

    private fun syncLease(error: Throwable): LettuceFencingLease {
        val commands = mockk<RedisScriptingCommands<String, String>>()
        every {
            commands.evalsha<List<String>>(
                FencingLeaseScripts.BOOTSTRAP.sha1,
                ScriptOutputType.MULTI,
                scriptKeys,
                config.epoch.toString(),
            )
        } throws error
        return LettuceFencingLease.createForTesting(
            DefaultFencingScriptExecutor(commands, mockk()),
            StringCodec.UTF8,
            config,
        )
    }

    private fun futureLease(error: Throwable): LettuceFencingLease =
        lease(asyncCommands(failedRedisFuture(error)))

    private fun suspendLease(error: Throwable): LettuceSuspendFencingLease =
        suspendLease(asyncCommands(failedRedisFuture(error)))

    private fun syncLease(frame: List<String>): LettuceFencingLease {
        val commands = mockk<RedisScriptingCommands<String, String>>()
        every {
            commands.evalsha<List<String>>(
                FencingLeaseScripts.BOOTSTRAP.sha1,
                ScriptOutputType.MULTI,
                scriptKeys,
                config.epoch.toString(),
            )
        } returns frame
        return LettuceFencingLease.createForTesting(
            DefaultFencingScriptExecutor(commands, mockk()),
            StringCodec.UTF8,
            config,
        )
    }

    private fun futureLease(frame: List<String>): LettuceFencingLease =
        lease(asyncCommands(completedRedisFuture(frame)))

    private fun suspendLease(frame: List<String>): LettuceSuspendFencingLease =
        suspendLease(asyncCommands(completedRedisFuture(frame)))

    private fun lease(commands: RedisScriptingAsyncCommands<String, String>): LettuceFencingLease =
        LettuceFencingLease.createForTesting(
            DefaultFencingScriptExecutor(mockk(), commands),
            StringCodec.UTF8,
            config,
        )

    private fun suspendLease(commands: RedisScriptingAsyncCommands<String, String>): LettuceSuspendFencingLease =
        LettuceSuspendFencingLease.createForTesting(
            DefaultFencingScriptExecutor(mockk(), commands),
            StringCodec.UTF8,
            config,
        )

    private fun asyncCommands(
        evalsha: RedisFuture<List<String>>,
        eval: RedisFuture<List<String>>? = null,
    ): RedisScriptingAsyncCommands<String, String> {
        val commands = mockk<RedisScriptingAsyncCommands<String, String>>()
        every {
            commands.evalsha<List<String>>(
                FencingLeaseScripts.BOOTSTRAP.sha1,
                ScriptOutputType.MULTI,
                scriptKeys,
                config.epoch.toString(),
            )
        } returns evalsha
        if (eval != null) {
            every {
                commands.eval<List<String>>(
                    FencingLeaseScripts.BOOTSTRAP.source,
                    ScriptOutputType.MULTI,
                    scriptKeys,
                    config.epoch.toString(),
                )
            } returns eval
        }
        return commands
    }

    private fun <T> completedRedisFuture(value: T): RedisFuture<T> =
        TestRedisFuture<T>().apply { complete(value) }

    private fun <T> failedRedisFuture(error: Throwable): RedisFuture<T> =
        TestRedisFuture<T>().apply { completeExceptionally(error) }

    private class TestRedisFuture<T> : CompletableFuture<T>(), RedisFuture<T> {
        override fun getError(): String? = if (isCompletedExceptionally) "completed exceptionally" else null

        override fun await(timeout: Long, unit: TimeUnit): Boolean = try {
            get(timeout, unit)
            true
        } catch (_: TimeoutException) {
            false
        }
    }

    private companion object {
        val config = LettuceFencingLeaseConfig("failure", "fixture", 13)
        val keys = deriveFencingLeaseKeys(config, StringCodec.UTF8)
        val scriptKeys = arrayOf(keys.lease, keys.counter)
        val malformedFrame = listOf("UNKNOWN", "0", "0", "-1")
    }
}
