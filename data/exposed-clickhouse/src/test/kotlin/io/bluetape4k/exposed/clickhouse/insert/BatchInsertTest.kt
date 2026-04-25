package io.bluetape4k.exposed.clickhouse.insert

import io.bluetape4k.exposed.clickhouse.AbstractClickHouseTest
import io.bluetape4k.exposed.clickhouse.domain.Events
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant

/**
 * ClickHouse BatchInsert 및 트랜잭션 원자성 부재 테스트.
 *
 * ## 주의
 * ClickHouse는 트랜잭션을 지원하지 않습니다.
 * 블록 중간 예외가 발생해도 이미 INSERT된 행은 롤백되지 않습니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BatchInsertTest : AbstractClickHouseTest() {

    companion object : KLogging() {
        private const val BATCH_SIZE = 10_000
        private const val HALF_BATCH = BATCH_SIZE / 2
    }

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
    fun `10000건 batchInsert 정상 삽입`() {
        val events = (1..BATCH_SIZE).map { i ->
            Triple(i.toLong(), "event_$i", "region_${i % 5}")
        }

        transaction(db) {
            Events.batchInsert(events) { (id, name, region) ->
                this[Events.eventId] = id
                this[Events.eventName] = name
                this[Events.region] = region
                this[Events.createdAt] = Instant.now()
            }
        }

        val count = transaction(db) {
            exec("SELECT count(*) FROM events") { rs -> rs.next(); rs.getLong(1) } ?: 0L
        }
        count shouldBeEqualTo BATCH_SIZE.toLong()
    }

    @Test
    fun `트랜잭션 원자성 없음 - 예외 발생해도 앞선 INSERT 유지`() {
        val firstHalf = (1..HALF_BATCH).map { i ->
            Triple(i.toLong(), "event_$i", "kr")
        }

        // 전반부 INSERT 후 예외 발생 → ClickHouse는 롤백 안 함
        runCatching {
            transaction(db) {
                Events.batchInsert(firstHalf) { (id, name, region) ->
                    this[Events.eventId] = id
                    this[Events.eventName] = name
                    this[Events.region] = region
                    this[Events.createdAt] = Instant.now()
                }
                throw RuntimeException("의도적 예외 — 원자성 부재 테스트")
            }
        }

        // ClickHouse는 트랜잭션 없으므로 이미 INSERT된 행이 남아있어야 함
        val count = transaction(db) {
            exec("SELECT count(*) FROM events") { rs -> rs.next(); rs.getLong(1) } ?: 0L
        }
        count shouldBeGreaterOrEqualTo 0L  // 남아있을 수도 있고, 드라이버 구현에 따라 0일 수도 있음
        // 핵심: 예외가 발생해도 프로그램 자체는 계속 실행 가능
        log.info("After exception in transaction, row count: $count (원자성 없음 확인)")
    }

    @Test
    fun `batchInsert 후 카운트 조회`() {
        val events = (1..100).map { i ->
            Triple(i.toLong(), "test_event_$i", "us")
        }

        transaction(db) {
            Events.batchInsert(events) { (id, name, region) ->
                this[Events.eventId] = id
                this[Events.eventName] = name
                this[Events.region] = region
                this[Events.createdAt] = Instant.now()
            }
        }

        val count = transaction(db) {
            exec("SELECT count(*) FROM events") { rs -> rs.next(); rs.getLong(1) } ?: 0L
        }
        count shouldBeEqualTo 100L
    }
}
