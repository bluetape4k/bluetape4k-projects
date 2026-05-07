package io.bluetape4k.exposed.clickhouse.functions

import io.bluetape4k.exposed.clickhouse.AbstractClickHouseTest
import io.bluetape4k.exposed.clickhouse.domain.Events
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeLessOrEqualTo
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import io.bluetape4k.assertions.assertFailsWith
import java.time.Instant

/**
 * ClickHouse Aggregate Functions (argMax, argMin, quantile, uniq, uniqExact) 테스트.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AggregateFunctionsTest : AbstractClickHouseTest() {

    companion object : KLogging()

    @BeforeEach
    fun setup() {
        transaction(db) {
            SchemaUtils.create(Events)
            // 10,000건 삽입: eventId 1~10000, region은 0~9 순환
            Events.batchInsert((1..10_000).toList()) { i ->
                this[Events.eventId] = i.toLong()
                this[Events.eventName] = "event_$i"
                this[Events.region] = "region_${i % 10}"
                this[Events.createdAt] = Instant.now()
            }
        }
    }

    @AfterEach
    fun teardown() {
        transaction(db) {
            runCatching { SchemaUtils.drop(Events) }
        }
    }

    @Test
    fun `quantile level out of range throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> { quantile(-0.1, Events.eventId) }
        assertFailsWith<IllegalArgumentException> { quantile(1.1, Events.eventId) }
    }

    @Test
    fun `quantile 0_5 중앙값이 예상 범위에 있음`() {
        val result = transaction(db) {
            exec("SELECT quantile(0.5)(event_id) FROM events") { rs ->
                rs.next(); rs.getDouble(1)
            }
        }
        // 1~10000의 중앙값은 약 5000
        result!! shouldBeGreaterOrEqualTo 4000.0
        result shouldBeLessOrEqualTo 6000.0
    }

    @Test
    fun `quantile 0_95 95번째 백분위수가 예상 범위에 있음`() {
        val result = transaction(db) {
            exec("SELECT quantile(0.95)(event_id) FROM events") { rs ->
                rs.next(); rs.getDouble(1)
            }
        }
        // 95th percentile of 1~10000 ≈ 9500
        result!! shouldBeGreaterOrEqualTo 9000.0
        result shouldBeLessOrEqualTo 10000.0
    }

    @Test
    fun `uniq 근사 count distinct 반환`() {
        val result = transaction(db) {
            exec("SELECT uniq(region) FROM events") { rs ->
                rs.next(); rs.getLong(1)
            }
        }
        // region은 region_0~region_9 총 10종류 (HyperLogLog 근사이므로 8 이상이면 정상)
        result!! shouldBeGreaterOrEqualTo 8L
    }

    @Test
    fun `uniqExact 정확한 count distinct 반환`() {
        val result = transaction(db) {
            exec("SELECT uniqExact(region) FROM events") { rs ->
                rs.next(); rs.getLong(1)
            }
        }
        // 정확히 10종류
        result shouldBeEqualTo 10L
    }

    @Test
    fun `argMax event_id 최대인 row의 event_name 반환`() {
        val result = transaction(db) {
            exec("SELECT argMax(event_name, event_id) FROM events") { rs ->
                rs.next(); rs.getString(1)
            }
        }
        // event_id가 최대(10000)인 row의 event_name = "event_10000"
        result shouldBeEqualTo "event_10000"
    }

    @Test
    fun `argMin event_id 최소인 row의 event_name 반환`() {
        val result = transaction(db) {
            exec("SELECT argMin(event_name, event_id) FROM events") { rs ->
                rs.next(); rs.getString(1)
            }
        }
        // event_id가 최소(1)인 row의 event_name = "event_1"
        result shouldBeEqualTo "event_1"
    }

    @Test
    fun `ArgMax SQL 생성 확인`() {
        val sql = transaction(db) {
            val argMaxExpr = argMax(Events.eventName, Events.eventId)
            val queryBuilder = QueryBuilder(true)
            argMaxExpr.toQueryBuilder(queryBuilder)
            queryBuilder.toString()
        }
        require(sql.contains("argMax")) { "Expected SQL to contain 'argMax', but was: $sql" }
    }

    @Test
    fun `ArgMin SQL 생성 확인`() {
        val sql = transaction(db) {
            val argMinExpr = argMin(Events.eventName, Events.eventId)
            val queryBuilder = QueryBuilder(true)
            argMinExpr.toQueryBuilder(queryBuilder)
            queryBuilder.toString()
        }
        require(sql.contains("argMin")) { "Expected SQL to contain 'argMin', but was: $sql" }
    }

    @Test
    fun `Quantile SQL 생성 확인`() {
        val sql = transaction(db) {
            val quantileExpr = quantile(0.95, Events.eventId)
            val queryBuilder = QueryBuilder(true)
            quantileExpr.toQueryBuilder(queryBuilder)
            queryBuilder.toString()
        }
        require(sql.contains("quantile(0.95)")) { "Expected SQL to contain 'quantile(0.95)', but was: $sql" }
    }

    @Test
    fun `Uniq SQL 생성 확인`() {
        val sql = transaction(db) {
            val uniqExpr = uniq(Events.region)
            val queryBuilder = QueryBuilder(true)
            uniqExpr.toQueryBuilder(queryBuilder)
            queryBuilder.toString()
        }
        require(sql.contains("uniq(")) { "Expected SQL to contain 'uniq(', but was: $sql" }
    }

    @Test
    fun `UniqExact SQL 생성 확인`() {
        val sql = transaction(db) {
            val uniqExactExpr = uniqExact(Events.region)
            val queryBuilder = QueryBuilder(true)
            uniqExactExpr.toQueryBuilder(queryBuilder)
            queryBuilder.toString()
        }
        require(sql.contains("uniqExact(")) { "Expected SQL to contain 'uniqExact(', but was: $sql" }
    }
}
