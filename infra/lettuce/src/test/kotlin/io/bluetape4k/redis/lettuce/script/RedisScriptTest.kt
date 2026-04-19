package io.bluetape4k.redis.lettuce.script

import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RedisScriptTest : AbstractLettuceTest() {

    companion object : KLogging()

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

    private lateinit var syncCommands: io.lettuce.core.api.sync.RedisCommands<String, String>
    private lateinit var asyncCommands: io.lettuce.core.api.async.RedisAsyncCommands<String, String>

    @BeforeEach
    fun setup() {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        syncCommands = connection.sync()
        asyncCommands = connection.async()
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
        (script1.sha1 == script2.sha1) shouldBeEqualTo false
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
    fun `동기 run - EVALSHA fallback 동작 (SCRIPT FLUSH 후)`() {
        val key = randomName()
        val value = "world"
        // SCRIPT FLUSH로 Redis 서버의 스크립트 캐시를 비워 NOSCRIPT fallback 경로 테스트
        syncCommands.scriptFlush()

        val result: String = RedisScriptRunner.run(
            syncCommands,
            setAndReturnScript,
            ScriptOutputType.VALUE,
            arrayOf(key),
            value
        )

        result shouldBeEqualTo value
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

    @Test
    fun `비동기 runAsync - SCRIPT FLUSH 후 NOSCRIPT fallback 동작`() {
        val key = randomName()
        val value = "async-fallback"
        syncCommands.scriptFlush()

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
    fun `코루틴 runSuspending - SET 후 GET 반환`() = runTest {
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
    fun `코루틴 runSuspending - SCRIPT FLUSH 후 NOSCRIPT fallback 동작`() = runTest {
        val key = randomName()
        val value = "suspend-fallback"
        syncCommands.scriptFlush()

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
    fun `INTEGER 반환 타입 스크립트 테스트`() = runTest {
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
}
