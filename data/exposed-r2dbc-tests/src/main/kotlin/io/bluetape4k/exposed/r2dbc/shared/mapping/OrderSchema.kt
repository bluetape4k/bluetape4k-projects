package io.bluetape4k.exposed.r2dbc.shared.mapping

import io.bluetape4k.exposed.r2dbc.tests.R2dbcExposedTestBase
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.insertAndGetId
import java.io.Serializable
import java.time.LocalDate

object OrderSchema {

    val allOrderTables = arrayOf(OrderTable, OrderDetailTable, ItemTable, OrderLineTable, UserTable)

    object OrderTable: LongIdTable("orders") {
        val orderDate = date("order_date")
    }

    object OrderDetailTable: LongIdTable("order_details") {
        val orderId = reference("order_id", OrderTable)
        val lineNumber = integer("line_number")
        val description = varchar("description", 255)
        val quantity = integer("quantity")
    }

    object ItemTable: LongIdTable("items") {
        val description = varchar("description", 255)
    }

    object OrderLineTable: LongIdTable("order_lines") {
        val orderId = reference("order_id", OrderTable)
        val itemId = optReference("item_id", ItemTable)
        val lineNumber = integer("line_number")
        val quantity = integer("quantity")
    }

    object UserTable: LongIdTable("users") {
        val userName = varchar("user_name", 255)
        val parentId = reference("parent_id", UserTable).nullable()
    }

    data class OrderRecord(
        val itemId: Long? = null,
        val orderId: Long? = null,
        val quantity: Int? = null,
        val description: String? = null,
    ): Comparable<OrderRecord>, Serializable {
        override fun compareTo(other: OrderRecord): Int =
            orderId?.compareTo(other.orderId ?: 0)
                ?: itemId?.compareTo(other.itemId ?: 0)
                ?: 0
    }

    @Suppress("UnusedReceiverParameter")
    suspend fun R2dbcExposedTestBase.withOrdersTables(
        testDB: TestDB,
        statement: suspend R2dbcTransaction.(
            orders: OrderTable,
            orderDetails: OrderDetailTable,
            items: ItemTable,
            orderLines: OrderLineTable,
            users: UserTable,
        ) -> Unit,
    ) {

        val orders = OrderTable
        val orderDetails = OrderDetailTable
        val items = ItemTable
        val orderLines = OrderLineTable
        val users = UserTable

        withTables(testDB, *allOrderTables) {
            val order1 = OrderTable.insertAndGetId {
                it[orderDate] = LocalDate.of(2017, 1, 17)
            }

            OrderDetailTable.insert {
                it[orderId] = order1
                it[lineNumber] = 1
                it[description] = "Tennis Ball"
                it[quantity] = 3
            }
            OrderDetailTable.insert {
                it[orderId] = order1
                it[lineNumber] = 2
                it[description] = "Tennis Racket"
                it[quantity] = 1
            }

            val order2 = OrderTable.insertAndGetId {
                it[orderDate] = LocalDate.of(2017, 1, 18)
            }
            OrderDetailTable.insert {
                it[orderId] = order2
                it[lineNumber] = 1
                it[description] = "Football"
                it[quantity] = 2
            }

            val item1 = ItemTable.insertAndGetId {
                it[id] = 22
                it[description] = "Helmet"
            }
            val item2 = ItemTable.insertAndGetId {
                it[id] = 33
                it[description] = "First Base Glove"
            }
            val item3 = ItemTable.insertAndGetId {
                it[id] = 44
                it[description] = "Outfield Glove"
            }
            ItemTable.insertAndGetId {
                it[id] = 55
                it[description] = "Catcher Glove"
            }

            OrderLineTable.insert {
                it[orderId] = order1
                it[itemId] = item1
                it[lineNumber] = 1
                it[quantity] = 1
            }
            OrderLineTable.insert {
                it[orderId] = order1
                it[itemId] = item2
                it[lineNumber] = 2
                it[quantity] = 1
            }
            OrderLineTable.insert {
                it[orderId] = order2
                it[itemId] = item1
                it[lineNumber] = 1
                it[quantity] = 1
            }
            OrderLineTable.insert {
                it[orderId] = order2
                it[itemId] = item3
                it[lineNumber] = 2
                it[quantity] = 1
            }
            OrderLineTable.insert {
                it[orderId] = order2
                it[itemId] = null
                it[lineNumber] = 3
                it[quantity] = 6
            }

            val fred = UserTable.insertAndGetId {
                it[userName] = "Fred"
            }
            val barney = UserTable.insertAndGetId {
                it[userName] = "Barney"
            }
            UserTable.insert {
                it[userName] = "Pebbles"
                it[parentId] = fred
            }
            UserTable.insert {
                it[userName] = "Bamm Bamm"
                it[parentId] = barney
            }

            statement(orders, orderDetails, items, orderLines, users)
        }
    }
}
