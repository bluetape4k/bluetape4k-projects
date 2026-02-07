package io.bluetape4k.vertx.sqlclient

import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.Tuple

suspend fun SqlClient.suspendQuery(sql: String): RowSet<Row> {
    return query(sql).execute().coAwait()
}

suspend inline fun <T> SqlClient.suspendQuery(
    sql: String,
    @BuilderInference mapper: (Row) -> T,
): List<T> {
    return suspendQuery(sql).map(mapper)
}

suspend fun SqlClient.suspendQuery(sql: String, params: Tuple): RowSet<Row> {
    return preparedQuery(sql).execute(params).coAwait()
}

suspend inline fun <T> SqlClient.suspendQuery(
    sql: String,
    params: Tuple,
    @BuilderInference mapper: (Row) -> T,
): List<T> {
    return suspendQuery(sql, params).map(mapper)
}
