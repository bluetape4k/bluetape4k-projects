package io.bluetape4k.exposed.trino

import io.bluetape4k.exposed.trino.domain.Events
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Test
import java.time.Instant

class TrinoExtensionsTest: AbstractTrinoTest() {

    companion object: KLogging() {
        private val CREATED_AT: Instant = Instant.parse("2024-06-01T00:00:00Z")

        private val FIXTURES = listOf(
            Triple(1L, "click", "kr"),
            Triple(2L, "view", "us"),
            Triple(3L, "purchase", "eu"),
        )
    }

    /**
     * 픽스처 데이터를 events 테이블에 삽입합니다.
     */
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
    fun `suspendTransaction 은 Trino 트랜잭션 결과를 반환한다`() = runTest {
        withEventsTableSuspend {
            val count = suspendTransaction(db) {
                Events.selectAll().count()
            }
            count shouldBeEqualTo 0L
        }
    }

    @Test
    fun `suspendTransaction 안에서 쓰기 후 읽기가 가능하다`() = runTest {
        withEventsTableSuspend {
            suspendTransaction(db) {
                insertFixtures()
            }

            val rows = suspendTransaction(db) {
                Events.selectAll()
                    .orderBy(Events.eventId to SortOrder.ASC)
                    .toList()
            }

            rows shouldHaveSize 3
            rows.map { it[Events.eventId] } shouldBeEqualTo listOf(1L, 2L, 3L)
            rows.map { it[Events.eventName] } shouldBeEqualTo listOf("click", "view", "purchase")
        }
    }

    @Test
    fun `queryFlow 는 Trino 쿼리 결과를 Flow 로 반환한다`() = runTest {
        withEventsTableSuspend {
            suspendTransaction(db) {
                insertFixtures()
            }

            val rows = queryFlow(db) {
                Events.selectAll()
                    .orderBy(Events.eventId to SortOrder.ASC)
            }.toList()

            rows shouldHaveSize 3
            rows.map { it[Events.eventId] } shouldBeEqualTo listOf(1L, 2L, 3L)
            rows.map { it[Events.region] } shouldBeEqualTo listOf("kr", "us", "eu")
        }
    }

    @Test
    fun `queryFlow 는 빈 테이블에서 빈 리스트를 반환한다`() = runTest {
        withEventsTableSuspend {
            val rows = queryFlow(db) {
                Events.selectAll()
            }.toList()

            rows.shouldBeEmpty()
        }
    }
}
