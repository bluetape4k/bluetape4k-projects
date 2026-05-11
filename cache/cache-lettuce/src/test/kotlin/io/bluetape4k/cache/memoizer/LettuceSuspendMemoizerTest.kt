package io.bluetape4k.cache.memoizer

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.cache.RedisServers
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.codec.LettuceIntCodec
import io.bluetape4k.redis.lettuce.codec.LettuceLongCodec
import io.bluetape4k.redis.lettuce.map.LettuceSuspendMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Test
import org.testcontainers.utility.Base58
import java.util.concurrent.atomic.AtomicInteger

class LettuceSuspendMemoizerTest: AbstractSuspendMemoizerTest() {

    companion object: KLogging() {
        private val intConnection by lazy { LettuceClients.connect(RedisServers.redisClient, LettuceIntCodec) }
        private val longConnection by lazy { LettuceClients.connect(RedisServers.redisClient, LettuceLongCodec) }
    }

    private val heavyMap = LettuceSuspendMap<Int>(intConnection, "memoizer:lettuce:suspend:heavy")

    override val heavyFunc: suspend (Int) -> Int = heavyMap.suspendMemoizer { x ->
        delay(100)
        x * x
    }

    override val factorial: SuspendFactorialProvider = object: SuspendFactorialProvider {
        override val cachedCalc: suspend (Long) -> Long =
            LettuceSuspendMap<Long>(longConnection, "memoizer:lettuce:suspend:factorial")
                .suspendMemoizer { calc(it) }
    }

    override val fibonacci: SuspendFibonacciProvider = object: SuspendFibonacciProvider {
        override val cachedCalc: suspend (Long) -> Long =
            LettuceSuspendMap<Long>(longConnection, "memoizer:lettuce:suspend:fibonacci")
                .suspendMemoizer { calc(it) }
    }

    @Test
    fun `evaluator 실패 후 같은 key를 다시 호출하면 새 계산으로 복구된다`() = runSuspendIO {
        val map = LettuceSuspendMap<Int>(
            intConnection,
            "memoizer:lettuce:suspend:recovery:" + Base58.randomString(8)
        )
        map.clear()
        val evalCount = AtomicInteger(0)
        val memoizer = map.suspendMemoizer<Int, Int> { key ->
            if (evalCount.incrementAndGet() == 1) {
                error("transient failure")
            }
            key * key
        }

        assertFailsWith<IllegalStateException> {
            memoizer(7)
        }

        // 실패한 in-flight Deferred가 제거되어야 같은 key가 이전 실패에 고착되지 않는다.
        memoizer(7) shouldBeEqualTo 49
        memoizer(7) shouldBeEqualTo 49
        evalCount.get() shouldBeEqualTo 2
    }

    @Test
    fun `evaluator 취소 후 같은 key를 다시 호출하면 새 계산으로 복구된다`() = runSuspendIO {
        val map = LettuceSuspendMap<Int>(
            intConnection,
            "memoizer:lettuce:suspend:cancel:" + Base58.randomString(8)
        )
        map.clear()
        val evalCount = AtomicInteger(0)
        val memoizer = map.suspendMemoizer<Int, Int> { key ->
            if (evalCount.incrementAndGet() == 1) {
                throw CancellationException("test cancellation")
            }
            key * key
        }

        assertFailsWith<CancellationException> {
            memoizer(9)
        }

        // CancellationException도 성공 값처럼 저장하거나 in-flight에 남기지 않는다.
        memoizer(9) shouldBeEqualTo 81
        evalCount.get() shouldBeEqualTo 2
    }

    @Test
    fun `evaluator job 취소 후 같은 key를 다시 호출하면 새 계산으로 복구된다`() = runSuspendIO {
        val map = LettuceSuspendMap<Int>(
            intConnection,
            "memoizer:lettuce:suspend:job-cancel:" + Base58.randomString(8)
        )
        map.clear()
        val started = CompletableDeferred<Unit>()
        val evalCount = AtomicInteger(0)
        val memoizer = map.suspendMemoizer<Int, Int> { key ->
            if (evalCount.incrementAndGet() == 1) {
                started.complete(Unit)
                delay(Long.MAX_VALUE)
            }
            key * key
        }

        val job = launch { memoizer(11) }
        started.await()
        job.cancelAndJoin()

        // 실제 Job 취소 경로에서도 in-flight 항목이 정리되어 다음 호출이 새 계산으로 복구된다.
        memoizer(11) shouldBeEqualTo 121
        evalCount.get() shouldBeEqualTo 2
    }
}
