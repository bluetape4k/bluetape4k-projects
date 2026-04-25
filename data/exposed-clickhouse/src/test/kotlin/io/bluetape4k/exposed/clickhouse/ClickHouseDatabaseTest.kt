package io.bluetape4k.exposed.clickhouse

import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * [ClickHouseDatabase.connect] 통합 테스트.
 *
 * 실제 ClickHouse 컨테이너에 연결해 단순 쿼리를 실행해, 두 가지 connect 오버로드가
 * 정상 동작하는지 확인합니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClickHouseDatabaseTest: AbstractClickHouseTest() {

    private fun selectOne(database: org.jetbrains.exposed.v1.jdbc.Database): Int {
        var value = -1
        transaction(database) {
            value = this.exec("SELECT 1") { rs -> rs.next(); rs.getInt(1) } ?: -1
        }
        return value
    }

    @Test
    fun `connect with host port database`() {
        val database = ClickHouseDatabase.connect(
            host = clickhouse.host,
            port = clickhouse.port,
            database = "default",
            user = clickhouse.username ?: "test",
            password = clickhouse.password ?: "test",
        )
        selectOne(database) shouldBeEqualTo 1
    }

    @Test
    fun `connect with jdbcUrl`() {
        val database = ClickHouseDatabase.connect(
            jdbcUrl = clickhouse.jdbcUrl,
            user = clickhouse.username ?: "test",
            password = clickhouse.password ?: "test",
        )
        selectOne(database) shouldBeEqualTo 1
    }
}
