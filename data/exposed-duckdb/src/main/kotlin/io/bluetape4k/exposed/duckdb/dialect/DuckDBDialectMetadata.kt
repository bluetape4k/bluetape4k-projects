package io.bluetape4k.exposed.duckdb.dialect

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.vendors.PostgreSQLDialectMetadata

/**
 * DuckDB dialect metadata implementation.
 *
 * DuckDB는 PostgreSQL과 호환되므로 [PostgreSQLDialectMetadata]를 상속합니다.
 */
class DuckDBDialectMetadata : PostgreSQLDialectMetadata() {

    /**
     * DuckDB JDBC 1.1.3은 [java.sql.DatabaseMetaData.getImportedKeys]를 지원하지 않습니다.
     * FK 제약 조건 캐싱을 건너뜁니다 — [columnConstraints]는 빈 맵을 반환합니다.
     */
    override fun fillConstraintCacheForTables(tables: List<Table>) {
        // no-op: DuckDB JDBC throws SQLFeatureNotSupportedException for getImportedKeys
    }
}
