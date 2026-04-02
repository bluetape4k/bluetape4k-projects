package io.bluetape4k.exposed.trino.dialect

import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnDiff
import org.jetbrains.exposed.v1.core.InternalApi
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect

/**
 * Exposed SQL 생성기를 Trino JDBC 제약에 맞춰 조정하는 다이얼렉트입니다.
 *
 * 기본 SQL 생성은 [PostgreSQLDialect]를 재사용하고,
 * Trino가 지원하지 않거나 의미가 다른 기능만 선택적으로 비활성화합니다.
 *
 * - `ALTER COLUMN TYPE` 미지원
 * - 다중 generated key 미지원
 * - `WINDOW FRAME GROUPS` 지원
 */
class TrinoDialect : PostgreSQLDialect(name = dialectName) {

    companion object : KLogging() {
        /** Exposed에 등록할 Trino 방언 이름입니다. */
        const val dialectName: String = "trino"
    }

    // Trino는 ALTER COLUMN TYPE 미지원
    override val supportsColumnTypeChange: Boolean = false

    // Trino는 multiple generated keys 미지원
    override val supportsMultipleGeneratedKeys: Boolean = false

    // Trino는 WINDOW FRAME GROUPS 지원
    override val supportsWindowFrameGroupsMode: Boolean = true

    @OptIn(InternalApi::class)
    override fun modifyColumn(column: Column<*>, columnDiff: ColumnDiff): List<String> = emptyList()
}
