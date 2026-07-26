package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeZero
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.redis.lettuce.lock.internal.DistributedLockKeys
import io.bluetape4k.redis.lettuce.lock.internal.DISTRIBUTED_LOCK_SCRIPT
import io.bluetape4k.redis.lettuce.lock.internal.DefaultLockCommandExecutor
import io.bluetape4k.redis.lettuce.lock.internal.DistributedLockOperation
import io.bluetape4k.redis.lettuce.lock.internal.deriveDistributedLockKeys
import io.lettuce.core.RedisNoScriptException
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisScriptingAsyncCommands
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.api.sync.RedisScriptingCommands
import io.lettuce.core.codec.StringCodec
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

internal class DistributedLockScriptTest : AbstractLettuceTest() {

    private lateinit var connection: StatefulRedisConnection<String, String>
    private lateinit var commands: RedisCommands<String, String>
    private lateinit var lock: LettuceDistributedLock
    private lateinit var keys: DistributedLockKeys

    private val owner = LockOwnerId.from("script-owner")
    private val lease = LeasePolicy.Fixed(Duration.ofSeconds(3))

    @BeforeEach
    fun setUp() {
        connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        commands = connection.sync()
        val name = "script-${randomName().substringAfter(':')}"
        keys = deriveDistributedLockKeys(name, LockConfig(), StringCodec.UTF8)
        deleteKeys()
        lock = LettuceDistributedLock.create(connection, name)
    }

    @AfterEach
    fun tearDown() {
        try {
            lock.close()
            deleteKeys()
        } finally {
            connection.close()
        }
    }

    @Test
    fun `final release preserves the generation counter and script flush falls back`() {
        val first = lock.tryAcquire(owner, LockRequestId.from("first-request"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()
            .handle
        commands.scriptFlush()
        lock.release(first) shouldBeEqualTo LockMutationResult.Released(0)

        commands.exists(keys.state, keys.holds).shouldBeZero()
        commands.get(keys.generation) shouldBeEqualTo first.generation.value.toString()
        val second = lock.tryAcquire(owner, LockRequestId.from("second-request"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()
            .handle
        second.generation shouldBeGreaterThan first.generation
    }

    @Test
    fun `malformed active state fails closed without mutation`() {
        commands.set(keys.generation, "1")
        commands.set(keys.state, "wrong-type")
        val before = commands.dump(keys.state)

        lock.tryAcquire(owner, LockRequestId.from("malformed-request"), lease)
            .shouldBeInstanceOf<LockAcquireResult.IntegrityFailure>()
        commands.dump(keys.state).contentEquals(before) shouldBeEqualTo true
        commands.get(keys.generation) shouldBeEqualTo "1"
    }

    @Test
    fun `persistent active state fails closed`() {
        val handle = lock.tryAcquire(owner, LockRequestId.from("persistent-request"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()
            .handle
        commands.persist(keys.state)

        lock.inspect(handle).shouldBeInstanceOf<LockInspectResult.IntegrityFailure>()
        commands.ttl(keys.state) shouldBeEqualTo -1L
    }

    @Test
    fun `maximum exact generation rejects acquisition without mutation`() {
        commands.set(keys.generation, MAX_EXACT_GENERATION)

        lock.tryAcquire(owner, LockRequestId.from("exhausted-request"), lease) shouldBeEqualTo
            LockAcquireResult.CapacityExceeded
        commands.exists(keys.state, keys.holds).shouldBeZero()
        commands.get(keys.generation) shouldBeEqualTo MAX_EXACT_GENERATION
    }

    @Test
    fun `warm operations dispatch evalsha exactly once and noscript alone falls back`() {
        val sync = mockk<RedisScriptingCommands<String, String>>()
        val async = mockk<RedisScriptingAsyncCommands<String, String>>()
        val executor = DefaultLockCommandExecutor(sync, async)
        val scriptKeys = keys.all
        val response = listOf("EXPIRED")

        DistributedLockOperation.entries.forEachIndexed { index, operation ->
            val arguments = arrayOf(operation.wireValue, "argument-$index")
            every {
                sync.evalsha<List<String>>(
                    DISTRIBUTED_LOCK_SCRIPT.sha1,
                    ScriptOutputType.MULTI,
                    scriptKeys,
                    *arguments,
                )
            } returns response

            executor.run(operation, keys, listOf("argument-$index")) shouldBeEqualTo response

            verify(exactly = 1) {
                sync.evalsha<List<String>>(
                    DISTRIBUTED_LOCK_SCRIPT.sha1,
                    ScriptOutputType.MULTI,
                    scriptKeys,
                    *arguments,
                )
            }
        }
        confirmVerified(sync)

        val fallbackSync = mockk<RedisScriptingCommands<String, String>>()
        val fallback = DefaultLockCommandExecutor(fallbackSync, async)
        val fallbackArguments = arrayOf(DistributedLockOperation.INSPECT.wireValue, "fallback")
        every {
            fallbackSync.evalsha<List<String>>(
                DISTRIBUTED_LOCK_SCRIPT.sha1,
                ScriptOutputType.MULTI,
                scriptKeys,
                *fallbackArguments,
            )
        } throws RedisNoScriptException("NOSCRIPT")
        every {
            fallbackSync.eval<List<String>>(
                DISTRIBUTED_LOCK_SCRIPT.source,
                ScriptOutputType.MULTI,
                scriptKeys,
                *fallbackArguments,
            )
        } returns response

        fallback.run(DistributedLockOperation.INSPECT, keys, listOf("fallback")) shouldBeEqualTo response

        verify(exactly = 1) {
            fallbackSync.evalsha<List<String>>(
                DISTRIBUTED_LOCK_SCRIPT.sha1,
                ScriptOutputType.MULTI,
                scriptKeys,
                *fallbackArguments,
            )
            fallbackSync.eval<List<String>>(
                DISTRIBUTED_LOCK_SCRIPT.source,
                ScriptOutputType.MULTI,
                scriptKeys,
                *fallbackArguments,
            )
        }
        confirmVerified(fallbackSync)
    }

    private fun deleteKeys() {
        commands.del(keys.state, keys.generation, keys.holds, keys.terminal)
    }

    private companion object {
        const val MAX_EXACT_GENERATION = "9007199254740991"
    }
}
