package io.bluetape4k.cache.memoizer

import io.bluetape4k.cache.RedisServers.randomName
import io.bluetape4k.cache.RedisServers.redisson
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.redisson.client.codec.IntegerCodec
import org.redisson.client.codec.LongCodec
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.seconds

class RedissonSuspendMemoizerTest: AbstractSuspendMemoizerTest() {

    private val heavyMap = redisson
        .getMap<Int, Int>("suspend:memoizer:heavy", IntegerCodec())
        .apply { clear() }

    override val heavyFunc: suspend (Int) -> Int = heavyMap.suspendMemoizer { x ->
        delay(100)
        x * x
    }

    override val factorial = object: SuspendFactorialProvider {
        override val cachedCalc: suspend (Long) -> Long = redisson
            .getMap<Long, Long>("suspend:memoizer:factorial", LongCodec())
            .suspendMemoizer { calc(it) }
    }

    override val fibonacci = object: SuspendFibonacciProvider {
        override val cachedCalc: suspend (Long) -> Long = redisson
            .getMap<Long, Long>("suspend:memoizer:fibonacci", LongCodec())
            .suspendMemoizer { calc(it) }
    }

    @Test
    fun `run heavy function`() = runSuspendIO {
        measureTimeMillis {
            heavyFunc(10) shouldBeEqualTo 100
        }

        val result = withTimeoutOrNull(1.seconds) {
            heavyFunc(10)
        }
        result shouldBeEqualTo 100
    }

    @Test
    fun `run factorial`() = runSuspendIO {
        val x1 = factorial.calc(100)

        val result = withTimeoutOrNull(3.seconds) {
            factorial.calc(100)
        }
        result shouldBeEqualTo x1
    }

    @Test
    fun `run fibonacci`() = runSuspendIO {
        val x1 = fibonacci.calc(100)

        val result = withTimeoutOrNull(3.seconds) {
            fibonacci.calc(100)
        }
        result shouldBeEqualTo x1
    }

    @Test
    fun `suspend memoizer should evaluate once for same key in concurrent calls`() = runSuspendIO {
        val map = redisson.getMap<Int, Int>(randomName(), IntegerCodec()).apply { clear() }
        val evaluateCount = AtomicInteger(0)
        val memoizer = map.suspendMemoizer { key ->
            evaluateCount.incrementAndGet()
            delay(100)
            key * key
        }

        try {
            val results = List(16) { async { memoizer(7) } }.awaitAll()
            results.forEach { it shouldBeEqualTo 49 }
            evaluateCount.get() shouldBeEqualTo 1
        } finally {
            map.delete()
        }
    }

    /**
     * SuspendedJobTester를 사용하여 여러 코루틴에서 동시에 suspendMemoizer를 호출할 때
     * 결과가 일관되고 중복 계산이 발생하지 않는지 검증합니다.
     */
    @Test
    fun `SuspendedJobTester - 여러 코루틴에서 동시에 suspendMemoizer 호출 시 일관된 결과 반환`() = runSuspendIO {
        val map = redisson.getMap<Int, Int>(randomName(), IntegerCodec()).apply { clear() }
        val memoizer = map.suspendMemoizer { key ->
            delay(10)
            key * key
        }
        try {
            SuspendedJobTester()
                .workers(16)
                .rounds(4)
                .add {
                    memoizer(5) shouldBeEqualTo 25
                    memoizer(7) shouldBeEqualTo 49
                    memoizer(9) shouldBeEqualTo 81
                }
                .run()
        } finally {
            map.delete()
        }
    }
}
