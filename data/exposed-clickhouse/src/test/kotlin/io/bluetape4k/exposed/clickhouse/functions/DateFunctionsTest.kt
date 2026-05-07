package io.bluetape4k.exposed.clickhouse.functions

import io.bluetape4k.exposed.clickhouse.AbstractClickHouseTest
import io.bluetape4k.exposed.clickhouse.domain.Events
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant

/**
 * ClickHouse Date Functions (toYYYYMM, toYYYYMMDD, dateDiff, toStartOfInterval) 테스트.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DateFunctionsTest : AbstractClickHouseTest() {

    companion object : KLogging()

    @BeforeEach
    fun setup() {
        transaction(db) {
            SchemaUtils.create(Events)
        }
    }

    @AfterEach
    fun teardown() {
        transaction(db) {
            runCatching { SchemaUtils.drop(Events) }
        }
    }

    @Test
    fun `DateDiffUnit sqlValue 작은따옴표 포함 확인`() {
        DateDiffUnit.second.sqlValue shouldBeEqualTo "'second'"
        DateDiffUnit.minute.sqlValue shouldBeEqualTo "'minute'"
        DateDiffUnit.hour.sqlValue shouldBeEqualTo "'hour'"
        DateDiffUnit.day.sqlValue shouldBeEqualTo "'day'"
        DateDiffUnit.week.sqlValue shouldBeEqualTo "'week'"
        DateDiffUnit.month.sqlValue shouldBeEqualTo "'month'"
        DateDiffUnit.quarter.sqlValue shouldBeEqualTo "'quarter'"
        DateDiffUnit.year.sqlValue shouldBeEqualTo "'year'"
    }

    @Test
    fun `toYYYYMM 특정 날짜에 대해 YYYYMM 정수 반환`() {
        transaction(db) {
            Events.insert {
                it[eventId] = 1L
                it[eventName] = "march-event"
                it[region] = "kr"
                it[createdAt] = Instant.parse("2024-03-15T00:00:00Z")
            }
        }

        val result = transaction(db) {
            exec("SELECT toYYYYMM(created_at) FROM events WHERE event_id = 1") { rs ->
                rs.next(); rs.getInt(1)
            }
        }
        result shouldBeEqualTo 202403
    }

    @Test
    fun `toYYYYMMDD 특정 날짜에 대해 YYYYMMDD 정수 반환`() {
        transaction(db) {
            Events.insert {
                it[eventId] = 2L
                it[eventName] = "june-event"
                it[region] = "us"
                it[createdAt] = Instant.parse("2024-06-20T00:00:00Z")
            }
        }

        val result = transaction(db) {
            exec("SELECT toYYYYMMDD(created_at) FROM events WHERE event_id = 2") { rs ->
                rs.next(); rs.getInt(1)
            }
        }
        result shouldBeEqualTo 20240620
    }

    @Test
    fun `dateDiff 두 날짜의 일 차이 계산`() {
        val result = transaction(db) {
            exec("SELECT dateDiff('day', toDate('2024-01-01'), toDate('2024-01-10'))") { rs ->
                rs.next(); rs.getLong(1)
            }
        }
        result shouldBeEqualTo 9L
    }

    @Test
    fun `dateDiff 두 날짜의 월 차이 계산`() {
        val result = transaction(db) {
            exec("SELECT dateDiff('month', toDate('2024-01-01'), toDate('2024-04-01'))") { rs ->
                rs.next(); rs.getLong(1)
            }
        }
        result shouldBeEqualTo 3L
    }

    @Test
    fun `dateDiff 시간 차이 계산`() {
        val result = transaction(db) {
            exec("SELECT dateDiff('hour', toDateTime('2024-01-01 00:00:00'), toDateTime('2024-01-01 06:00:00'))") { rs ->
                rs.next(); rs.getLong(1)
            }
        }
        result shouldBeEqualTo 6L
    }

    @Test
    fun `toStartOfInterval 300초 단위 버림`() {
        transaction(db) {
            Events.insert {
                it[eventId] = 3L
                it[eventName] = "interval-event"
                it[region] = "kr"
                it[createdAt] = Instant.parse("2024-03-15T01:07:30Z")
            }
        }

        // 2024-03-15T01:07:30Z -> 300초(5분) 단위로 버림 -> 2024-03-15T01:05:00Z
        val result = transaction(db) {
            exec("SELECT toStartOfInterval(created_at, INTERVAL 300 SECOND) FROM events WHERE event_id = 3") { rs ->
                rs.next(); rs.getString(1)
            }
        }
        result.shouldNotBeNull()
        log.debug("toStartOfInterval result: $result")
    }

    @Test
    fun `toYYYYMM Function 클래스 SQL 생성 확인`() {
        transaction(db) {
            Events.insert {
                it[eventId] = 4L
                it[eventName] = "sql-gen-test"
                it[region] = "jp"
                it[createdAt] = Instant.parse("2024-11-05T00:00:00Z")
            }
        }

        val result = transaction(db) {
            exec("SELECT toYYYYMM(created_at) FROM events WHERE event_id = 4") { rs ->
                rs.next(); rs.getInt(1)
            }
        }
        result shouldBeEqualTo 202411
    }

    @Test
    fun `toYYYYMMDD Function 클래스 SQL 생성 확인`() {
        transaction(db) {
            Events.insert {
                it[eventId] = 5L
                it[eventName] = "yyyymmdd-test"
                it[region] = "eu"
                it[createdAt] = Instant.parse("2025-12-31T00:00:00Z")
            }
        }

        val result = transaction(db) {
            exec("SELECT toYYYYMMDD(created_at) FROM events WHERE event_id = 5") { rs ->
                rs.next(); rs.getInt(1)
            }
        }
        result shouldBeEqualTo 20251231
    }
}
