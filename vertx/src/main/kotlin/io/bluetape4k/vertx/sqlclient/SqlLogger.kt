package io.bluetape4k.vertx.sqlclient

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug

/**
 * SQL 구문을 로그로 출력합니다.
 */
object SqlLogger: KLoggingChannel() {

    fun logSql(sql: String, params: Map<String, Any?> = emptyMap()) {
        log.debug { "SQL: $sql" }
        if (params.isNotEmpty()) {
            log.debug { "PARAMS: $params" }
        }
    }

    fun <T: Any> logSQL(sql: String, record: T) {
        log.debug { "SQL: $sql" }
        log.debug { "RECORD: $record" }
    }

    fun <T: Any> logSQL(sql: String, records: Collection<T>) {
        log.debug { "SQL: $sql" }
        if (records.isNotEmpty()) {
            log.debug { "RECORD: ${records.joinToString()}" }
        }
    }
}
