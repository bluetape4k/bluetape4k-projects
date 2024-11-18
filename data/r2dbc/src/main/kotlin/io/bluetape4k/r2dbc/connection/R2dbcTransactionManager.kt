package io.bluetape4k.r2dbc.connection

import io.r2dbc.spi.ConnectionFactory
import org.springframework.r2dbc.connection.R2dbcTransactionManager

/**
 * [connectionFactory]를 사용하는 [R2dbcTransactionManager]를 생성합니다.
 *
 * @param connectionFactory 트랜잭션을 관리할 [ConnectionFactory]
 * @return [R2dbcTransactionManager] 인스턴스
 */
fun r2dbcTransactionManagerOf(connectionFactory: ConnectionFactory): R2dbcTransactionManager =
    R2dbcTransactionManager(connectionFactory)
