package io.bluetape4k.exposed.trino

import org.amshove.kluent.invoking
import org.amshove.kluent.shouldThrow
import org.junit.jupiter.api.Test

/**
 * [TrinoDatabase] 입력값 검증 로직을 단위 검증하는 테스트.
 *
 * Docker/Testcontainers 없이 파라미터 유효성 검사만 확인합니다.
 * 실제 연결을 시도하지 않으므로, DriverManager.getConnection 호출 이전에
 * 예외가 발생하는지를 확인합니다.
 */
class TrinoDatabaseValidationTest {

    // ----------------------------------------------------------------
    // connect(host, port, catalog, schema, user) 오버로드 검증
    // ----------------------------------------------------------------

    @Test
    fun `host 가 공백이면 IllegalArgumentException 을 던진다`() {
        invoking { TrinoDatabase.connect(host = "", port = 8080, catalog = "memory", schema = "default") }
            .shouldThrow(IllegalArgumentException::class)
    }

    @Test
    fun `port 가 0 이면 IllegalArgumentException 을 던진다`() {
        invoking { TrinoDatabase.connect(host = "localhost", port = 0, catalog = "memory", schema = "default") }
            .shouldThrow(IllegalArgumentException::class)
    }

    @Test
    fun `port 가 65536 이면 IllegalArgumentException 을 던진다`() {
        invoking { TrinoDatabase.connect(host = "localhost", port = 65536, catalog = "memory", schema = "default") }
            .shouldThrow(IllegalArgumentException::class)
    }

    @Test
    fun `catalog 가 공백이면 IllegalArgumentException 을 던진다`() {
        invoking { TrinoDatabase.connect(host = "localhost", port = 8080, catalog = "", schema = "default") }
            .shouldThrow(IllegalArgumentException::class)
    }

    @Test
    fun `schema 가 공백이면 IllegalArgumentException 을 던진다`() {
        invoking { TrinoDatabase.connect(host = "localhost", port = 8080, catalog = "memory", schema = "") }
            .shouldThrow(IllegalArgumentException::class)
    }

    // ----------------------------------------------------------------
    // connect(jdbcUrl, user) 오버로드 검증
    // ----------------------------------------------------------------

    @Test
    fun `jdbcUrl 이 공백이면 IllegalArgumentException 을 던진다`() {
        invoking { TrinoDatabase.connect(jdbcUrl = "") }
            .shouldThrow(IllegalArgumentException::class)
    }

    @Test
    fun `jdbcUrl 이 trino 프로토콜로 시작하지 않으면 IllegalArgumentException 을 던진다`() {
        invoking { TrinoDatabase.connect(jdbcUrl = "jdbc:postgresql://localhost/mydb") }
            .shouldThrow(IllegalArgumentException::class)
    }
}
