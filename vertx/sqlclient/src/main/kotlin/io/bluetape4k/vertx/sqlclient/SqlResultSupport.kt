package io.bluetape4k.vertx.sqlclient

import io.vertx.jdbcclient.JDBCPool
import io.vertx.mysqlclient.MySQLConnection
import io.vertx.sqlclient.PropertyKind
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.SqlResult

/**
 * 최신의 Auto Increment Identifier 값을 가져온다
 *
 * @param ID         identifier type
 * @param client     sql client
 * @param columnName identifier column name
 * @return identifier value or null
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
