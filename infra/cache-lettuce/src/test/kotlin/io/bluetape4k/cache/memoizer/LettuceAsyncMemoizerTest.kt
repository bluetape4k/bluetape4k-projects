package io.bluetape4k.cache.memoizer

import io.bluetape4k.cache.RedisServers
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.codec.LettuceIntCodec
import io.bluetape4k.redis.lettuce.codec.LettuceLongCodec
import io.bluetape4k.redis.lettuce.map.LettuceMap
import io.lettuce.core.codec.RedisCodec
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

class LettuceAsyncMemoizerTest: AbstractAsyncMemoizerTest() {

    companion object: KLogging() {
        private fun <V: Any> newMap(
            codec: RedisCodec<String, V>,
            name: String,
        ): LettuceMap<V> = LettuceMap(
            LettuceClients.connect(RedisServers.redisClient, codec),
            name
        )
    }

    private val heavyMap = newMap(LettuceIntCodec, "memoizer:lettuce:async:heavy").apply { clear() }

    override val heavyFunc: (Int) -> CompletableFuture<Int> = heavyMap.asyncMemoizer { x ->
        CompletableFuture.supplyAsync {
            Thread.sleep(100)
            x * x
        }
    }

    override val factorial: AsyncFactorialProvider = object: AsyncFactorialProvider {
        override val cachedCalc: (Long) -> CompletableFuture<Long> =
            newMap(LettuceLongCodec, "memoizer:lettuce:async:factorial")
                .asyncMemoizer { calc(it) }
    }

    override val fibonacci: AsyncFibonacciProvider = object: AsyncFibonacciProvider {
        override val cachedCalc: (Long) -> CompletableFuture<Long> =
            newMap(LettuceLongCodec, "memoizer:lettuce:async:fibonacci")
                .asyncMemoizer { calc(it) }
    }

    /**
     * 재진입 레이스 회귀 테스트.
     *
     * `inFlight.remove(key)` 단독 호출은 완료된 promise 의 정리 중에 다른 스레드가 같은 key 에
     * 새 promise 를 심는 경우, 그 재진입 promise 까지 오삭제해 evaluator 가 중복 실행될 수 있었다.
     * 현재 구현은 `ConcurrentHashMap.remove(key, promise)` 로 key+value atomic 삭제를 보장한다.
     *
     * 이 테스트는 동일 key 에 대해 많은 쓰레드가 빠르게 `invoke(k)` 를 반복해도 evaluator 가
     * 과도하게 재실행되지 않음을 확인한다. 완벽한 1회 실행은 분산 환경이라 보장할 수 없으나,
     * 호출 횟수 대비 evaluator 실행은 극히 소수여야 한다 (Redis 캐시 + in-flight 공유 효과).
     */
    @Test
    fun `동일 key 재진입 시 inFlight remove 가 새 promise 를 오삭제하지 않음`() {
        val evalCount = AtomicInteger(0)
        val raceMap = newMap(LettuceIntCodec, "memoizer:lettuce:async:race").apply { clear() }
        val memoizer = raceMap.asyncMemoizer<Int, Int> { x ->
            evalCount.incrementAndGet()
            CompletableFuture.supplyAsync {
                Thread.sleep(30)
                x * x
            }
        }

        val workers = 16
        val rounds = 8
        val targetKey = 7

        MultithreadingTester()
            .workers(workers)
            .rounds(rounds)
            .add {
                memoizer(targetKey).get() shouldBeEqualTo 49
            }
            .run()

        // 호출 수 = workers * rounds. Redis miss 가 발생한 회차만 evaluator 실행.
        // inFlight 공유 + Redis 캐시 덕에 evaluator 실행 횟수는 전체 호출 수의 일부에 그쳐야 한다.
        // (레이스가 되살아나면 매 완료마다 중복 실행되어 급증함)
        val totalCalls = workers * rounds
        val evaluations = evalCount.get()
        log.debug { "totalCalls=$totalCalls, evaluations=$evaluations" }
        (evaluations <= workers) shouldBeEqualTo true
    }
}
