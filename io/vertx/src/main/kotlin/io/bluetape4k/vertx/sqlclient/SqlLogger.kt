package io.bluetape4k.vertx.sqlclient

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug

/**
 * SQL 구문을 DEBUG 레벨로 로그에 출력합니다.
 *
 * ```kotlin
 * SqlLogger.logSql("SELECT * FROM users WHERE id = #{id}", mapOf("id" to 1))
 * // [DEBUG] SQL: SELECT * FROM users WHERE id = #{id}
 * // [DEBUG] PARAMS: {id=1}
 * ```
 */
object SqlLogger: KLoggingChannel() {

    /**
     * SQL 문장과 파라미터 맵을 DEBUG 레벨로 로그에 출력합니다.
     *
     * ```kotlin
     * SqlLogger.logSql("SELECT * FROM users", emptyMap())
     * // [DEBUG] SQL: SELECT * FROM users
     * ```
     *
     * @param sql 로그에 출력할 SQL 문장
     * @param params SQL 파라미터 맵 (비어있으면 출력하지 않음)
     */
    fun logSql(sql: String, params: Map<String, Any?> = emptyMap()) {
        log.debug { "SQL: $sql" }
        if (params.isNotEmpty()) {
            log.debug { "PARAMS: $params" }
        }
    }

    /**
     * SQL 문장과 단일 레코드를 DEBUG 레벨로 로그에 출력합니다.
     *
     * ```kotlin
     * SqlLogger.logSQL("INSERT INTO users VALUES (#{id})", user)
     * // [DEBUG] SQL: INSERT INTO users VALUES (#{id})
     * // [DEBUG] RECORD: User(id=1, name=Alice)
     * ```
     *
     * @param sql 로그에 출력할 SQL 문장
     * @param record 함께 출력할 레코드 객체
     */
    fun <T: Any> logSQL(sql: String, record: T) {
        log.debug { "SQL: $sql" }
        log.debug { "RECORD: $record" }
    }

    /**
     * SQL 문장과 레코드 컬렉션을 DEBUG 레벨로 로그에 출력합니다.
     *
     * ```kotlin
     * SqlLogger.logSQL("INSERT INTO users ...", listOf(user1, user2))
     * // [DEBUG] SQL: INSERT INTO users ...
     * // [DEBUG] RECORD: User(id=1,...), User(id=2,...)
     * ```
     *
     * @param sql 로그에 출력할 SQL 문장
     * @param records 함께 출력할 레코드 컬렉션 (비어있으면 출력하지 않음)
     */
    fun <T: Any> logSQL(sql: String, records: Collection<T>) {
        log.debug { "SQL: $sql" }
        if (records.isNotEmpty()) {
            log.debug { "RECORD: ${records.joinToString()}" }
        }
    }
}
