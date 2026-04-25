package io.bluetape4k.exposed.clickhouse.dialect

import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnDiff
import org.jetbrains.exposed.v1.core.InternalApi
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect

/**
 * Exposed SQL 생성기를 ClickHouse JDBC 제약에 맞춰 조정하는 다이얼렉트입니다.
 *
 * 기본 SQL 생성은 [PostgreSQLDialect]를 재사용하고,
 * ClickHouse가 지원하지 않거나 의미가 다른 기능만 선택적으로 비활성화합니다.
 *
 * - `ALTER COLUMN TYPE` 미지원
 * - 다중 generated key 미지원
 * - `CREATE SEQUENCE` 미지원
 * - `Ternary affected row values` 미지원
 * - `RESTRICT` / `SET DEFAULT` 참조 옵션 미지원
 * - DDL은 autocommit으로 실행되어야 함
 */
class ClickHouseDialect: PostgreSQLDialect(name = dialectName) {

    companion object: KLogging() {
        /** Exposed에 등록할 ClickHouse 방언 이름입니다. */
        const val dialectName: String = "clickhouse"
    }

    // ClickHouse는 ALTER COLUMN TYPE 미지원
    override val supportsColumnTypeChange: Boolean = false

    // ClickHouse는 multiple generated keys 미지원
    override val supportsMultipleGeneratedKeys: Boolean = false

    // ClickHouse는 CREATE SEQUENCE 미지원
    override val supportsCreateSequence: Boolean = false

    // ClickHouse는 ternary affected row values 미지원
    override val supportsTernaryAffectedRowValues: Boolean = false

    // ClickHouse는 RESTRICT 참조 옵션 미지원
    override val supportsRestrictReferenceOption: Boolean = false

    // ClickHouse는 SET DEFAULT 참조 옵션 미지원
    override val supportsSetDefaultReferenceOption: Boolean = false

    // ClickHouse는 DDL을 autocommit으로 실행해야 함
    override val requiresAutoCommitOnCreateDrop: Boolean = true

    @OptIn(InternalApi::class)
    override fun modifyColumn(column: Column<*>, columnDiff: ColumnDiff): List<String> = emptyList()
}
