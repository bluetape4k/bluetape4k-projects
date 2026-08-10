package io.bluetape4k.jdbc.sql

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.sql.ResultSet
import java.sql.SQLException
import io.bluetape4k.assertions.assertFailsWith

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
    fun `columnLabels - 별칭을 포함한 컬럼 레이블 목록 반환`() {
        val labels =
            dataSource.runQuery("SELECT id AS actor_id, firstname AS actor_name FROM Actors LIMIT 1") { rs ->
                rs.next()
                rs.columnLabels
            }

        labels shouldBeEqualTo listOf("ACTOR_ID", "ACTOR_NAME")
    }

    @Test
    fun `ResultSet get 연산자는 인덱스와 레이블을 통해 null을 보존한다`() {
        dataSource.runQuery("SELECT NULL AS nullable_value FROM Actors LIMIT 1") { rs ->
            rs.next()
            rs[1].shouldBeNull()
            rs["nullable_value"].shouldBeNull()
        }
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
    fun `singleBigDecimal - 단일 BigDecimal 값 반환`() {
        val value =
            dataSource.runQuery("SELECT CAST(12.50 AS DECIMAL(10, 2)) FROM Actors LIMIT 1") { rs ->
                rs.singleBigDecimal()
            }

        value shouldBeEqualTo java.math.BigDecimal("12.50")
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
                rs.isEmptyByMovingCursor()
            }

        empty.shouldBeTrue()
    }

    @Test
    fun `isNotEmpty - 결과가 있으면 true 반환`() {
        val notEmpty =
            dataSource.runQuery("SELECT id FROM Actors") { rs ->
                rs.isNotEmptyByMovingCursor()
            }

        notEmpty.shouldBeTrue()
    }

    @Test
    fun `isNotEmptyByMovingCursor positions cursor on first row`() {
        val firstId =
            dataSource.runQuery("SELECT id FROM Actors ORDER BY id") { rs ->
                rs.isNotEmptyByMovingCursor().shouldBeTrue()
                rs.getInt("id")
            }

        firstId shouldBeEqualTo 1
    }

    @Test
    fun `isNotEmptyByMovingCursor consumes first row before normal iteration`() {
        val remainingIds =
            dataSource.runQuery("SELECT id FROM Actors ORDER BY id") { rs ->
                rs.isNotEmptyByMovingCursor().shouldBeTrue()
                rs.toList { it.getInt("id") }
            }

        remainingIds.first() shouldBeEqualTo 2
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

    @Test
    fun `first - 조건을 만족하는 첫 번째 행 반환`() {
        val firstname =
            dataSource.runQuery("SELECT * FROM Actors ORDER BY id") { rs ->
                rs.first(
                    predicate = { it.getInt("id") == 1 },
                    mapper = { it.getString("firstname") }
                )
            }

        firstname shouldBeEqualTo "Sunghyouk"
    }

    @Test
    fun `first - 조건을 만족하는 행이 없으면 예외 발생`() {
        assertFailsWith<NoSuchElementException> {
            dataSource.runQuery("SELECT * FROM Actors") { rs ->
                rs.first(
                    predicate = { it.getInt("id") < 0 },
                    mapper = { it.getString("firstname") }
                )
            }
        }
    }

    @Test
    fun `ResultSet iterator - mapper 없이 현재 ResultSet을 순회한다`() {
        val ids =
            dataSource.runQuery("SELECT id FROM Actors ORDER BY id LIMIT 2") { rs ->
                val iterator = rs.iterator()
                buildList {
                    while (iterator.hasNext()) {
                        add(iterator.next().getInt("id"))
                    }
                }
            }

        ids shouldBeEqualTo listOf(1, 2)
    }

    @Test
    fun `mapAsSequence - ResultSet을 지연 Sequence로 변환한다`() {
        val ids =
            dataSource.runQuery("SELECT id FROM Actors ORDER BY id LIMIT 2") { rs ->
                rs.mapAsSequence { getInt("id") }.toList()
            }

        ids shouldBeEqualTo listOf(1, 2)
    }

    @Test
    fun `emptyResultToNull - 정상 결과와 SQLException을 각각 처리한다`() {
        val value =
            dataSource.runQuery("SELECT id FROM Actors LIMIT 1") { rs ->
                rs.emptyResultToNull { resultSet ->
                    resultSet.next()
                    resultSet.getInt(1)
                }
            }
        value shouldBeEqualTo 1

        val empty =
            dataSource.runQuery("SELECT id FROM Actors LIMIT 1") { rs ->
                rs.emptyResultToNull<Int> { throw SQLException("synthetic failure") }
            }
        empty.shouldBeNull()
    }

    @Test
    @Suppress("DEPRECATION")
    fun `deprecated cursor helpers and moveToPrevious preserve their contracts`() {
        dataSource.runQuery("SELECT id FROM Actors ORDER BY id LIMIT 2") { rs ->
            rs.isNotEmpty().shouldBeTrue()
            rs.isEmpty().shouldBeFalse()
        }

        val scrollableResultSet = java.lang.reflect.Proxy.newProxyInstance(
            ResultSet::class.java.classLoader,
            arrayOf(ResultSet::class.java)
        ) { _, method, _ ->
            method.name == "previous"
        } as ResultSet
        scrollableResultSet.moveToPrevious().shouldBeTrue()

        val resultSet = java.lang.reflect.Proxy.newProxyInstance(
            ResultSet::class.java.classLoader,
            arrayOf(ResultSet::class.java)
        ) { _, method, _ ->
            if (method.name == "previous") {
                throw SQLException("not scrollable")
            }
            false
        } as ResultSet

        resultSet.moveToPrevious().shouldBeFalse()
    }

    @Test
    fun `all nullable ResultSet getters return null for SQL NULL`() {
        val resultSet = java.lang.reflect.Proxy.newProxyInstance(
            ResultSet::class.java.classLoader,
            arrayOf(ResultSet::class.java)
        ) { _, method, _ ->
            when {
                method.name == "wasNull" -> true
                method.returnType == Boolean::class.javaPrimitiveType -> false
                method.returnType == Byte::class.javaPrimitiveType -> 0.toByte()
                method.returnType == Short::class.javaPrimitiveType -> 0.toShort()
                method.returnType == Int::class.javaPrimitiveType -> 0
                method.returnType == Long::class.javaPrimitiveType -> 0L
                method.returnType == Float::class.javaPrimitiveType -> 0.0f
                method.returnType == Double::class.javaPrimitiveType -> 0.0
                else -> null
            }
        } as ResultSet

        val values = listOf(
            resultSet.getBooleanOrNull(1), resultSet.getBooleanOrNull("value"),
            resultSet.getByteOrNull(1), resultSet.getByteOrNull("value"),
            resultSet.getShortOrNull(1), resultSet.getShortOrNull("value"),
            resultSet.getIntOrNull(1), resultSet.getIntOrNull("value"),
            resultSet.getLongOrNull(1), resultSet.getLongOrNull("value"),
            resultSet.getFloatOrNull(1), resultSet.getFloatOrNull("value"),
            resultSet.getDoubleOrNull(1), resultSet.getDoubleOrNull("value"),
            resultSet.getBigDecimalOrNull(1), resultSet.getBigDecimalOrNull("value"),
            resultSet.getBytesOrNull(1), resultSet.getBytesOrNull("value"),
            resultSet.getObjectOrNull(1), resultSet.getObjectOrNull("value"),
            resultSet.getArrayOrNull(1), resultSet.getArrayOrNull("value"),
            resultSet.getDateOrNull(1), resultSet.getDateOrNull("value"),
            resultSet.getTimeOrNull(1), resultSet.getTimeOrNull("value"),
            resultSet.getTimestampOrNull(1), resultSet.getTimestampOrNull("value"),
            resultSet.getAsciiStreamOrNull(1), resultSet.getAsciiStreamOrNull("value"),
            resultSet.getBinaryStreamOrNull(1), resultSet.getBinaryStreamOrNull("value"),
            resultSet.getCharacterStreamOrNull(1), resultSet.getCharacterStreamOrNull("value"),
            resultSet.getNCharacterStreamOrNull(1), resultSet.getNCharacterStreamOrNull("value"),
            resultSet.getStringOrNull(1), resultSet.getStringOrNull("value"),
            resultSet.getNStringOrNull(1), resultSet.getNStringOrNull("value"),
            resultSet.getBlobOrNull(1), resultSet.getBlobOrNull("value"),
            resultSet.getClobOrNull(1), resultSet.getClobOrNull("value"),
            resultSet.getNClobOrNull(1), resultSet.getNClobOrNull("value"),
            resultSet.getSQLXMLOrNull(1), resultSet.getSQLXMLOrNull("value"),
            resultSet.getRefOrNull(1), resultSet.getRefOrNull("value"),
            resultSet.getRowIdOrNull(1), resultSet.getRowIdOrNull("value"),
            resultSet.getURLOrNull(1), resultSet.getURLOrNull("value")
        )

        values.all { it == null }.shouldBeTrue()
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
