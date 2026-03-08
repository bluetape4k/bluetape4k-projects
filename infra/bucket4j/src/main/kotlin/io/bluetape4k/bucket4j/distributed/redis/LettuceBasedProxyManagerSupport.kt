package io.bluetape4k.bucket4j.distributed.redis

import io.github.bucket4j.redis.lettuce.Bucket4jLettuce
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager
import io.lettuce.core.RedisClient
import io.lettuce.core.cluster.RedisClusterClient

/**
 * Lettuce 단일 노드 클라이언트로 Bucket4j CAS 기반 proxy manager를 생성합니다.
 *
 * ## 동작/계약
 * - 호출마다 새 [LettuceBasedProxyManager]를 생성합니다.
 * - [builder]에서 client-side config, expiration strategy, execution strategy를 설정할 수 있습니다.
 * - key 직렬화 타입은 `ByteArray`로 고정됩니다.
 *
 * ```
 * val redisClient = RedisClient.create("redis://localhost:6379")
 * val proxyManager = lettuceBasedProxyManagerOf(redisClient) {
 *    withClientSideConfig(
 *        ClientSideConfig.getDefault()
 *           .withExpirationAfterWriteStrategy(
 *               ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
 *                    90.seconds.toJavaDuration()
 *               )
 *           )
 *           .withExecutionStrategy(ExecutionStrategy.background(Executors.newVirtualThreadPerTaskExecutor()))
 *    )
 * }
 * ```
 *
 * @param redisClient Lettuce의 [io.lettuce.core.RedisClient] 인스턴스
 * @param builder ProxyManager 를 초기화하는 람다 함수
 * @return [LettuceBasedProxyManager] 인스턴스
 */
inline fun lettuceBasedProxyManagerOf(
    redisClient: RedisClient,
    @BuilderInference builder: Bucket4jLettuce.LettuceBasedProxyManagerBuilder<ByteArray>.() -> Unit,
): LettuceBasedProxyManager<ByteArray> {
    return Bucket4jLettuce
        .casBasedBuilder(redisClient)
        .apply(builder)
        .build()
}

/**
 * Lettuce cluster client로 Bucket4j CAS 기반 proxy manager를 생성합니다.
 *
 * ## 동작/계약
 * - 호출마다 새 [LettuceBasedProxyManager]를 생성합니다.
 * - [builder]에서 client-side config, expiration strategy, execution strategy를 설정할 수 있습니다.
 * - key 직렬화 타입은 `ByteArray`로 고정됩니다.
 *
 * ```
 * val redisClusterClient = RedisClusterClient.create("redis://localhost:6379")
 * val proxyManager = lettuceBasedProxyManagerOf(redisClient) {
 *    withClientSideConfig(
 *        ClientSideConfig.getDefault()
 *           .withExpirationAfterWriteStrategy(
 *               ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
 *                    90.seconds.toJavaDuration()
 *               )
 *           )
 *           .withExecutionStrategy(ExecutionStrategy.background(Executors.newVirtualThreadPerTaskExecutor()))
 *    )
 * }
 * ```
 *
 * @param redisClusterClient Lettuce의 [io.lettuce.core.cluster.RedisClusterClient] 인스턴스
 * @param builder ProxyManager 를 초기화하는 람다 함수
 * @return [LettuceBasedProxyManager] 인스턴스
 */
inline fun lettuceBasedProxyManagerOf(
    redisClusterClient: RedisClusterClient,
    @BuilderInference builder: Bucket4jLettuce.LettuceBasedProxyManagerBuilder<ByteArray>.() -> Unit,
): LettuceBasedProxyManager<ByteArray> {
    return Bucket4jLettuce
        .casBasedBuilder(redisClusterClient)
        .apply(builder)
        .build()
}
