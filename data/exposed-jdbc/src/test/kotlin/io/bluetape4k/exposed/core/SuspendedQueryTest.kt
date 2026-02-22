package io.bluetape4k.exposed.core

import io.bluetape4k.exposed.dao.id.SnowflakeIdTable
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTablesSuspending
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFailsWith

/**
 * 코루틴 환경에서 다양한 쿼리를 수행합니다.
 */
class SuspendedQueryTest: AbstractExposedTest() {

    private object ProductTable: IntIdTable("products") {
        val name = varchar("name", 255)
        val price = integer("price")
    }

    data class Product(
        val id: Int? = null,
        val name: String,
        val price: Int,
    )

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `코루틴 환경에서 AutoInc ID를 가진 테이블에서 Batch 로 읽어온다`(testDB: TestDB) = runSuspendIO {
        withTablesSuspending(testDB, ProductTable) {
            val products = List(100) {
                Product(
                    name = faker.app().name() + " Version:$it",
                    price = faker.number().randomDigitNotZero()
                )
            }
            ProductTable.batchInsert(products, shouldReturnGeneratedValues = false) { product ->
                this[ProductTable.name] = product.name
                this[ProductTable.price] = product.price
            }

            // `fetchBatchedResultFlow` 를 사용하여 10개씩 Batch 방식으로 읽어옵니다.
            val batchedIds = ProductTable
                .select(ProductTable.id)
                .fetchBatchedResultFlow(10)
                .buffer(capacity = 4)
                .onEach { log.debug { "fetch rows. rows.size=${it.size}" } }
                .flatMapConcat { rows ->
                    rows.asFlow().map { it[ProductTable.id].value }
                }
                .toList()

            batchedIds.size shouldBeEqualTo products.size

            // `fetchBatchedResultFlow` 를 사용하여 10개씩 Batch 방식으로 읽어옵니다.
            val reversedIds = ProductTable
                .select(ProductTable.id)
                .fetchBatchedResultFlow(10, SortOrder.DESC)
                .buffer(capacity = 4)
                .onEach { log.debug { "fetch rows. rows.size=${it.size}" } }
                .flatMapMerge { rows ->
                    rows.asFlow().map { it[ProductTable.id].value }
                }
                .toList()

            reversedIds.size shouldBeEqualTo products.size

            val query = ProductTable.select(ProductTable.id)
            query.fetchBatchedResultFlow(10).toList()
            query.limit shouldBeEqualTo null
            query.orderByExpressions shouldBeEqualTo emptyList()
        }
    }

    private object ItemTable: SnowflakeIdTable("snowflake_items") {
        val name = varchar("name", 255)
        val price = integer("price")
    }

    data class Item(
        val name: String,
        val price: Int,
    )

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `코루틴 환경에서 ID가 SnowflakeID의 Long 수형의 테이블에서 Batch 로 읽어온다`(testDB: TestDB) = runSuspendIO {
        withTablesSuspending(testDB, ItemTable) {
            val items = List(100) {
                Item(
                    name = faker.app().name() + " Version:$it",
                    price = faker.number().randomDigitNotZero()
                )
            }

            // Snowflake ID 를 가진 테이블에 Batch Insert 를 수행합니다.
            ItemTable.batchInsert(items, shouldReturnGeneratedValues = false) { product ->
                this[ItemTable.name] = product.name
                this[ItemTable.price] = product.price
            }

            // `fetchBatchedResultFlow` 를 사용하여 Batch 방식으로 읽어옵니다.
            val batchedIds = ItemTable
                .select(ItemTable.id)
                .fetchBatchedResultFlow(10)
                .buffer(capacity = 4)
                .onEach { log.debug { "fetch rows. rows.size=${it.size}" } }
                .flatMapConcat { rows ->
                    rows.asFlow().map { it[ItemTable.id].value }
                }
                .toList()
                .sorted()

            batchedIds.size shouldBeEqualTo items.size

            // `fetchBatchedResultFlow` 를 사용하여 Batch 방식으로 읽어옵니다.
            val reversedIds = ItemTable
                .select(ItemTable.id)
                .fetchBatchedResultFlow(10, SortOrder.DESC)
                .buffer(capacity = 4)
                .onEach { log.debug { "fetch rows. rows.size=${it.size}" } }
                .flatMapConcat { rows ->
                    rows.asFlow().map { it[ItemTable.id].value }
                }
                .toList()
                .sorted()

            reversedIds.size shouldBeEqualTo items.size
        }
    }

    private object StringIdTable: Table("string_id_items") {
        val id = varchar("id", 64)
        val name = varchar("name", 255)
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Batch 로 읽기에서 Int-Long 이 아닌 id 컬럼은 예외를 던진다`(testDB: TestDB) = runSuspendIO {
        withTablesSuspending(testDB, StringIdTable) {
            StringIdTable.insert {
                it[id] = "id-1"
                it[name] = "sample"
            }

            assertFailsWith<IllegalArgumentException> {
                StringIdTable.select(StringIdTable.id)
                    .fetchBatchedResultFlow(10)
                    .toList()
            }
        }
    }
}
