package io.bluetape4k.redis.lettuce.script

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.testcontainers.storage.RedisServer
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisFuture
import io.lettuce.core.RedisNoScriptException
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.async.RedisScriptingAsyncCommands
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.api.sync.RedisScriptingCommands
import io.lettuce.core.codec.StringCodec
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RedisScriptTest : AbstractLettuceTest() {

    companion object : KLogging() {
        private val connection by lazy { LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8) }
    }

    private val setAndReturnScript = RedisScript(
        """
        redis.call('SET', KEYS[1], ARGV[1])
        return redis.call('GET', KEYS[1])
        """.trimIndent()
    )

    private val incrAndReturnScript = RedisScript(
        """
        redis.call('INCR', KEYS[1])
        return tonumber(redis.call('GET', KEYS[1]))
        """.trimIndent()
    )

    private lateinit var syncCommands: RedisCommands<String, String>
    private lateinit var asyncCommands: RedisAsyncCommands<String, String>
    private lateinit var syncScriptingCommands: RedisScriptingCommands<String, String>
    private lateinit var asyncScriptingCommands: RedisScriptingAsyncCommands<String, String>

    @BeforeEach
    fun setup() {
        connection.sync().let { commands ->
            syncCommands = commands
            syncScriptingCommands = commands
        }
        connection.async().let { commands ->
            asyncCommands = commands
            asyncScriptingCommands = commands
        }
    }

    // =========================================================================
    // RedisScript SHA1 계산 테스트
    // =========================================================================

    @Test
    fun `같은 스크립트는 같은 SHA1을 반환한다`() {
        val script1 = RedisScript("return 1")
        val script2 = RedisScript("return 1")
        script1.sha1 shouldBeEqualTo script2.sha1
    }

    @Test
    fun `다른 스크립트는 다른 SHA1을 반환한다`() {
        val script1 = RedisScript("return 1")
        val script2 = RedisScript("return 2")
        (script1.sha1 == script2.sha1).shouldBeFalse()
    }

    @Test
    fun `SHA1은 40자리 16진수 문자열이다`() {
        val script = RedisScript("return 1")
        script.sha1.length shouldBeEqualTo 40
        script.sha1.shouldNotBeEmpty()
    }

    // =========================================================================
    // RedisScriptRunner 동기 테스트
    // =========================================================================

    @Test
    fun `동기 run - SET 후 GET 반환`() {
        val key = randomName()
        val value = "hello"

        val result: String = RedisScriptRunner.run(
            syncCommands,
            setAndReturnScript,
            ScriptOutputType.VALUE,
            arrayOf(key),
            value
        )

        result shouldBeEqualTo value
    }

    @Test
    fun `스크립팅 명령 인터페이스로 동기 비동기 코루틴 실행`() = runSuspendIO {
        val syncValue = "scripting-sync"
        val asyncValue = "scripting-async"
        val suspendValue = "scripting-suspend"

        val syncResult: String = RedisScriptRunner.run(
            syncScriptingCommands,
            setAndReturnScript,
            ScriptOutputType.VALUE,
            arrayOf(randomName()),
            syncValue,
        )
        val asyncResult: String = RedisScriptRunner.runAsync<String>(
            asyncScriptingCommands,
            setAndReturnScript,
            ScriptOutputType.VALUE,
            arrayOf(randomName()),
            asyncValue,
        ).get()
        val suspendResult: String = RedisScriptRunner.runSuspending(
            asyncScriptingCommands,
            setAndReturnScript,
            ScriptOutputType.VALUE,
            arrayOf(randomName()),
            suspendValue,
        )

        syncResult shouldBeEqualTo syncValue
        asyncResult shouldBeEqualTo asyncValue
        suspendResult shouldBeEqualTo suspendValue
    }

    // =========================================================================
    // RedisScriptRunner 비동기 테스트
    // =========================================================================

    @Test
    fun `비동기 runAsync - SET 후 GET 반환`() {
        val key = randomName()
        val value = "async-val"

        val result: String = RedisScriptRunner.runAsync<String>(
            asyncCommands,
            setAndReturnScript,
            ScriptOutputType.VALUE,
            arrayOf(key),
            value
        ).get()

        result shouldBeEqualTo value
    }

    // =========================================================================
    // RedisScriptRunner 코루틴 테스트
    // =========================================================================

    @Test
    fun `코루틴 runSuspending - SET 후 GET 반환`() = runSuspendIO {
        val key = randomName()
        val value = "suspend-val"

        val result: String = RedisScriptRunner.runSuspending(
            asyncCommands,
            setAndReturnScript,
            ScriptOutputType.VALUE,
            arrayOf(key),
            value
        )

        result shouldBeEqualTo value
    }

    @Test
    fun `dedicated Redis proves sync async and suspend NOSCRIPT fallback after script flush`() = runSuspendIO {
        withDedicatedRedis { dedicatedConnection ->
            val dedicatedSync = dedicatedConnection.sync()
            val dedicatedAsync = dedicatedConnection.async()

            dedicatedSync.scriptFlush()
            RedisScriptRunner.run<String>(
                dedicatedSync,
                setAndReturnScript,
                ScriptOutputType.VALUE,
                arrayOf(randomName()),
                "sync-fallback",
            ) shouldBeEqualTo "sync-fallback"

            dedicatedSync.scriptFlush()
            RedisScriptRunner.runAsync<String>(
                dedicatedAsync,
                setAndReturnScript,
                ScriptOutputType.VALUE,
                arrayOf(randomName()),
                "async-fallback",
            ).get() shouldBeEqualTo "async-fallback"

            dedicatedSync.scriptFlush()
            RedisScriptRunner.runSuspending<String>(
                dedicatedAsync,
                setAndReturnScript,
                ScriptOutputType.VALUE,
                arrayOf(randomName()),
                "suspend-fallback",
            ) shouldBeEqualTo "suspend-fallback"
        }
    }

    @Test
    fun `sync scripting interface dispatches evalsha and eval exactly once on NOSCRIPT`() {
        val commands = mockk<RedisScriptingCommands<String, String>>()
        val keys = arrayOf("key:{sync}")
        every {
            commands.evalsha<String>(setAndReturnScript.sha1, ScriptOutputType.VALUE, keys, "value")
        } throws RedisNoScriptException("NOSCRIPT")
        every {
            commands.eval<String>(setAndReturnScript.source, ScriptOutputType.VALUE, keys, "value")
        } returns "value"

        RedisScriptRunner.run<String>(
            commands,
            setAndReturnScript,
            ScriptOutputType.VALUE,
            keys,
            "value",
        ) shouldBeEqualTo "value"

        verify(exactly = 1) {
            commands.evalsha<String>(setAndReturnScript.sha1, ScriptOutputType.VALUE, keys, "value")
            commands.eval<String>(setAndReturnScript.source, ScriptOutputType.VALUE, keys, "value")
        }
        confirmVerified(commands)
    }

    @Test
    fun `async scripting interface dispatches evalsha and eval exactly once on NOSCRIPT`() {
        val commands = mockk<RedisScriptingAsyncCommands<String, String>>()
        val keys = arrayOf("key:{async}")
        every {
            commands.evalsha<String>(setAndReturnScript.sha1, ScriptOutputType.VALUE, keys, "value")
        } returns failedRedisFuture(RedisNoScriptException("NOSCRIPT"))
        every {
            commands.eval<String>(setAndReturnScript.source, ScriptOutputType.VALUE, keys, "value")
        } returns completedRedisFuture("value")

        RedisScriptRunner.runAsync<String>(
            commands,
            setAndReturnScript,
            ScriptOutputType.VALUE,
            keys,
            "value",
        ).get() shouldBeEqualTo "value"

        verify(exactly = 1) {
            commands.evalsha<String>(setAndReturnScript.sha1, ScriptOutputType.VALUE, keys, "value")
            commands.eval<String>(setAndReturnScript.source, ScriptOutputType.VALUE, keys, "value")
        }
        confirmVerified(commands)
    }

    @Test
    fun `suspend scripting interface dispatches evalsha and eval exactly once on NOSCRIPT`() = runTest {
        val commands = mockk<RedisScriptingAsyncCommands<String, String>>()
        val keys = arrayOf("key:{suspend}")
        every {
            commands.evalsha<String>(setAndReturnScript.sha1, ScriptOutputType.VALUE, keys, "value")
        } returns failedRedisFuture(RedisNoScriptException("NOSCRIPT"))
        every {
            commands.eval<String>(setAndReturnScript.source, ScriptOutputType.VALUE, keys, "value")
        } returns completedRedisFuture("value")

        RedisScriptRunner.runSuspending<String>(
            commands,
            setAndReturnScript,
            ScriptOutputType.VALUE,
            keys,
            "value",
        ) shouldBeEqualTo "value"

        verify(exactly = 1) {
            commands.evalsha<String>(setAndReturnScript.sha1, ScriptOutputType.VALUE, keys, "value")
            commands.eval<String>(setAndReturnScript.source, ScriptOutputType.VALUE, keys, "value")
        }
        confirmVerified(commands)
    }

    @Test
    fun `suspend NOSCRIPT fallback cancellation cancels the eval future`() = runTest {
        val commands = mockk<RedisScriptingAsyncCommands<String, String>>()
        val keys = arrayOf("key:{suspend-cancel}")
        val fallback = TestRedisFuture<String>()
        every {
            commands.evalsha<String>(setAndReturnScript.sha1, ScriptOutputType.VALUE, keys, "value")
        } returns failedRedisFuture(RedisNoScriptException("NOSCRIPT"))
        every {
            commands.eval<String>(setAndReturnScript.source, ScriptOutputType.VALUE, keys, "value")
        } returns fallback

        val job = launch {
            RedisScriptRunner.runSuspending<String>(
                commands,
                setAndReturnScript,
                ScriptOutputType.VALUE,
                keys,
                "value",
            )
        }
        runCurrent()
        job.cancelAndJoin()

        fallback.isCancelled.shouldBeTrue()
        verify(exactly = 1) {
            commands.evalsha<String>(setAndReturnScript.sha1, ScriptOutputType.VALUE, keys, "value")
            commands.eval<String>(setAndReturnScript.source, ScriptOutputType.VALUE, keys, "value")
        }
        confirmVerified(commands)
    }

    @Test
    fun `INTEGER 반환 타입 스크립트 테스트`() = runSuspendIO {
        val key = randomName()
        syncCommands.set(key, "0")

        val result: Long = RedisScriptRunner.runSuspending(
            asyncCommands,
            incrAndReturnScript,
            ScriptOutputType.INTEGER,
            arrayOf(key)
        )

        result shouldBeEqualTo 1L
    }

    private suspend fun <T> withDedicatedRedis(
        block: suspend (StatefulRedisConnection<String, String>) -> T,
    ): T {
        val server = RedisServer()
        try {
            server.start()
            val dedicatedClient = LettuceClients.clientOf(server.host, server.port)
            try {
                val dedicatedConnection = dedicatedClient.connect(StringCodec.UTF8)
                try {
                    return block(dedicatedConnection)
                } finally {
                    dedicatedConnection.close()
                }
            } finally {
                dedicatedClient.shutdown()
            }
        } finally {
            server.close()
        }
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
}
