package io.bluetape4k.bucket4j.distributed.redis

import io.github.bucket4j.distributed.serialization.Mapper
import io.github.bucket4j.redis.redisson.Bucket4jRedisson
import io.github.bucket4j.redis.redisson.cas.RedissonBasedProxyManager
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.command.CommandAsyncExecutor

/**
 * Redisson 기반의 [io.github.bucket4j.distributed.proxy.ProxyManager] 를 생성합니다.
 *
 * ```
 * val redisson = Redisson.create()
 * val proxyManager = lettuceBasedProxyManagerOf(redisson) {
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
 * @param redisson [Redisson] instance
 * @param builder ProxyManager 를 초기화하는 람다 함수
 * @receiver
 * @return [RedissonBasedProxyManager] 인스턴스
 */
inline fun redissonBasedProxyManagerOf(
    redisson: RedissonClient,
    builder: Bucket4jRedisson.RedissonBasedProxyManagerBuilder<ByteArray>.() -> Unit,
): RedissonBasedProxyManager<ByteArray> {
    return redissonBasedProxyManagerOf(
        (redisson as Redisson).commandExecutor,
        builder
    )
}


/**
 * Redisson 기반의 [io.github.bucket4j.distributed.proxy.ProxyManager] 를 생성합니다.
 *
 * ```
 * val redisson = Redisson.create()
 * val proxyManager = lettuceBasedProxyManagerOf(redisson) {
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
 * @param commandAsyncExecutor Redisson의 [CommandAsyncExecutor] 인스턴스
 * @param builder ProxyManager 를 초기화하는 람다 함수
 * @receiver
 * @return [RedissonBasedProxyManager] 인스턴스
 */
inline fun redissonBasedProxyManagerOf(
    commandAsyncExecutor: CommandAsyncExecutor,
    builder: Bucket4jRedisson.RedissonBasedProxyManagerBuilder<ByteArray>.() -> Unit,
): RedissonBasedProxyManager<ByteArray> {
    return Bucket4jRedisson
        .RedissonBasedProxyManagerBuilder(Mapper.BYTES, commandAsyncExecutor)
        .apply(builder)
        .build()
}
