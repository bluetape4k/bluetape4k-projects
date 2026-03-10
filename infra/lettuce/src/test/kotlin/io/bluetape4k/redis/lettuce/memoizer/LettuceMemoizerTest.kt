package io.bluetape4k.redis.lettuce.memoizer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.map.LettuceMap
import io.lettuce.core.codec.StringCodec
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class LettuceMemoizerTest: AbstractLettuceTest() {

    companion object: KLogging()

    private lateinit var redisMap: LettuceMap<String>
    private lateinit var memoizer: LettuceMemoizer

    private val evaluateCount = AtomicInteger(0)

    @BeforeEach
    fun setup() {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        val mapKey = randomName()
        redisMap = LettuceMap(connection, mapKey)
        evaluateCount.set(0)

        memoizer = redisMap.memoizer { key ->
            evaluateCount.incrementAndGet()
            "value-$key"
        }
    }

    @AfterEach
    fun teardown() {
        redisMap.clear()
    }

    // =========================================================================
    // LettuceMemorizer (동기)
    // =========================================================================

    @Test
    fun `동기 - 첫 번째 호출 시 evaluator 실행`() {
        val result = memoizer("key1")
        result shouldBeEqualTo "value-key1"
        evaluateCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `동기 - 두 번째 호출 시 캐시에서 반환`() {
        memoizer("key1")
        val result = memoizer("key1")
        result shouldBeEqualTo "value-key1"
        evaluateCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `동기 - 다른 키는 별도 evaluator 실행`() {
        memoizer("key1")
        memoizer("key2")
        evaluateCount.get() shouldBeEqualTo 2
    }

    @Test
    fun `동기 - clear 후 재평가`() {
        memoizer("key1")
        memoizer.clear()
        memoizer("key1")
        evaluateCount.get() shouldBeEqualTo 2
    }

    @Test
    fun `동기 - null 필드 조회 시 null 반환`() {
        val result = redisMap.get("nonexistent")
        result.shouldBeNull()
    }


}
