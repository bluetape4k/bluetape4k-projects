package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import kotlin.random.Random

class BufferUntilChangedTest: AbstractFlowTest() {

    companion object: KLogging()

    @Test
    fun `master id 가 같은 경우끼리 하나의 List로 묶습니다`() = runTest {
        val orderCount = Random.nextInt(3, 6)
        val itemCount = Random.nextInt(2, 9)
        val orders = getOrderRows(orderCount, itemCount).log("source")
            .bufferUntilChanged { it.orderId }.log("buffer")
            .map { rows ->
                Order(rows[0].orderId, rows.map { OrderItem(it.itemId, it.itemName, it.itemQuantity) })
            }
            .toList()

        orders shouldHaveSize orderCount
        orders.all { it.items.size == itemCount }.shouldBeTrue()
        orders.forEach { order ->
            log.trace { "order=${order.prettyString()}" }
        }
    }

    private fun getOrderRows(orderCount: Int = 4, itemCount: Int = 5): Flow<OrderRow> {
        log.trace { "order=$orderCount, item=$itemCount" }

        return List(orderCount) { oid ->
            List(itemCount) { itemId ->
                OrderRow(
                    oid + 1,
                    (oid + 1) * 10 + itemId + 1,
                )
            }
        }.flatten().asFlow()
    }
}
