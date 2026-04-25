package io.bluetape4k.exposed.clickhouse.insert

import io.bluetape4k.exposed.clickhouse.AbstractClickHouseTest
import io.bluetape4k.exposed.clickhouse.domain.Events
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant

/**
 * ClickHouse BatchInsert 성능 측정 벤치마크.
 *
 * 10만 건 데이터를 3회 반복 삽입하여 평균 성능을 측정합니다.
 * 임계치: 10,000행당 500ms 이내
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BatchInsertBenchmarkTest : AbstractClickHouseTest() {

    companion object : KLogging() {
        private const val TOTAL_ROWS = 100_000
        private const val ROUNDS = 3
        private const val THRESHOLD_MS_PER_10K = 500L  // 임계치: 10K 행당 500ms
    }

    @BeforeEach
    fun setup() {
        transaction(db) { SchemaUtils.create(Events) }
    }

    @AfterEach
    fun teardown() {
        transaction(db) { runCatching { SchemaUtils.drop(Events) } }
    }

    @Test
    fun `100000건 batchInsert 성능 측정`() {
        val events = (1..TOTAL_ROWS).map { i ->
            Triple(i.toLong(), "event_$i", "region_${i % 10}")
        }

        val durations = (1..ROUNDS).map { round ->
            // 테이블 초기화
            transaction(db) {
                runCatching { SchemaUtils.drop(Events) }
                SchemaUtils.create(Events)
            }
            val start = System.currentTimeMillis()
            transaction(db) {
                Events.batchInsert(events) { (id, name, region) ->
                    this[Events.eventId] = id
                    this[Events.eventName] = name
                    this[Events.region] = region
                    this[Events.createdAt] = Instant.now()
                }
            }
            val elapsed = System.currentTimeMillis() - start
            log.info("Round $round: ${elapsed}ms for $TOTAL_ROWS rows")
            elapsed
        }

        val avgMs = durations.average().toLong()
        val avgPer10K = avgMs * 10_000 / TOTAL_ROWS

        log.info("BatchInsert $TOTAL_ROWS rows — 3회 평균: ${avgMs}ms (${avgPer10K}ms/10K)")
        log.info("개별 측정: ${durations.map { "${it}ms" }}")

        // 임계치 검증: 10K 행당 500ms 이내
        avgPer10K shouldBeLessOrEqualTo THRESHOLD_MS_PER_10K
    }
}
