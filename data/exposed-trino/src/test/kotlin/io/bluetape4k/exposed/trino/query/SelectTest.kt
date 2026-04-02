package io.bluetape4k.exposed.trino.query

import io.bluetape4k.exposed.trino.AbstractTrinoTest
import io.bluetape4k.exposed.trino.domain.Events
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import java.time.Instant

class SelectTest : AbstractTrinoTest() {

    companion object : KLogging() {
        private val CREATED_AT: Instant = Instant.parse("2024-01-01T00:00:00Z")

        /**
         * 테스트 픽스처: (eventId, eventName, region)
         */
        private val FIXTURES = listOf(
            Triple(1L, "click", "kr"),
            Triple(2L, "view", "kr"),
            Triple(3L, "purchase", "us"),
            Triple(4L, "scroll", "us"),
            Triple(5L, "impression", "eu"),
        )
    }

    private fun insertFixtures() {
        FIXTURES.forEach { (id, name, region) ->
            Events.insert {
                it[eventId] = id
                it[eventName] = name
                it[Events.region] = region
                it[createdAt] = CREATED_AT
            }
        }
    }

    @Test
    fun `selectAll - 전체 이벤트 조회`() = withEventsTable {
        transaction(db) {
            insertFixtures()

            val rows = Events.selectAll().toList()
            rows.shouldNotBeEmpty()
            rows.size shouldBeEqualTo 5
        }
    }

    @Test
    fun `where - kr 리전 필터 조회`() = withEventsTable {
        transaction(db) {
            insertFixtures()

            val rows = Events.selectAll().where { Events.region eq "kr" }.toList()
            rows.size shouldBeEqualTo 2
            rows.all { it[Events.region] == "kr" } shouldBeEqualTo true
        }
    }

    @Test
    fun `orderBy - eventId 오름차순 정렬 검증`() = withEventsTable {
        transaction(db) {
            insertFixtures()

            val rows = Events.selectAll()
                .orderBy(Events.eventId to SortOrder.ASC)
                .toList()

            rows.size shouldBeEqualTo 5
            rows.first()[Events.eventId] shouldBeEqualTo 1L
            rows.last()[Events.eventId] shouldBeEqualTo 5L
        }
    }

    @Test
    fun `limit - 상위 2건만 조회`() = withEventsTable {
        transaction(db) {
            insertFixtures()

            val rows = Events.selectAll()
                .orderBy(Events.eventId to SortOrder.ASC)
                .limit(2)
                .toList()

            rows.size shouldBeEqualTo 2
        }
    }

    @Test
    fun `count - eventId 집계 함수로 전체 건수 조회`() = withEventsTable {
        transaction(db) {
            insertFixtures()

            val count = Events.selectAll().count()
            count shouldBeEqualTo 5L
        }
    }

    @Test
    fun `groupBy - 리전별 이벤트 수 집계`() = withEventsTable {
        transaction(db) {
            insertFixtures()

            val countExpr = Events.eventId.count()
            val rows = Events
                .select(Events.region, countExpr)
                .groupBy(Events.region)
                .orderBy(Events.region to SortOrder.ASC)
                .toList()

            rows.size shouldBeEqualTo 3

            val regionCounts = rows.associate { it[Events.region] to it[countExpr] }
            regionCounts["eu"] shouldBeEqualTo 1L
            regionCounts["kr"] shouldBeEqualTo 2L
            regionCounts["us"] shouldBeEqualTo 2L
        }
    }
}
