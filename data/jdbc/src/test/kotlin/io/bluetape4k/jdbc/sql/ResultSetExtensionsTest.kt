package io.bluetape4k.jdbc.sql

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class ResultSetExtensionsTest: AbstractJdbcSqlTest() {

    @Test
    fun `iterator hasNext repeated 호출은 row를 건너뛰지 않는다`() {
        val ids = dataSource.runQuery("SELECT id FROM Actors ORDER BY id") { rs ->
            val iterator = rs.iterator { it.getInt("id") }

            iterator.hasNext().shouldBeTrue()
            iterator.hasNext().shouldBeTrue()

            listOf(iterator.next(), iterator.next())
        }

        ids shouldBeEqualTo listOf(1, 2)
    }

    @Test
    fun `iterator next는 hasNext 없이도 첫 row를 읽는다`() {
        val firstId = dataSource.runQuery("SELECT id FROM Actors ORDER BY id") { rs ->
            rs.iterator { it.getInt("id") }.next()
        }

        firstId shouldBeEqualTo 1
    }

    @Test
    fun `iterator next는 빈 결과에서 NoSuchElementException 을 발생시킨다`() {
        assertFailsWith<NoSuchElementException> {
            dataSource.runQuery("SELECT id FROM Actors WHERE 1 = 0") { rs ->
                rs.iterator { it.getInt("id") }.next()
            }
        }
    }
}
