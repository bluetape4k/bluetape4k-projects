package io.bluetape4k.redis.lettuce.memorizer

import io.bluetape4k.cache.memorizer.Memorizer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.lettuce.map.LettuceMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * [RedisMap]을 사용하는 동기 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val memorizer = redisMap.memorizer { key -> expensiveCompute(key) }
 * val result1 = memorizer("key1")
 * val result2 = memorizer("key1")  // 캐시된 값 반환
 * ```
 *
 * @receiver [RedisMap] 인스턴스
 * @param evaluator 키를 받아 값을 계산하는 함수
 * @return [LettuceMemorizer] 인스턴스
 */
@Deprecated(
    message = "memorizer()는 memoizer()로 이름이 변경되었습니다.",
    replaceWith = ReplaceWith("memoizer(evaluator)", "io.bluetape4k.redis.lettuce.memoizer.memoizer"),
    level = DeprecationLevel.WARNING
)
fun LettuceMap<String>.memorizer(evaluator: (String) -> String): LettuceMemorizer =
    LettuceMemorizer(this, evaluator)

/**
 * [RedisMap]을 사용하는 동기 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val memorizer = { key: String -> expensiveCompute(key) }.memorizer(redisMap)
 * val result = memorizer("key1")
 * ```
 *
 * @receiver 키를 받아 값을 계산하는 함수
 * @param map [RedisMap] 인스턴스
 * @return [LettuceMemorizer] 인스턴스
 */
@Deprecated(
    message = "memorizer()는 memoizer()로 이름이 변경되었습니다.",
    replaceWith = ReplaceWith("memoizer(map)", "io.bluetape4k.redis.lettuce.memoizer.memoizer"),
    level = DeprecationLevel.WARNING
)
fun ((String) -> String).memorizer(map: LettuceMap<String>): LettuceMemorizer =
    LettuceMemorizer(map, this)

/**
 * [RedisMap]을 사용하여 함수 실행 결과를 메모이제이션하는 동기 구현체입니다.
 *
 * 동일 JVM에서 동시 요청 시 in-flight 연산을 공유하여 중복 실행을 방지합니다.
 *
 * ```kotlin
 * val memorizer = LettuceMemorizer(redisMap) { key -> expensiveCompute(key) }
 * val result1 = memorizer("key1")  // 계산 후 Redis에 저장
 * val result2 = memorizer("key1")  // Redis에서 반환
 * ```
 *
 * @property map       [RedisMap] 인스턴스
 * @property evaluator 키를 받아 값을 계산하는 함수
 */
@Deprecated(
    message = "LettuceMemorizer는 LettuceMemoizer로 이름이 변경되었습니다.",
    replaceWith = ReplaceWith("LettuceMemoizer", "io.bluetape4k.redis.lettuce.memoizer.LettuceMemoizer"),
    level = DeprecationLevel.WARNING
)
class LettuceMemorizer(
    val map: LettuceMap<String>,
    val evaluator: (String) -> String,
) : Memorizer<String, String> {

    companion object : KLogging()

    private val inFlight = ConcurrentHashMap<String, CompletableFuture<String>>()

    override fun invoke(key: String): String {
        inFlight[key]?.let { return it.join() }

        val promise = CompletableFuture<String>()
        val existing = inFlight.putIfAbsent(key, promise)
        if (existing != null) return existing.join()

        try {
            val cached = map.get(key)
            if (cached != null) {
                promise.complete(cached)
                return cached
            }

            val evaluated = evaluator(key)
            val isNew = map.putIfAbsent(key, evaluated)
            val winner = if (isNew) evaluated else (map.get(key) ?: evaluated)
            promise.complete(winner)
            return winner
        } catch (e: Throwable) {
            promise.completeExceptionally(e)
            throw e
        } finally {
            inFlight.remove(key, promise)
        }
    }

    override fun clear() {
        log.debug { "모든 메모이제이션 값 삭제: mapKey=${map.mapKey}" }
        map.clear()
    }
}
