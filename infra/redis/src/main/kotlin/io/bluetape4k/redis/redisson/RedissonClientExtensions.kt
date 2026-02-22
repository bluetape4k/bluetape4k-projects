package io.bluetape4k.redis.redisson

import org.redisson.api.BatchOptions
import org.redisson.api.BatchResult
import org.redisson.api.RBatch
import org.redisson.api.RTransaction
import org.redisson.api.RedissonClient
import org.redisson.api.TransactionOptions

inline fun RedissonClient.withBatch(
    options: BatchOptions = BatchOptions.defaults(),
    action: RBatch.() -> Unit,
): BatchResult<*> = createBatch(options).apply(action).execute()

inline fun RedissonClient.withTransaction(
    options: TransactionOptions = TransactionOptions.defaults(),
    action: RTransaction.() -> Unit,
) {
    val tx = createTransaction(options)

    try {
        action(tx)
        tx.commit()
    } catch (e: Throwable) {
        runCatching { tx.rollback() }
        throw e
    }
}
