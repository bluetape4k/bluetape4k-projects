package io.bluetape4k.redis.lettuce.memorizer

import io.bluetape4k.cache.memorizer.SuspendMemorizer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.lettuce.map.LettuceSuspendMap
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap

/**
 * [RedisMap]을 사용하는 코루틴 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val memorizer = redisMap.suspendMemorizer { key -> expensiveComputeSuspend(key) }
 * val result = memorizer("key1")
 * ```
 *
 * @receiver [RedisMap] 인스턴스
 * @param evaluator 키를 받아 값을 계산하는 suspend 함수
 * @return [LettuceSuspendMemorizer] 인스턴스
 */
@Deprecated(
    message = "suspendMemorizer()는 suspendMemoizer()로 이름이 변경되었습니다.",
    replaceWith = ReplaceWith("suspendMemoizer(evaluator)", "io.bluetape4k.redis.lettuce.memoizer.suspendMemoizer"),
    level = DeprecationLevel.WARNING
)
fun LettuceSuspendMap<String>.suspendMemorizer(evaluator: suspend (String) -> String): LettuceSuspendMemorizer =
    LettuceSuspendMemorizer(this, evaluator)

/**
 * [RedisMap]을 사용하여 함수 실행 결과를 코루틴 기반으로 메모이제이션하는 구현체입니다.
 *
 * 동일 JVM에서 동시 요청 시 in-flight [CompletableDeferred]를 공유하여 중복 실행을 방지합니다.
 *
 * ```kotlin
 * val memorizer = LettuceSuspendMemorizer(redisMap) { key -> expensiveComputeSuspend(key) }
 * val result1 = memorizer("key1")  // 계산 후 Redis에 저장
 * val result2 = memorizer("key1")  // Redis에서 반환
 * ```
 *
 * @property map       [RedisMap] 인스턴스
 * @property evaluator 키를 받아 값을 계산하는 suspend 함수
 */
@Deprecated(
    message = "LettuceSuspendMemorizer는 LettuceSuspendMemoizer로 이름이 변경되었습니다.",
    replaceWith = ReplaceWith("LettuceSuspendMemoizer", "io.bluetape4k.redis.lettuce.memoizer.LettuceSuspendMemoizer"),
    level = DeprecationLevel.WARNING
)
class LettuceSuspendMemorizer(
    val map: LettuceSuspendMap<String>,
    val evaluator: suspend (String) -> String,
) : SuspendMemorizer<String, String> {

    companion object : KLogging()

    private val inFlight = ConcurrentHashMap<String, CompletableDeferred<String>>()

    override suspend fun invoke(key: String): String {
        inFlight[key]?.let { return it.await() }

        val deferred = CompletableDeferred<String>()
        val existing = inFlight.putIfAbsent(key, deferred)
        if (existing != null) return existing.await()

        try {
            val cached = map.get(key)
            if (cached != null) {
                deferred.complete(cached)
                return cached
            }

            val evaluated = evaluator(key)
            val isNew = map.putIfAbsent(key, evaluated)
            val winner = if (isNew) evaluated else (map.get(key) ?: evaluated)
            deferred.complete(winner)
            return winner
        } catch (e: Throwable) {
            deferred.completeExceptionally(e)
            throw e
        } finally {
            inFlight.remove(key, deferred)
        }
    }

    override suspend fun clear() {
        log.debug { "모든 메모이제이션 값 삭제: mapKey=${map.mapKey}" }
        map.clear()
    }
}
