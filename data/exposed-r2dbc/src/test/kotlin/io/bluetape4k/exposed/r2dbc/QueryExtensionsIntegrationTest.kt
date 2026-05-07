package io.bluetape4k.exposed.r2dbc

import io.bluetape4k.exposed.r2dbc.tests.AbstractExposedR2dbcTest
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * [Query.forEach] 및 [Query.forEachIndexed] DB 연동 통합 테스트입니다.
 *
 * 실제 데이터베이스 연결이 필요한 suspend 확장 함수를 검증합니다.
 */
class QueryExtensionsIntegrationTest : AbstractExposedR2dbcTest() {

    companion object : KLoggingChannel()

    private object ItemTable : IntIdTable("r2dbc_qext_items") {
        val name = varchar("name", 128)
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `forEach 는 모든 row 를 순서대로 방문한다`(testDB: TestDB) = runSuspendIO {
        withTables(testDB, ItemTable) {
            val names = listOf("alpha", "beta", "gamma")
            ItemTable.batchInsert(names) { name -> this[ItemTable.name] = name }

            val collected = mutableListOf<String>()
            ItemTable.selectAll().orderBy(ItemTable.id, SortOrder.ASC).forEach { row ->
                collected.add(row[ItemTable.name])
            }

            collected shouldBeEqualTo names
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `forEach 는 빈 테이블에서 블록을 실행하지 않는다`(testDB: TestDB) = runSuspendIO {
        withTables(testDB, ItemTable) {
            var count = 0
            ItemTable.selectAll().forEach { count++ }

            count shouldBeEqualTo 0
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `forEachIndexed 는 0부터 시작하는 인덱스를 함께 제공한다`(testDB: TestDB) = runSuspendIO {
        withTables(testDB, ItemTable) {
            val names = listOf("one", "two", "three")
            ItemTable.batchInsert(names) { name -> this[ItemTable.name] = name }

            val indices = mutableListOf<Int>()
            ItemTable.selectAll().orderBy(ItemTable.id, SortOrder.ASC).forEachIndexed { index, _ ->
                indices.add(index)
            }

            indices shouldBeEqualTo listOf(0, 1, 2)
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `forEachIndexed 는 row 값과 인덱스를 동시에 접근한다`(testDB: TestDB) = runSuspendIO {
        withTables(testDB, ItemTable) {
            val names = listOf("x", "y")
            ItemTable.batchInsert(names) { name -> this[ItemTable.name] = name }

            val pairs = mutableListOf<Pair<Int, String>>()
            ItemTable.selectAll().orderBy(ItemTable.id, SortOrder.ASC).forEachIndexed { index, row ->
                pairs.add(index to row[ItemTable.name])
            }

            pairs.map { it.first } shouldBeEqualTo listOf(0, 1)
            pairs.map { it.second } shouldBeEqualTo names
            pairs.all { it.second.isNotEmpty() }.shouldBeTrue()
        }
    }
}
