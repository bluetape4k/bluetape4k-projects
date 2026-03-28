package io.bluetape4k.exposed.duckdb.dialect

import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect

/**
 * DuckDB Dialect for JetBrains Exposed ORM
 *
 * DuckDB는 PostgreSQL과 호환되므로 [PostgreSQLDialect]를 상속합니다.
 */
class DuckDBDialect : PostgreSQLDialect(name = dialectName) {

    companion object : KLogging() {
        const val dialectName: String = "duckdb"
    }
}
