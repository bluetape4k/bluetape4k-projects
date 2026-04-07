package io.bluetape4k.exposed.trino.insert

import io.bluetape4k.exposed.trino.AbstractTrinoTest
import io.bluetape4k.exposed.trino.domain.Events
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import java.time.Instant

class InsertTest: AbstractTrinoTest() {

    companion object: KLogging()

    @Test
    fun `insert - 단건 INSERT 후 count 검증`() = withEventsTable {
        transaction(db) {
            Events.insert {
                it[eventId] = 1L
                it[eventName] = "click"
                it[region] = "kr"
                it[createdAt] = Instant.parse("2024-01-01T00:00:00Z")
            }
        }

        val count = transaction(db) {
            Events.selectAll().count()
        }

        count shouldBeEqualTo 1L
    }

    @Test
    fun `batchInsert - N건 일괄 INSERT 후 count 검증`() = withEventsTable {
        val items = listOf(
            Triple(1L, "click", "kr"),
            Triple(2L, "view", "us"),
            Triple(3L, "purchase", "eu"),
        )

        transaction(db) {
            Events.batchInsert(items) { (id, name, region) ->
                this[Events.eventId] = id
                this[Events.eventName] = name
                this[Events.region] = region
                this[Events.createdAt] = Instant.parse("2024-01-01T00:00:00Z")
            }
        }

        val count = transaction(db) {
            Events.selectAll().count()
        }

        count shouldBeEqualTo items.size.toLong()
    }

    @Test
    fun `insert - nullable createdAt null 값으로 삽입 검증`() = withEventsTable {
        transaction(db) {
            Events.insert {
                it[eventId] = 1L
                it[eventName] = "view"
                it[region] = "us"
                it[createdAt] = null
            }
        }

        val row = transaction(db) {
            Events.selectAll().single()
        }

        row[Events.createdAt].shouldBeNull()
    }
}
