package io.bluetape4k.exposed.sql

import io.bluetape4k.exposed.dao.id.SnowflakeIdTable
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withSuspendedTables
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.batchInsert
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

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
        withSuspendedTables(testDB, ProductTable) {
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

            val batchedIds = ProductTable
                .select(ProductTable.id)
                .fetchBatchResultFlow(10)
                .buffer(capacity = 4)
                .onEach { log.debug { "fetch rows. rows.size=${it.size}" } }
                .map { rows -> rows.map { it[ProductTable.id].value } }
                .toList()
                .flatten()

            batchedIds.size shouldBeEqualTo products.size

            val reversedIds = ProductTable
                .select(ProductTable.id)
                .fetchBatchResultFlow(10, SortOrder.DESC)
                .buffer(capacity = 4)
                .onEach { log.debug { "fetch rows. rows.size=${it.size}" } }
                .flatMapMerge { rows ->
                    rows.asFlow().map { it[ProductTable.id].value }
                }
                .toList()

            reversedIds.size shouldBeEqualTo products.size
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
        withSuspendedTables(testDB, ItemTable) {
            val items = List(100) {
                Item(
                    name = faker.app().name() + " Version:$it",
                    price = faker.number().randomDigitNotZero()
                )
            }
            ItemTable.batchInsert(items, shouldReturnGeneratedValues = false) { product ->
                this[ItemTable.name] = product.name
                this[ItemTable.price] = product.price
            }

            val batchedIds = ItemTable
                .select(ItemTable.id)
                .fetchBatchResultFlow(10)
                .buffer(capacity = 4)
                .onEach { log.debug { "fetch rows. rows.size=${it.size}" } }
                .map { rows -> rows.map { it[ItemTable.id].value } }
                .toList()
                .flatten()

            batchedIds.size shouldBeEqualTo items.size

            val reversedIds = ItemTable
                .select(ItemTable.id)
                .fetchBatchResultFlow(10, SortOrder.DESC)
                .buffer(capacity = 4)
                .onEach { log.debug { "fetch rows. rows.size=${it.size}" } }
                .map { rows -> rows.map { it[ItemTable.id].value } }
                .toList()
                .flatten()

            reversedIds.size shouldBeEqualTo items.size
        }
    }
}
