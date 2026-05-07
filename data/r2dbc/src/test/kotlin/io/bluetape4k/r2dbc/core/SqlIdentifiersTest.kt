package io.bluetape4k.r2dbc.core

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * [requireValidIdentifier] SQL 식별자 허용목록 검증 테스트.
 *
 * 영문자·숫자·언더스코어·점(스키마.테이블 표기)만 허용하며,
 * SQL injection 패턴이 포함된 식별자는 [IllegalArgumentException]을 발생시켜야 합니다.
 */
class SqlIdentifiersTest {

    // -------------------------------------------------------------------------
    // 유효한 식별자 — requireValidIdentifier 가 입력을 그대로 반환해야 함
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "유효한 식별자: [{0}]")
    @ValueSource(strings = ["users", "public.users", "schema_name.table_name", "_private", "Col1"])
    fun `유효한 식별자는 그대로 반환된다`(identifier: String) {
        val result = requireValidIdentifier(identifier)
        result shouldBeEqualTo identifier
    }

    // -------------------------------------------------------------------------
    // 무효한 식별자 — SQL injection 패턴 등 IllegalArgumentException 을 발생시켜야 함
    // -------------------------------------------------------------------------

    @Test
    fun `세미콜론과 DROP TABLE 구문이 포함된 식별자는 예외를 발생시킨다`() {
        assertFailsWith<IllegalArgumentException> {
            requireValidIdentifier("users; DROP TABLE users--")
        }
    }

    @Test
    fun `작은따옴표와 OR 구문이 포함된 식별자는 예외를 발생시킨다`() {
        assertFailsWith<IllegalArgumentException> {
            requireValidIdentifier("users' OR '1'='1")
        }
    }

    @Test
    fun `백틱이 포함된 식별자는 예외를 발생시킨다`() {
        assertFailsWith<IllegalArgumentException> {
            requireValidIdentifier("`users`")
        }
    }

    @Test
    fun `숫자로 시작하는 식별자는 예외를 발생시킨다`() {
        assertFailsWith<IllegalArgumentException> {
            requireValidIdentifier("123invalid")
        }
    }

    @Test
    fun `빈 문자열 식별자는 예외를 발생시킨다`() {
        assertFailsWith<IllegalArgumentException> {
            requireValidIdentifier("")
        }
    }

    @Test
    fun `공백이 포함된 식별자는 예외를 발생시킨다`() {
        assertFailsWith<IllegalArgumentException> {
            requireValidIdentifier("users WHERE 1=1")
        }
    }

    @ParameterizedTest(name = "무효한 SQL injection 식별자: [{0}]")
    @ValueSource(strings = [
        "users; DROP TABLE users--",
        "users' OR '1'='1",
        "`users`",
        "123invalid",
        "users WHERE 1=1",
    ])
    fun `SQL injection 패턴을 포함한 식별자는 예외를 발생시킨다`(identifier: String) {
        assertFailsWith<IllegalArgumentException> {
            requireValidIdentifier(identifier)
        }
    }
}
