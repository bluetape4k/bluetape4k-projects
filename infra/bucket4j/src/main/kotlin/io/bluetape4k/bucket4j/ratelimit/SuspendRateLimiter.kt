package io.bluetape4k.bucket4j.ratelimit

/**
 * Coroutine 환경에서의 Rate Limiter 인터페이스
 *
 * [consume]은 토큰을 즉시 소비 시도하며, 부족한 경우 대기하지 않고 거절 결과를 반환한다.
 *
 * @param K  Key 수형
 */
interface SuspendRateLimiter<K> {

    /**
     * [key] 기준으로 [numToken] 갯수만큼 즉시 소비 시도합니다. 결과는 [RateLimitResult]로 반환됩니다.
     *
     * @param key      Rate Limit 적용 대상 Key
     * @param numToken 소비할 토큰 수
     * @return [RateLimitResult] 토큰 소비 결과
     *
     * @throws kotlinx.coroutines.CancellationException 코루틴 취소 시 그대로 전파
     */
    suspend fun consume(key: K, numToken: Long = 1): RateLimitResult

}
