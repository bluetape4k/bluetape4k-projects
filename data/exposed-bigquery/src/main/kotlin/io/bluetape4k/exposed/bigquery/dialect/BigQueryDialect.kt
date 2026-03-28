package io.bluetape4k.exposed.bigquery.dialect

import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnDiff
import org.jetbrains.exposed.v1.core.InternalApi
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect

/**
 * BigQuery Dialect for JetBrains Exposed ORM
 *
 * PostgreSQLDialect 상속하여 BigQuery 제약사항만 override:
 * - ALTER COLUMN TYPE 미지원
 * - SERIAL/SEQUENCE 미지원 (BigQuery는 auto-increment 없음)
 * - multiple generated keys 제한
 * - UPDATE/DELETE 지원되나 BigQuery 비용 정책상 사용 자제 권고
 * - WINDOW FRAME GROUPS 지원 (BigQuery 지원)
 */
class BigQueryDialect : PostgreSQLDialect(name = dialectName) {

    companion object : KLogging() {
        const val dialectName: String = "BigQuery"
        const val DRIVER_CLASS_NAME: String = "com.simba.googlebigquery.jdbc.Driver"
    }

    // BigQuery는 ALTER COLUMN TYPE 미지원
    override val supportsColumnTypeChange: Boolean = false

    // BigQuery는 multiple generated keys 미지원
    override val supportsMultipleGeneratedKeys: Boolean = false

    // BigQuery는 WINDOW FRAME GROUPS 지원
    override val supportsWindowFrameGroupsMode: Boolean = true

    @OptIn(InternalApi::class)
    override fun modifyColumn(column: Column<*>, columnDiff: ColumnDiff): List<String> = emptyList()
}
