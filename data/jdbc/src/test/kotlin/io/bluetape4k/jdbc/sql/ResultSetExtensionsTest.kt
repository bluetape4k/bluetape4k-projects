package io.bluetape4k.jdbc.sql

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class ResultSetExtensionsTest: AbstractJdbcSqlTest() {
    // ─── iterator ────────────────────────────────────────────────────────────

    @Test
    fun `iterator hasNext repeated 호출은 row를 건너뛰지 않는다`() {
        val ids =
            dataSource.runQuery("SELECT id FROM Actors ORDER BY id") { rs ->
                val iterator = rs.iterator { it.getInt("id") }

                iterator.hasNext().shouldBeTrue()
                iterator.hasNext().shouldBeTrue()

                listOf(iterator.next(), iterator.next())
            }

        ids shouldBeEqualTo listOf(1, 2)
    }

    @Test
    fun `iterator next는 hasNext 없이도 첫 row를 읽는다`() {
        val firstId =
            dataSource.runQuery("SELECT id FROM Actors ORDER BY id") { rs ->
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

    // ─── map ─────────────────────────────────────────────────────────────────

    @Test
    fun `map - 모든 행을 리스트로 변환한다`() {
        val firstnames =
            dataSource.runQuery("SELECT firstname FROM Actors ORDER BY id") { rs ->
                rs.map { getString("firstname") }
            }

        firstnames.shouldNotBeEmpty()
        firstnames.first() shouldBeEqualTo "Sunghyouk"
    }

    // ─── sequence ────────────────────────────────────────────────────────────

    @Test
    fun `sequence - 모든 행을 Sequence 로 변환한다`() {
        val count =
            dataSource.runQuery("SELECT id FROM Actors") { rs ->
                rs.sequence { it.getInt("id") }.count()
            }

        count shouldBeGreaterThan 0
    }

    // ─── columnNames / columnLabels / columnCount ─────────────────────────────

    @Test
    fun `columnNames - 컬럼 이름 목록 반환`() {
        val names =
            dataSource.runQuery("SELECT id, firstname, lastname FROM Actors LIMIT 1") { rs ->
                rs.next()
                rs.columnNames
            }

        names shouldContain "ID"
        names shouldContain "FIRSTNAME"
        names shouldContain "LASTNAME"
    }

    @Test
    fun `columnCount - 컬럼 수 반환`() {
        val count =
            dataSource.runQuery("SELECT id, firstname, lastname FROM Actors LIMIT 1") { rs ->
                rs.next()
                rs.columnCount
            }

        count shouldBeEqualTo 3
    }

    // ─── singleInt / singleLong / singleDouble / singleString ────────────────

    @Test
    fun `singleInt - 단일 Int 값 반환`() {
        val count =
            dataSource.runQuery("SELECT COUNT(*) FROM Actors") { rs ->
                rs.singleInt()
            }

        count shouldBeGreaterThan 0
    }

    @Test
    fun `singleLong - 단일 Long 값 반환`() {
        val count =
            dataSource.runQuery("SELECT COUNT(*) FROM Actors") { rs ->
                rs.singleLong()
            }

        count shouldBeGreaterThan 0L
    }

    @Test
    fun `singleDouble - 단일 Double 값 반환`() {
        val value =
            dataSource.runQuery("SELECT CAST(COUNT(*) AS DOUBLE) FROM Actors") { rs ->
                rs.singleDouble()
            }

        value shouldBeGreaterThan 0.0
    }

    @Test
    fun `singleString - 단일 String 값 반환`() {
        val name =
            dataSource.runQuery("SELECT firstname FROM Actors ORDER BY id LIMIT 1") { rs ->
                rs.singleString()
            }

        name shouldBeEqualTo "Sunghyouk"
    }

    @Test
    fun `singleInt - 빈 ResultSet 에서 IllegalStateException 발생`() {
        assertFailsWith<IllegalStateException> {
            dataSource.runQuery("SELECT id FROM Actors WHERE 1 = 0") { rs ->
                rs.singleInt()
            }
        }
    }

    // ─── isEmpty / isNotEmpty ─────────────────────────────────────────────────

    @Test
    fun `isEmpty - 결과가 없으면 true 반환`() {
        val empty =
            dataSource.runQuery("SELECT id FROM Actors WHERE 1 = 0") { rs ->
                rs.isEmpty()
            }

        empty.shouldBeTrue()
    }

    @Test
    fun `isNotEmpty - 결과가 있으면 true 반환`() {
        val notEmpty =
            dataSource.runQuery("SELECT id FROM Actors") { rs ->
                rs.isNotEmpty()
            }

        notEmpty.shouldBeTrue()
    }

    // ─── count ────────────────────────────────────────────────────────────────

    @Test
    fun `count - 전체 행 수 반환`() {
        val count =
            dataSource.runQuery("SELECT id FROM Actors") { rs ->
                rs.count()
            }

        count shouldBeGreaterThan 0
    }

    @Test
    fun `count - 조건부 행 수 반환`() {
        val count =
            dataSource.runQuery("SELECT id FROM Actors") { rs ->
                rs.count { it.getInt("id") > 0 }
            }

        count shouldBeGreaterThan 0
    }

    // ─── firstOrNull / first ──────────────────────────────────────────────────

    @Test
    fun `firstOrNull - 조건을 만족하는 첫 번째 행 반환`() {
        val firstname =
            dataSource.runQuery("SELECT * FROM Actors ORDER BY id") { rs ->
                rs.firstOrNull(
                    predicate = { it.getInt("id") == 1 },
                    mapper = { it.getString("firstname") }
                )
            }

        firstname shouldBeEqualTo "Sunghyouk"
    }

    @Test
    fun `firstOrNull - 조건을 만족하는 행이 없으면 null 반환`() {
        val result =
            dataSource.runQuery("SELECT * FROM Actors") { rs ->
                rs.firstOrNull(
                    predicate = { it.getInt("id") < 0 },
                    mapper = { it.getString("firstname") }
                )
            }

        result.shouldBeNull()
    }

    // ─── filterMap ────────────────────────────────────────────────────────────

    @Test
    fun `filterMap - 조건을 만족하는 행만 매핑하여 반환`() {
        val ids =
            dataSource.runQuery("SELECT id FROM Actors") { rs ->
                rs.filterMap(
                    predicate = { it.getInt("id") > 0 },
                    mapper = { it.getInt("id") }
                )
            }

        ids.shouldNotBeEmpty()
        ids.all { it > 0 }.shouldBeTrue()
    }

    // ─── all / any / none ─────────────────────────────────────────────────────

    @Test
    fun `all - 모든 행이 조건을 만족하면 true 반환`() {
        val result =
            dataSource.runQuery("SELECT id FROM Actors") { rs ->
                rs.all { it.getInt("id") > 0 }
            }

        result.shouldBeTrue()
    }

    @Test
    fun `all - 하나라도 조건을 만족하지 않으면 false 반환`() {
        val result =
            dataSource.runQuery("SELECT id FROM Actors") { rs ->
                rs.all { it.getInt("id") > 999 }
            }

        result.shouldBeFalse()
    }

    @Test
    fun `any - 하나라도 조건을 만족하면 true 반환`() {
        val result =
            dataSource.runQuery("SELECT id FROM Actors") { rs ->
                rs.any { it.getInt("id") == 1 }
            }

        result.shouldBeTrue()
    }

    @Test
    fun `none - 조건을 만족하는 행이 없으면 true 반환`() {
        val result =
            dataSource.runQuery("SELECT id FROM Actors") { rs ->
                rs.none { it.getInt("id") < 0 }
            }

        result.shouldBeTrue()
    }

    // ─── forEach / forEachIndexed ─────────────────────────────────────────────

    @Test
    fun `forEach - 모든 행에 대해 작업 수행`() {
        val collected = mutableListOf<Int>()
        dataSource.runQuery("SELECT id FROM Actors ORDER BY id") { rs ->
            rs.forEach { collected.add(it.getInt("id")) }
        }

        collected.shouldNotBeEmpty()
        collected.first() shouldBeEqualTo 1
    }

    @Test
    fun `forEachIndexed - 인덱스와 함께 모든 행에 대해 작업 수행`() {
        val indices = mutableListOf<Int>()
        dataSource.runQuery("SELECT id FROM Actors ORDER BY id") { rs ->
            rs.forEachIndexed { idx, _ -> indices.add(idx) }
        }

        indices.shouldNotBeEmpty()
        indices.first() shouldBeEqualTo 0
    }

    // ─── getXxxOrNull helpers ─────────────────────────────────────────────────

    @Test
    fun `getIntOrNull - SQL NULL 인 경우 null 반환`() {
        val value =
            dataSource.runQuery("SELECT NULL AS val FROM Actors LIMIT 1") { rs ->
                rs.next()
                rs.getIntOrNull(1)
            }

        value.shouldBeNull()
    }

    @Test
    fun `getStringOrNull - SQL NULL 인 경우 null 반환`() {
        val value =
            dataSource.runQuery("SELECT NULL AS val FROM Actors LIMIT 1") { rs ->
                rs.next()
                rs.getStringOrNull(1)
            }

        value.shouldBeNull()
    }

    @Test
    fun `getLongOrNull - 정상 값을 반환한다`() {
        val value =
            dataSource.runQuery("SELECT CAST(id AS BIGINT) FROM Actors ORDER BY id LIMIT 1") { rs ->
                rs.next()
                rs.getLongOrNull(1)
            }

        value.shouldNotBeNull()
        value shouldBeEqualTo 1L
    }

    @Test
    fun `getBooleanOrNull - SQL NULL 인 경우 null 반환`() {
        val value =
            dataSource.runQuery("SELECT NULL AS val FROM Actors LIMIT 1") { rs ->
                rs.next()
                rs.getBooleanOrNull(1)
            }

        value.shouldBeNull()
    }
}
