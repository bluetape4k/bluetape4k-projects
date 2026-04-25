package io.bluetape4k.exposed.clickhouse

import org.amshove.kluent.shouldThrow
import org.amshove.kluent.invoking
import org.junit.jupiter.api.Test

/**
 * [ClickHouseDatabase.connect] 입력 유효성 검증 테스트.
 *
 * 컨테이너 없이 빠르게 실행되며, 잘못된 인자에 대해 [IllegalArgumentException]을
 * 명확하게 던지는지 확인합니다.
 */
class ClickHouseDatabaseValidationTest {

    @Test
    fun `connect fails when host is blank`() {
        invoking {
            ClickHouseDatabase.connect(host = "", port = 8123, database = "default")
        } shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `connect fails when port is below range`() {
        invoking {
            ClickHouseDatabase.connect(host = "localhost", port = 0, database = "default")
        } shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `connect fails when port is above range`() {
        invoking {
            ClickHouseDatabase.connect(host = "localhost", port = 65536, database = "default")
        } shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `connect fails when database is blank`() {
        invoking {
            ClickHouseDatabase.connect(host = "localhost", port = 8123, database = "")
        } shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `connect with jdbcUrl fails when blank`() {
        invoking {
            ClickHouseDatabase.connect(jdbcUrl = "")
        } shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `connect with jdbcUrl fails when wrong prefix`() {
        invoking {
            ClickHouseDatabase.connect(jdbcUrl = "jdbc:postgresql://localhost/db")
        } shouldThrow IllegalArgumentException::class
    }
}
