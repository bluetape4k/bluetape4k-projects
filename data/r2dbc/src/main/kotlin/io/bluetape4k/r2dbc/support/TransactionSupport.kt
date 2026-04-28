package io.bluetape4k.r2dbc.support

import io.r2dbc.spi.ConnectionFactory
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.transaction.ReactiveTransaction
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import java.util.WeakHashMap

/**
 * [ConnectionFactory] 별 [R2dbcTransactionManager] 캐시.
 *
 * 동일한 [ConnectionFactory]에 대해 매 호출마다 새로운 [R2dbcTransactionManager]를 생성하지 않도록
 * [WeakHashMap]으로 캐싱합니다. [WeakHashMap]을 사용하므로 [ConnectionFactory]가 GC 대상이 되면
 * 자동으로 캐시에서 제거됩니다.
 */
internal val transactionManagerCache = WeakHashMap<ConnectionFactory, R2dbcTransactionManager>()

/**
 * [ConnectionFactory]에 대응하는 [R2dbcTransactionManager]를 캐시에서 조회하거나 새로 생성합니다.
 */
@PublishedApi
internal fun ConnectionFactory.getOrCreateTransactionManager(): R2dbcTransactionManager =
    synchronized(transactionManagerCache) {
        transactionManagerCache.getOrPut(this) { R2dbcTransactionManager(this) }
    }

/**
 * R2DBC 트랜잭션 환경 하에서 suspend 함수를 실행합니다.
 *
 * 트랜잭션 내에서 예외가 발생하면 자동으로 롤백되고, 성공하면 자동으로 커밋됩니다.
 *
 * ```kotlin
 * val result = databaseClient.withTransactionSuspend { tx ->
 *     // 첫 번째 INSERT
 *     databaseClient
 *         .sql("INSERT INTO accounts (user_id, balance) VALUES (:userId, :balance)")
 *         .bind("userId", 1)
 *         .bind("balance", 1000)
 *         .fetch()
 *         .awaitRowsUpdated()
 *
 *     // 두 번째 INSERT
 *     databaseClient
 *         .sql("INSERT INTO logs (message) VALUES (:message)")
 *         .bind("message", "Account created")
 *         .fetch()
 *         .awaitRowsUpdated()
 *
 *     "success" // 트랜잭션 결과 반환
 * }
 * ```
 *
 * @param T 반환 타입
 * @param transactionDefinition 트랜잭션 설정 (격리 수준, 전파 등)
 * @param block 트랜잭션 내에서 실행할 suspend 함수 블록
 * @return 블록의 실행 결과
 */
suspend inline fun <T: Any> DatabaseClient.withTransactionSuspend(
    transactionDefinition: TransactionDefinition = TransactionDefinition.withDefaults(),
    crossinline block: suspend (tx: ReactiveTransaction) -> T?,
): T? {
    val tm = this.connectionFactory.getOrCreateTransactionManager()

    return TransactionalOperator
        .create(tm, transactionDefinition)
        .executeAndAwait {
            block(it)
        }
}

/**
 * @deprecated `withTransactionSuspend` 을 대신 사용하세요.
 * 내부적으로 [withTransactionSuspend] 에 위임합니다.
 */
@Deprecated("use withTransactionSuspend", replaceWith = ReplaceWith("withTransactionSuspend(block)"))
suspend inline fun <T: Any> DatabaseClient.withTransactionSuspending(
    transactionDefinition: TransactionDefinition = TransactionDefinition.withDefaults(),
    crossinline block: suspend (tx: ReactiveTransaction) -> T?,
): T? = withTransactionSuspend(transactionDefinition, block)
