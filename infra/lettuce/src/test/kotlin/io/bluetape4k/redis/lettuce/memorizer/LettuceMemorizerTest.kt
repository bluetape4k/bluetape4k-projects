package io.bluetape4k.redis.lettuce.memorizer

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.map.LettuceMap
import io.bluetape4k.redis.lettuce.map.LettuceSuspendMap
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.atomic.AtomicInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LettuceMemorizerTest : AbstractLettuceTest() {

    private lateinit var redisMap: LettuceMap<String>
    private lateinit var redisSuspendMap: LettuceSuspendMap<String>
    private lateinit var memorizer: LettuceMemorizer
    private lateinit var asyncMemorizer: LettuceAsyncMemorizer
    private lateinit var suspendMemorizer: LettuceSuspendMemorizer

    private val evaluateCount = AtomicInteger(0)

    @BeforeEach
    fun setup() {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        val mapKey = randomName()
        redisMap = LettuceMap(connection, mapKey)
        redisSuspendMap = LettuceSuspendMap(connection, mapKey)
        evaluateCount.set(0)

        memorizer = redisMap.memorizer { key ->
            evaluateCount.incrementAndGet()
            "value-$key"
        }
        asyncMemorizer = redisMap.asyncMemorizer { key ->
            evaluateCount.incrementAndGet()
            "async-value-$key"
        }
        suspendMemorizer = redisSuspendMap.suspendMemorizer { key ->
            evaluateCount.incrementAndGet()
            "suspend-value-$key"
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
        val result = memorizer("key1")
        result shouldBeEqualTo "value-key1"
        evaluateCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `동기 - 두 번째 호출 시 캐시에서 반환`() {
        memorizer("key1")
        val result = memorizer("key1")
        result shouldBeEqualTo "value-key1"
        evaluateCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `동기 - 다른 키는 별도 evaluator 실행`() {
        memorizer("key1")
        memorizer("key2")
        evaluateCount.get() shouldBeEqualTo 2
    }

    @Test
    fun `동기 - clear 후 재평가`() {
        memorizer("key1")
        memorizer.clear()
        memorizer("key1")
        evaluateCount.get() shouldBeEqualTo 2
    }

    @Test
    fun `동기 - null 필드 조회 시 null 반환`() {
        val result = redisMap.get("nonexistent")
        result.shouldBeNull()
    }

    // =========================================================================
    // LettuceAsyncMemorizer (비동기)
    // =========================================================================

    @Test
    fun `비동기 - 첫 번째 호출 시 evaluator 실행`() {
        val result = asyncMemorizer("key1").get()
        result shouldBeEqualTo "async-value-key1"
        evaluateCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `비동기 - 두 번째 호출 시 캐시에서 반환`() {
        asyncMemorizer("key1").get()
        val result = asyncMemorizer("key1").get()
        result shouldBeEqualTo "async-value-key1"
        evaluateCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `비동기 - 다른 키는 별도 evaluator 실행`() {
        asyncMemorizer("key1").get()
        asyncMemorizer("key2").get()
        evaluateCount.get() shouldBeEqualTo 2
    }

    @Test
    fun `비동기 - clear 후 재평가`() {
        asyncMemorizer("key1").get()
        asyncMemorizer.clear()
        asyncMemorizer("key1").get()
        evaluateCount.get() shouldBeEqualTo 2
    }

    // =========================================================================
    // LettuceSuspendMemorizer (코루틴)
    // =========================================================================

    @Test
    fun `코루틴 - 첫 번째 호출 시 evaluator 실행`() = runSuspendIO {
        val result = suspendMemorizer("key1")
        result shouldBeEqualTo "suspend-value-key1"
        evaluateCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `코루틴 - 두 번째 호출 시 캐시에서 반환`() = runSuspendIO {
        suspendMemorizer("key1")
        val result = suspendMemorizer("key1")
        result shouldBeEqualTo "suspend-value-key1"
        evaluateCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `코루틴 - 다른 키는 별도 evaluator 실행`() = runSuspendIO {
        suspendMemorizer("key1")
        suspendMemorizer("key2")
        evaluateCount.get() shouldBeEqualTo 2
    }

    @Test
    fun `코루틴 - clear 후 재평가`() = runSuspendIO {
        suspendMemorizer("key1")
        suspendMemorizer.clear()
        suspendMemorizer("key1")
        evaluateCount.get() shouldBeEqualTo 2
    }

    @Test
    fun `코루틴 동시성 - 여러 코루틴에서 동시 호출 시 단일 평가`() = runSuspendIO {
        val concurrency = 10
        val results = List(concurrency) {
            async { suspendMemorizer("shared-key") }
        }.awaitAll()

        results.all { it == "suspend-value-shared-key" } shouldBeEqualTo true
        // evaluator는 한 번만 실행되어야 함 (캐싱 효과)
        // in-flight 로직이 없어도 Redis에 저장되면 이후 호출은 캐시에서 반환
        evaluateCount.get() shouldBeEqualTo 1
    }
}
