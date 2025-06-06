package io.bluetape4k.vertx.sqlclient

import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.Tuple

suspend fun SqlClient.querySuspending(sql: String): RowSet<Row> {
    return query(sql).execute().coAwait()
}

suspend inline fun <T> SqlClient.querySuspending(
    sql: String,
    @BuilderInference mapper: (Row) -> T,
): List<T> {
    return querySuspending(sql).map(mapper)
}

suspend fun SqlClient.querySuspending(sql: String, params: Tuple): RowSet<Row> {
    return preparedQuery(sql).execute(params).coAwait()
}

suspend fun <T> SqlClient.querySuspending(
    sql: String,
    params: Tuple,
    @BuilderInference mapper: (Row) -> T,
): List<T> {
    return querySuspending(sql, params).map(mapper)
}
