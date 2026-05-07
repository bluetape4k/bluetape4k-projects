package io.bluetape4k.exposed.clickhouse

import org.junit.jupiter.api.Test
import io.bluetape4k.assertions.assertFailsWith

/**
 * [ClickHouseDatabase.connect] 입력 유효성 검증 테스트.
 *
 * 컨테이너 없이 빠르게 실행되며, 잘못된 인자에 대해 [IllegalArgumentException]을
 * 명확하게 던지는지 확인합니다.
 */
class ClickHouseDatabaseValidationTest {

    @Test
    fun `connect fails when host is blank`() {
        assertFailsWith<IllegalArgumentException> {
            ClickHouseDatabase.connect(host = "", port = 8123, database = "default")
        }
    }

    @Test
    fun `connect fails when port is below range`() {
        assertFailsWith<IllegalArgumentException> {
            ClickHouseDatabase.connect(host = "localhost", port = 0, database = "default")
        }
    }

    @Test
    fun `connect fails when port is above range`() {
        assertFailsWith<IllegalArgumentException> {
            ClickHouseDatabase.connect(host = "localhost", port = 65536, database = "default")
        }
    }

    @Test
    fun `connect fails when database is blank`() {
        assertFailsWith<IllegalArgumentException> {
            ClickHouseDatabase.connect(host = "localhost", port = 8123, database = "")
        }
    }

    @Test
    fun `connect with jdbcUrl fails when blank`() {
        assertFailsWith<IllegalArgumentException> {
            ClickHouseDatabase.connect(jdbcUrl = "")
        }
    }

    @Test
    fun `connect with jdbcUrl fails when wrong prefix`() {
        assertFailsWith<IllegalArgumentException> {
            ClickHouseDatabase.connect(jdbcUrl = "jdbc:postgresql://localhost/db")
        }
    }
}
