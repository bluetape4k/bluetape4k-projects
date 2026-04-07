package io.bluetape4k.exposed.duckdb.dialect

import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect

/**
 * Exposed JDBC에서 DuckDB를 PostgreSQL 계열 방언으로 취급하도록 연결하는 다이얼렉트입니다.
 *
 * DuckDB SQL 문법이 PostgreSQL과 상당 부분 호환되므로 [PostgreSQLDialect]를 기반으로 사용합니다.
 * 별도 문법 차이가 필요한 시점까지는 부모 다이얼렉트의 SQL 생성 규칙을 그대로 재사용합니다.
 */
class DuckDBDialect: PostgreSQLDialect(name = dialectName) {

    companion object: KLogging() {
        /** Exposed에 등록할 DuckDB 방언 이름입니다. */
        const val dialectName: String = "duckdb"
    }
}
