package io.bluetape4k.vertx.sqlclient

import io.vertx.jdbcclient.JDBCPool
import io.vertx.mysqlclient.MySQLConnection
import io.vertx.sqlclient.PropertyKind
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.SqlResult

/**
 * 방금 실행된 Insert 결과에서 자동 증가 식별자 값을 가져옵니다.
 *
 * MySQL은 `last-inserted-id` 속성에서 값을 조회하고, 그 외 구현체(JDBC 등)는
 * `GENERATED_KEYS`에서 [columnName] 키를 조회합니다.
 *
 * @param ID 식별자 타입
 * @param client 현재 결과를 생성한 SQL 클라이언트 연결
 * @param columnName 생성 키 컬럼명 (기본값: `id`)
 * @return 식별자 값, 없으면 `null`
 */
inline fun <reified ID: Number> SqlResult<*>.getGeneratedId(
    client: SqlConnection,
    columnName: String = "id",
): ID? {
    return when (client) {
        is MySQLConnection -> {
            val lastInsertedId = PropertyKind.create("last-inserted-id", ID::class.java)
            this.property(lastInsertedId)
        }

        else ->
            this.property(JDBCPool.GENERATED_KEYS).getValue(columnName) as? ID
    }
}
