package io.bluetape4k.jdbc

import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test

/**
 * [JdbcDrivers] 유틸리티 함수 테스트.
 *
 * [JdbcDrivers.isMySQL], [JdbcDrivers.isPostgreSQL] 등 드라이버 판별 함수의
 * 정상 케이스·경계값·null 입력을 모두 검증합니다.
 */
class JdbcDriversTest {

    // ─── isMySQL ─────────────────────────────────────────────────────────────

    @Test
    fun `isMySQL - MySQL 드라이버 클래스명이면 true 반환`() {
        JdbcDrivers.isMySQL(JdbcDrivers.DRIVER_CLASS_MYSQL).shouldBeTrue()
    }

    @Test
    fun `isMySQL - MariaDB 드라이버 클래스명이면 true 반환`() {
        JdbcDrivers.isMySQL(JdbcDrivers.DRIVER_CLASS_MARIADB).shouldBeTrue()
    }

    @Test
    fun `isMySQL - PostgreSQL 드라이버 클래스명이면 false 반환`() {
        JdbcDrivers.isMySQL(JdbcDrivers.DRIVER_CLASS_POSTGRESQL).shouldBeFalse()
    }

    @Test
    fun `isMySQL - null 이면 false 반환`() {
        JdbcDrivers.isMySQL(null).shouldBeFalse()
    }

    @Test
    fun `isMySQL - H2 드라이버 클래스명이면 false 반환`() {
        JdbcDrivers.isMySQL(JdbcDrivers.DRIVER_CLASS_H2).shouldBeFalse()
    }

    // ─── isPostgreSQL ─────────────────────────────────────────────────────────

    @Test
    fun `isPostgreSQL - PostgreSQL 드라이버 클래스명이면 true 반환`() {
        JdbcDrivers.isPostgreSQL(JdbcDrivers.DRIVER_CLASS_POSTGRESQL).shouldBeTrue()
    }

    @Test
    fun `isPostgreSQL - MySQL 드라이버 클래스명이면 false 반환`() {
        JdbcDrivers.isPostgreSQL(JdbcDrivers.DRIVER_CLASS_MYSQL).shouldBeFalse()
    }

    @Test
    fun `isPostgreSQL - null 이면 false 반환`() {
        JdbcDrivers.isPostgreSQL(null).shouldBeFalse()
    }

    @Test
    fun `isPostgreSQL - H2 드라이버 클래스명이면 false 반환`() {
        JdbcDrivers.isPostgreSQL(JdbcDrivers.DRIVER_CLASS_H2).shouldBeFalse()
    }
}
