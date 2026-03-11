package io.bluetape4k.redis.redisson

import org.redisson.api.BatchOptions
import org.redisson.api.BatchResult
import org.redisson.api.RBatch
import org.redisson.api.RTransaction
import org.redisson.api.RedissonClient
import org.redisson.api.TransactionOptions

/**
 * Redisson Batch 명령을 DSL 방식으로 실행합니다.
 *
 * 여러 Redis 명령을 하나의 배치로 묶어 네트워크 왕복 횟수를 줄이고 처리량을 높입니다.
 * [action] 블록 내에서 [RBatch]의 메서드를 호출하면 각 명령이 큐에 쌓이고,
 * 블록 종료 후 한 번에 Redis 서버로 전송되어 실행됩니다.
 *
 * ```kotlin
 * val result = redissonClient.withBatch {
 *     getBucket<String>("key1").setAsync("value1")
 *     getBucket<String>("key2").setAsync("value2")
 *     getAtomicLong("counter").incrementAndGetAsync()
 * }
 * ```
 *
 * @param options 배치 실행 옵션 (응답 모드, 재시도 정책 등). 기본값: [BatchOptions.defaults]
 * @param action 배치에 추가할 Redis 명령을 정의하는 DSL 블록
 * @return 배치 실행 결과 ([BatchResult])
 * @see withTransaction
 */
inline fun RedissonClient.withBatch(
    options: BatchOptions = BatchOptions.defaults(),
    action: RBatch.() -> Unit,
): BatchResult<*> = createBatch(options).apply(action).execute()

/**
 * Redisson 트랜잭션을 DSL 방식으로 실행합니다.
 *
 * [action] 블록이 정상 완료되면 자동으로 [RTransaction.commit]을 호출하고,
 * 예외가 발생하면 [RTransaction.rollback]을 시도한 뒤 원래 예외를 재전파합니다.
 * rollback 자체가 실패하더라도 원래 예외가 전파됩니다.
 *
 * ```kotlin
 * redissonClient.withTransaction {
 *     getBucket<String>("key1").set("value1")
 *     getMap<String, Int>("map1").put("field", 42)
 *     // 블록 정상 종료 시 자동 commit
 * }
 * // 예외 발생 시 rollback 후 예외 재전파
 * ```
 *
 * > **주의**: Coroutine 환경에서는 스레드 전환으로 인해 트랜잭션이 깨질 수 있습니다.
 * > Coroutine 환경에서는 suspend 버전을 사용하세요.
 *
 * @param options 트랜잭션 옵션 (격리 수준, 타임아웃 등). 기본값: [TransactionOptions.defaults]
 * @param action 트랜잭션 내에서 실행할 Redis 명령 블록
 * @throws Throwable [action] 내에서 발생한 예외 (rollback 후 재전파)
 * @see withBatch
 */
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
