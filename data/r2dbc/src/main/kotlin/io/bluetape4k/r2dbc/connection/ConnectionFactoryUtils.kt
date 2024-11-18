package io.bluetape4k.r2dbc.connection

import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.R2dbcException
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.dao.DataAccessException
import org.springframework.r2dbc.connection.ConnectionFactoryUtils
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * Obtain a [Connection] from the given [ConnectionFactory].
 * [ConnectionFactory]로부터 [Connection]을 얻는다.
 *
 * 좀 더 의미있는 예외를 던지고 호출 코드를 단순화하기 위해 스프링의 일반적인 데이터 접근 예외 계층 구조로 예외를 변환한다.
 *
 * 현 [TransactionSynchronizationManager]에 바인딩된 해당 Connection을 인식한다.
 * 트랜잭션 동기화가 활성화되어 있으면 Connection을 [TransactionSynchronizationManager]에 바인딩한다.
 *
 * @receiver [Connection]을 제공하는 [ConnectionFactory]
 * @return [ConnectionFactory]로부터 얻은 R2DBC [Connection]
 * @throws [Connection]을 얻는데 실패하면 [org.springframework.dao.DataAccessResourceFailureException] 예외를 던진다.
 *
 * @see #releaseConnection
 */
suspend fun ConnectionFactory.getConnectionAndAwait(): Connection? =
    ConnectionFactoryUtils.getConnection(this).awaitFirstOrNull()

/**
 * [ConnectionFactory]로부터 R2DBC [Connection]을 실제로 얻는다. [getConnectionAndAwait]과 동일하지만 원래 예외를 보존한다.
 *
 * <p>Is aware of a corresponding Connection bound to the current
 * [org.springframework.transaction.support.TransactionSynchronizationManager].
 *
 * 현 [org.springframework.transaction.support.TransactionSynchronizationManager]에 바인딩된 해당 Connection을 인식한다.
 * 만약 트랜잭션 동기화가 활성화되어 있으면 Connection을 [org.springframework.transaction.support.TransactionSynchronizationManager]에 바인딩한다.
 *
 * @receiver [Connection]을 얻을 [ConnectionFactory]
 * @return [ConnectionFactory]로 부터 얻은 R2DBC [Connection]
 */
suspend fun ConnectionFactory.doGetConnectionAndAwait(): Connection? =
    ConnectionFactoryUtils.doGetConnection(this).awaitFirstOrNull()

/**
 * 주어진 [ConnectionFactory]에서 [Connection]을 가져온다.
 *
 * @receiver [Connection]을 제공하는 [ConnectionFactory]
 * @return R2DBC [Connection]
 * @throws [ConnectionFactory]가 Connection을 생성하지 못한 경우 [IllegalStateException]을 던집니다.
 *
 * @see [getConnectionAndAwait]
 * @see [doGetConnectionAndAwait]
 */
suspend fun ConnectionFactory.fetchConnectionAndAwait(): Connection =
    create().awaitFirst()

/**
 * 만약 외부에서 관리되지 않은 [Connection]이라면(즉, 구독에 바인딩되지 않은 경우)
 * 주어진 [ConnectionFactory]에서 얻은 [Connection]을 닫는다.
 *
 * @receiver [Connection]을 얻은 [ConnectionFactory]
 * @param conn 닫을 [Connection]
 *
 * @see [doReleaseConnectionAndAwait]
 * @see [getConnectionAndAwait]
 */
suspend fun ConnectionFactory.releaseConnectionAndAwait(conn: Connection) {
    ConnectionFactoryUtils.releaseConnection(conn, this).awaitFirstOrNull()
}

/**
 * 주어진 [ConnectionFactory]에서 얻은 [Connection]을 실제로 닫는다.
 * [releaseConnectionAndAwait]와 동일하지만 원래 예외를 보존한다.
 *
 * @receiver [Connection]을 얻은 [ConnectionFactory]
 * @param conn 닫을 [Connection]
 *
 * @see [releaseConnectionAndAwait]
 * @see [doGetConnectionAndAwait]
 */
suspend fun ConnectionFactory.doReleaseConnectionAndAwait(conn: Connection) {
    ConnectionFactoryUtils.doReleaseConnection(conn, this).awaitFirstOrNull()
}

/**
 * [TransactionSynchronizationManager]에 바인딩된 현재 [ConnectionFactory]를 얻는다.
 */
suspend fun ConnectionFactory.currentAndAwait(): ConnectionFactory {
    return ConnectionFactoryUtils.currentConnectionFactory(this).awaitSingle()
}

/**
 * [R2dbcException]을 [DataAccessException]로 변환한다.
 */
fun R2dbcException.convert(task: String, sql: String? = null): DataAccessException {
    return ConnectionFactoryUtils.convertR2dbcException(task, sql, this)
}

/**
 * 주어진 [Connection]의 가장 안쪽 타겟 [Connection]을 반환한다.
 */
fun Connection.getTargetConnection(): Connection =
    ConnectionFactoryUtils.getTargetConnection(this)
