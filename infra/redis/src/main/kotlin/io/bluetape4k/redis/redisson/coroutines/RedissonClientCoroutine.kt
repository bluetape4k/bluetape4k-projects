package io.bluetape4k.redis.redisson.coroutines

import io.bluetape4k.LibraryName
import io.bluetape4k.coroutines.support.awaitSuspending
import io.bluetape4k.support.requireNotBlank
import org.redisson.api.BatchOptions
import org.redisson.api.BatchResult
import org.redisson.api.RBatch
import org.redisson.api.RTransaction
import org.redisson.api.RedissonClient
import org.redisson.api.TransactionOptions
import org.redisson.transaction.TransactionException

/**
 * Redisson 작업을 Coroutines 환경에서 Batch 모드에서 실행하도록 합니다.
 */
suspend inline fun RedissonClient.withSuspendedBatch(
    options: BatchOptions = BatchOptions.defaults(),
    action: RBatch.() -> Unit,
): BatchResult<*> =
    createBatch(options).apply(action).executeAsync().awaitSuspending()

/**
 * Redisson 작업을 Coroutines 환경에서 Transaction model 에서 실행하도록 합니다.
 */
suspend inline fun RedissonClient.withSuspendedTransaction(
    options: TransactionOptions = TransactionOptions.defaults(),
    action: RTransaction.() -> Unit,
) {
    val tx: RTransaction = createTransaction(options)
    try {
        action(tx)
        tx.commitAsync().awaitSuspending()
    } catch (e: TransactionException) {
        runCatching { tx.rollbackAsync().awaitSuspending() }
        throw e
    }
}

private const val LOCK_ID_NAME_PREFIX = "$LibraryName:lock-id"

/**
 * Redisson은 Thread 기반의 Lock을 지원합니다.
 * Coroutines 환경에서 Lock을 사용하고자 한다면, Unique 한 Lock Id를 제공해야 합니다.
 * 만약 이때 Lock Id를 제공하지 않으면, 제대로 Unlock을 할 수 없습니다.
 *
 * ```
 * val lockId = redisson.getLockId("lock-name")
 * val lock = redisson.getLock(lockId)
 * lock.lock()
 * try {
 *    // do something
 * } finally {
 *   lock.unlock(lockId)
 * }
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
