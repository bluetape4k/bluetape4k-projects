package io.bluetape4k.exposed.clickhouse

import io.bluetape4k.exposed.clickhouse.dialect.ClickHouseDialect
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

/**
 * [ClickHouseDialect] 단위 테스트.
 *
 * 다이얼렉트 이름과 ClickHouse 제약을 반영한 boolean flag 들이 올바르게 설정되어
 * 있는지 확인합니다.
 */
class ClickHouseDialectTest {

    @Test
    fun `dialectName is clickhouse`() {
        ClickHouseDialect.dialectName shouldBeEqualTo "clickhouse"
    }

    @Test
    fun `dialect flags are set correctly`() {
        val dialect = ClickHouseDialect()

        dialect.supportsColumnTypeChange shouldBeEqualTo false
        dialect.supportsMultipleGeneratedKeys shouldBeEqualTo false
        dialect.supportsCreateSequence shouldBeEqualTo false
        dialect.supportsTernaryAffectedRowValues shouldBeEqualTo false
        dialect.supportsRestrictReferenceOption shouldBeEqualTo false
        dialect.supportsSetDefaultReferenceOption shouldBeEqualTo false
        dialect.requiresAutoCommitOnCreateDrop shouldBeEqualTo true
    }
}
