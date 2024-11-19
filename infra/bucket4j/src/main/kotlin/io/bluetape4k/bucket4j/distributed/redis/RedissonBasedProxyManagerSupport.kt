package io.bluetape4k.bucket4j.distributed.redis

import io.github.bucket4j.distributed.serialization.Mapper
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
 * @param initializer ProxyManager 를 초기화하는 람다 함수
 * @receiver
 * @return [RedissonBasedProxyManager] 인스턴스
 */
inline fun redissonBasedProxyManagerOf(
    redisson: RedissonClient,
    initializer: RedissonBasedProxyManager.RedissonBasedProxyManagerBuilder<ByteArray>.() -> Unit,
): RedissonBasedProxyManager<ByteArray> {
    return redissonBasedProxyManagerOf((redisson as Redisson).commandExecutor, initializer)
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
 * @param initializer ProxyManager 를 초기화하는 람다 함수
 * @receiver
 * @return [RedissonBasedProxyManager] 인스턴스
 */
inline fun redissonBasedProxyManagerOf(
    commandAsyncExecutor: CommandAsyncExecutor,
    initializer: RedissonBasedProxyManager.RedissonBasedProxyManagerBuilder<ByteArray>.() -> Unit,
): RedissonBasedProxyManager<ByteArray> {
    return RedissonBasedProxyManager
        .builderFor(commandAsyncExecutor)
        .withKeyMapper(Mapper.BYTES)
        .apply(initializer)
        .build()
}
