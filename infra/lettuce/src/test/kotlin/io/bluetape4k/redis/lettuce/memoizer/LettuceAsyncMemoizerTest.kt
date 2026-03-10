package io.bluetape4k.redis.lettuce.memoizer

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.map.LettuceMap
import io.lettuce.core.codec.StringCodec
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class LettuceAsyncMemoizerTest: AbstractLettuceTest() {

    companion object: KLoggingChannel()

    private lateinit var redisMap: LettuceMap<String>
    private lateinit var asyncMemoizer: LettuceAsyncMemoizer

    private val evaluateCount = AtomicInteger(0)

    @BeforeEach
    fun setup() {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        val mapKey = randomName()
        redisMap = LettuceMap(connection, mapKey)
        evaluateCount.set(0)

        asyncMemoizer = redisMap.asyncMemoizer { key ->
            evaluateCount.incrementAndGet()
            "async-value-$key"
        }
    }

    @AfterEach
    fun teardown() {
        redisMap.clear()
    }

    // =========================================================================
    // LettuceAsyncMemorizer (비동기)
    // =========================================================================

    @Test
    fun `비동기 - 첫 번째 호출 시 evaluator 실행`() {
        val result = asyncMemoizer("key1").get()
        result shouldBeEqualTo "async-value-key1"
        evaluateCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `비동기 - 두 번째 호출 시 캐시에서 반환`() {
        asyncMemoizer("key1").get()
        val result = asyncMemoizer("key1").get()
        result shouldBeEqualTo "async-value-key1"
        evaluateCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `비동기 - 다른 키는 별도 evaluator 실행`() {
        asyncMemoizer("key1").get()
        asyncMemoizer("key2").get()
        evaluateCount.get() shouldBeEqualTo 2
    }

    @Test
    fun `비동기 - clear 후 재평가`() {
        asyncMemoizer("key1").get()
        asyncMemoizer.clear()
        asyncMemoizer("key1").get()
        evaluateCount.get() shouldBeEqualTo 2
    }
}
