package io.bluetape4k.bucket4j.distributed.redis

import io.github.bucket4j.distributed.serialization.Mapper
import io.github.bucket4j.redis.redisson.Bucket4jRedisson
import io.github.bucket4j.redis.redisson.cas.RedissonBasedProxyManager
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.command.CommandAsyncExecutor

/**
 * [RedissonClient]로 Bucket4j CAS 기반 proxy manager를 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [Redisson] 구현체의 [CommandAsyncExecutor]를 추출해 사용합니다.
 * - 호출마다 새 [RedissonBasedProxyManager]를 생성합니다.
 * - key 직렬화 타입은 `ByteArray`로 고정됩니다.
 *
 * ```
 * val redisson = Redisson.create()
 * val proxyManager = redissonBasedProxyManagerOf(redisson) {
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
 * @param redisson [RedissonClient] 인스턴스. 내부 구현이 [Redisson]이어야 합니다.
 * @param builder ProxyManager 를 초기화하는 람다 함수
 * @return [RedissonBasedProxyManager] 인스턴스
 */
inline fun redissonBasedProxyManagerOf(
    redisson: RedissonClient,
    @BuilderInference builder: Bucket4jRedisson.RedissonBasedProxyManagerBuilder<ByteArray>.() -> Unit,
): RedissonBasedProxyManager<ByteArray> {
    val executor = (redisson as? Redisson)?.commandExecutor
        ?: throw IllegalArgumentException(
            "redisson must be an instance of org.redisson.Redisson to extract CommandAsyncExecutor. actual=${redisson::class.qualifiedName}"
        )

    return redissonBasedProxyManagerOf(
        executor,
        builder
    )
}


/**
 * [CommandAsyncExecutor]로 Bucket4j CAS 기반 proxy manager를 생성합니다.
 *
 * ## 동작/계약
 * - 이미 준비된 executor를 직접 주입해 Redisson lifecycle과 분리할 수 있습니다.
 * - 호출마다 새 [RedissonBasedProxyManager]를 생성합니다.
 * - key 직렬화 타입은 `ByteArray`로 고정됩니다.
 *
 * ```
 * val redisson = Redisson.create()
 * val proxyManager = redissonBasedProxyManagerOf(redisson) {
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
 * @return [RedissonBasedProxyManager] 인스턴스
 */
inline fun redissonBasedProxyManagerOf(
    commandAsyncExecutor: CommandAsyncExecutor,
    @BuilderInference builder: Bucket4jRedisson.RedissonBasedProxyManagerBuilder<ByteArray>.() -> Unit,
): RedissonBasedProxyManager<ByteArray> {
    return Bucket4jRedisson
        .RedissonBasedProxyManagerBuilder(Mapper.BYTES, commandAsyncExecutor)
        .apply(builder)
        .build()
}
