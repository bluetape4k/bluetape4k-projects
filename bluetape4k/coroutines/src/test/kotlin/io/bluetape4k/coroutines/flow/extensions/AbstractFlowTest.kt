package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.io.Serializable
import kotlin.random.Random

abstract class AbstractFlowTest {

    companion object: KLoggingChannel() {
        const val REPEAT_SIZE = 3

        @JvmStatic
        val faker = Fakers.faker
    }

    data class OrderRow(
        val orderId: Int,
        val itemId: Int,
        val itemName: String = Fakers.fixedString(16),
        val itemQuantity: Int = Random.nextInt(10, 99),
    ): Serializable

    data class Order(
        val id: Int,
        val items: List<OrderItem>,
    ): Serializable {
        fun prettyString(): String = buildString {
            appendLine("Order[$id]")
            items.forEachIndexed { index, item ->
                appendLine("item[$index]=$item")
            }
        }
    }

    data class OrderItem(
        val id: Int,
        val name: String,
        val quantity: Int,
    ): Serializable
}
