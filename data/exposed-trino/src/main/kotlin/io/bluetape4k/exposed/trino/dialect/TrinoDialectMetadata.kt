package io.bluetape4k.exposed.trino.dialect

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.vendors.PostgreSQLDialectMetadata

/**
 * Trino용 Exposed 메타데이터 어댑터입니다.
 *
 * 기본 메타데이터 해석은 [PostgreSQLDialectMetadata]를 재사용하되,
 * Trino JDBC가 지원하지 않는 메타데이터 조회는 안전하게 우회합니다.
 */
class TrinoDialectMetadata : PostgreSQLDialectMetadata() {

    /**
     * Trino JDBC는 [java.sql.DatabaseMetaData.getImportedKeys]를 지원하지 않습니다.
     * 따라서 FK 제약 조건 캐시 적재를 생략하고, 관련 조회는 빈 결과로 처리합니다.
     */
    override fun fillConstraintCacheForTables(tables: List<Table>) {
        // no-op: Trino JDBC does not support getImportedKeys
    }
}
