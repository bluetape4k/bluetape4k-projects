package io.bluetape4k.redis.lettuce.memoizer

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.map.LettuceSuspendMap
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class LettuceSuspendMemoizerTest: AbstractLettuceTest() {

    companion object: KLoggingChannel()

    private lateinit var redisSuspendMap: LettuceSuspendMap<String>
    private lateinit var suspendMemoizer: LettuceSuspendMemoizer

    private val evaluateCount = AtomicInteger(0)

    @BeforeEach
    fun setup() {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        val mapKey = randomName()

        redisSuspendMap = LettuceSuspendMap(connection, mapKey)
        evaluateCount.set(0)

        suspendMemoizer = redisSuspendMap.suspendMemoizer { key ->
            evaluateCount.incrementAndGet()
            "suspend-value-$key"
        }
    }

    @AfterEach
    fun teardown() = runSuspendIO {
        redisSuspendMap.clear()
    }

    @Test
    fun `코루틴 - 첫 번째 호출 시 evaluator 실행`() = runSuspendIO {
        val result = suspendMemoizer("key1")
        result shouldBeEqualTo "suspend-value-key1"
        evaluateCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `코루틴 - 두 번째 호출 시 캐시에서 반환`() = runSuspendIO {
        suspendMemoizer("key1")
        val result = suspendMemoizer("key1")
        result shouldBeEqualTo "suspend-value-key1"
        evaluateCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `코루틴 - 다른 키는 별도 evaluator 실행`() = runSuspendIO {
        suspendMemoizer("key1")
        suspendMemoizer("key2")
        evaluateCount.get() shouldBeEqualTo 2
    }

    @Test
    fun `코루틴 - clear 후 재평가`() = runSuspendIO {
        suspendMemoizer("key1")
        suspendMemoizer.clear()
        suspendMemoizer("key1")
        evaluateCount.get() shouldBeEqualTo 2
    }

    @Test
    fun `코루틴 동시성 - 여러 코루틴에서 동시 호출 시 단일 평가`() = runSuspendIO {
        val concurrency = 10
        val results = List(concurrency) {
            async { suspendMemoizer("shared-key") }
        }.awaitAll()

        results.all { it == "suspend-value-shared-key" } shouldBeEqualTo true
        // evaluator는 한 번만 실행되어야 함 (캐싱 효과)
        // in-flight 로직이 없어도 Redis에 저장되면 이후 호출은 캐시에서 반환
        evaluateCount.get() shouldBeEqualTo 1
    }
}
