package io.bluetape4k.bucket4j.distributed.redis

import io.github.bucket4j.redis.lettuce.Bucket4jLettuce
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager
import io.lettuce.core.RedisClient
import io.lettuce.core.cluster.RedisClusterClient

/**
 * Lettuce 기반의 ProxyManager 를 생성합니다.
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
 * @receiver
 * @return
 */
inline fun lettuceBasedProxyManagerOf(
    redisClient: RedisClient,
    builder: Bucket4jLettuce.LettuceBasedProxyManagerBuilder<ByteArray>.() -> Unit,
): LettuceBasedProxyManager<ByteArray> {
    return Bucket4jLettuce
        .casBasedBuilder(redisClient)
        .apply(builder)
        .build()
}

/**
 * Lettuce 기반의 ProxyManager 를 생성합니다.
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
 * @receiver
 * @return
 */
inline fun lettuceBasedProxyManagerOf(
    redisClusterClient: RedisClusterClient,
    builder: Bucket4jLettuce.LettuceBasedProxyManagerBuilder<ByteArray>.() -> Unit,
): LettuceBasedProxyManager<ByteArray> {
    return Bucket4jLettuce
        .casBasedBuilder(redisClusterClient)
        .apply(builder)
        .build()
}
