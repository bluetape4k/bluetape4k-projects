package io.bluetape4k.vertx.sqlclient

import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.Tuple

/**
 * SQL 문자열을 코루틴 환경에서 실행하고 [RowSet]을 반환합니다.
 *
 * @param sql 실행할 SQL 문장
 * @return 조회 결과 [RowSet]
 */
suspend fun SqlClient.suspendQuery(sql: String): RowSet<Row> {
    return query(sql).execute().coAwait()
}

/**
 * SQL 문자열을 실행하고 각 행을 [mapper]로 변환한 결과 목록을 반환합니다.
 *
 * @param sql 실행할 SQL 문장
 * @param mapper 조회된 [Row]를 도메인 객체로 변환하는 함수
 * @return 변환된 결과 목록
 */
suspend inline fun <T> SqlClient.suspendQuery(
    sql: String,
    mapper: (Row) -> T,
): List<T> {
    return suspendQuery(sql).map(mapper)
}

/**
 * 파라미터 바인딩 SQL을 코루틴 환경에서 실행하고 [RowSet]을 반환합니다.
 *
 * @param sql 실행할 SQL 문장
 * @param params 바인딩할 파라미터 [Tuple]
 * @return 조회 결과 [RowSet]
 */
suspend fun SqlClient.suspendQuery(sql: String, params: Tuple): RowSet<Row> {
    return preparedQuery(sql).execute(params).coAwait()
}

/**
 * 파라미터 바인딩 SQL을 실행하고 각 행을 [mapper]로 변환한 결과 목록을 반환합니다.
 *
 * @param sql 실행할 SQL 문장
 * @param params 바인딩할 파라미터 [Tuple]
 * @param mapper 조회된 [Row]를 도메인 객체로 변환하는 함수
 * @return 변환된 결과 목록
 */
suspend inline fun <T> SqlClient.suspendQuery(
    sql: String,
    params: Tuple,
    mapper: (Row) -> T,
): List<T> {
    return suspendQuery(sql, params).map(mapper)
}
