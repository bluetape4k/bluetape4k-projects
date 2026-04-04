package io.bluetape4k.redis.redisson.coroutines

import io.bluetape4k.LibraryName
import io.bluetape4k.support.requireNotBlank
import kotlinx.coroutines.future.await
import org.redisson.api.BatchOptions
import org.redisson.api.BatchResult
import org.redisson.api.RBatch
import org.redisson.api.RTransaction
import org.redisson.api.RedissonClient
import org.redisson.api.TransactionOptions

/**
 * Redisson 작업을 코루틴 환경에서 Batch 모드로 실행합니다.
 *
 * [action] 블록 내에서 [RBatch]를 통해 여러 Redis 커맨드를 하나의 배치로 묶어 실행합니다.
 * 배치 실행은 네트워크 왕복을 줄여 성능을 크게 향상시킵니다.
 *
 * ## 사용 예
 * ```kotlin
 * val result = redisson.withSuspendedBatch {
 *     mapAsync.putAsync("key1", "value1")
 *     mapAsync.putAsync("key2", "value2")
 * }
 * ```
 *
 * @param options 배치 실행 옵션 (기본값: [BatchOptions.defaults])
 * @param action [RBatch] 수신 객체로 Redis 커맨드를 등록하는 블록
 * @return 배치 실행 결과 ([BatchResult])
 */
suspend inline fun RedissonClient.withSuspendedBatch(
    options: BatchOptions = BatchOptions.defaults(),
    action: RBatch.() -> Unit,
): BatchResult<*> =
    createBatch(options).apply(action).executeAsync().await()

/**
 * Redisson 작업을 코루틴 환경에서 트랜잭션 모드로 실행합니다.
 *
 * [action] 블록이 정상적으로 완료되면 트랜잭션을 커밋하고,
 * 예외가 발생하면 롤백을 시도한 후 원래 예외를 다시 던집니다.
 *
 * ## 주의사항
 * - 롤백 실패 시 롤백 예외는 무시되고 원래 예외만 전파됩니다 ([runCatching] 처리).
 * - Redisson 트랜잭션은 낙관적 잠금 방식이므로 충돌 시 재시도 로직이 필요할 수 있습니다.
 *
 * ## 사용 예
 * ```kotlin
 * redisson.withSuspendedTransaction {
 *     val map = getMap<String, String>("myMap")
 *     map.putAsync("key", "value")
 * }
 * ```
 *
 * @param options 트랜잭션 옵션 (기본값: [TransactionOptions.defaults])
 * @param action [RTransaction] 수신 객체로 Redis 커맨드를 등록하는 블록
 * @throws Throwable [action] 블록에서 발생한 예외
 */
suspend inline fun RedissonClient.withSuspendedTransaction(
    options: TransactionOptions = TransactionOptions.defaults(),
    action: RTransaction.() -> Unit,
) {
    val tx: RTransaction = createTransaction(options)
    try {
        action(tx)
        tx.commitAsync().await()
    } catch (e: Throwable) {
        runCatching { tx.rollbackAsync().await() }
        throw e
    }
}

private const val LOCK_ID_NAME_PREFIX = "$LibraryName:lock-id"

/**
 * Redisson은 Thread 기반의 Lock을 지원합니다.
 * Coroutines 환경에서 Lock을 사용하고자 한다면, Unique 한 Lock Id를 제공해야 합니다.
 * 만약 이때 Lock Id를 제공하지 않으면, 제대로 Unlock을 할 수 없습니다.
 *
 * ```kotlin
 * val lockId = redisson.getLockId("lock-name")
 * val lock = redisson.getLock("lock-name")
 * val acquired = lock.tryLockAsync(3000L, 10000L, TimeUnit.MILLISECONDS, lockId).await()
 * try {
 *     // 리더 작업 수행
 * } finally {
 *     if (lock.isHeldByThread(lockId)) {
 *         lock.unlockAsync(lockId).await()
 *     }
 * }
 * // lockId > 0
 * ```
 *
 * @param lockName Redisson Lock 이름
 * @return Lock 을 구분하기 위한 Identifier
 */
fun RedissonClient.getLockId(lockName: String): Long {
    lockName.requireNotBlank("lockName")

    val sequenceName = "$LOCK_ID_NAME_PREFIX:$lockName"

    val atomicLong = getAtomicLong(sequenceName)
    atomicLong.compareAndSet(0, System.currentTimeMillis())
    return atomicLong.andIncrement
}
