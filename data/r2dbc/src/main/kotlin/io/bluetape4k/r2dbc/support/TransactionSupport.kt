package io.bluetape4k.r2dbc.support

import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.transaction.ReactiveTransaction
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait

/**
 * [R2dbcTransactionManager]가 생성한 [ReactiveTransaction] 환경 하에서 실행합니다.
 *
 * @param T
 * @param transactionDefinition Transaction 설정 정보
 * @param block [ReactiveTransaction] 환경에서 실행할 블럭
 * @return 실행 결과
 */
suspend inline fun <T: Any> DatabaseClient.withTransactionSuspend(
    transactionDefinition: TransactionDefinition = TransactionDefinition.withDefaults(),
    crossinline block: suspend (tx: ReactiveTransaction) -> T?,
): T? {
    val tm = R2dbcTransactionManager(this.connectionFactory)

    return TransactionalOperator
        .create(tm, transactionDefinition)
        .executeAndAwait {
            block(it)
        }
}

@Deprecated("use withTransactionSuspend", replaceWith = ReplaceWith("withTransactionSuspend(block)"))
suspend inline fun <T: Any> DatabaseClient.withTransactionSuspending(
    transactionDefinition: TransactionDefinition = TransactionDefinition.withDefaults(),
    crossinline block: suspend (tx: ReactiveTransaction) -> T?,
): T? {
    val tm = R2dbcTransactionManager(this.connectionFactory)

    return TransactionalOperator
        .create(tm, transactionDefinition)
        .executeAndAwait {
            block(it)
        }
}
