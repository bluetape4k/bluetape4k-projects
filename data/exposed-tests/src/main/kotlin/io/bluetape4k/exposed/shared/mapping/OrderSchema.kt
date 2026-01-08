package io.bluetape4k.exposed.shared.mapping

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.dao.flushCache
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import java.io.Serializable
import java.time.LocalDate

object OrderSchema: KLogging() {

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

    class Order(id: EntityID<Long>): LongEntity(id), Serializable {
        companion object: LongEntityClass<Order>(OrderTable)

        val details by OrderDetail referrersOn OrderDetailTable.orderId
        var orderDate by OrderTable.orderDate

        override fun equals(other: Any?): Boolean = other is Order && id._value == other.id._value
        override fun hashCode(): Int = id._value?.hashCode() ?: System.identityHashCode(this)
        override fun toString(): String = ToStringBuilder(this)
            .add("id", id._value)
            .add("orderDate", orderDate)
            .toString()
    }

    class OrderDetail(id: EntityID<Long>): LongEntity(id), Serializable {
        companion object: LongEntityClass<OrderDetail>(OrderDetailTable)

        var order by Order referencedOn OrderDetailTable
        var lineNumber by OrderDetailTable.lineNumber
        var description by OrderDetailTable.description
        var quantity by OrderDetailTable.quantity

        val orderId by OrderDetailTable.orderId

        override fun equals(other: Any?): Boolean = other is OrderDetail && id._value == other.id._value
        override fun hashCode(): Int = id._value?.hashCode() ?: System.identityHashCode(this)
        override fun toString(): String = ToStringBuilder(this)
            .add("id", id._value)
            .add("orderId", orderId)
            .add("lineNumber", lineNumber)
            .add("description", description)
            .add("quantity", quantity)
            .toString()
    }

    class Item(id: EntityID<Long>): LongEntity(id), Serializable {
        companion object: LongEntityClass<Item>(ItemTable)

        var description by ItemTable.description

        override fun equals(other: Any?): Boolean = other is Item && id._value == other.id._value
        override fun hashCode(): Int = id._value?.hashCode() ?: System.identityHashCode(this)
        override fun toString(): String = ToStringBuilder(this)
            .add("id", id._value)
            .add("description", description)
            .toString()
    }

    class OrderLine(id: EntityID<Long>): LongEntity(id), Serializable {
        companion object: LongEntityClass<OrderLine>(OrderLineTable)

        var order by Order referencedOn OrderLineTable
        var item by Item optionalReferencedOn OrderLineTable
        var lineNumber by OrderLineTable.lineNumber
        var quantity by OrderLineTable.quantity

        override fun equals(other: Any?): Boolean = other is OrderLine && id._value == other.id._value
        override fun hashCode(): Int = id._value?.hashCode() ?: System.identityHashCode(this)
        override fun toString(): String = ToStringBuilder(this)
            .add("id", id._value)
            .add("orderId", order.id.value)
            .add("itemId", item?.id?.value)
            .add("lineNumber", lineNumber)
            .add("quantity", quantity)
            .toString()
    }

    class User(id: EntityID<Long>): LongEntity(id), Serializable {
        companion object: LongEntityClass<User>(UserTable)

        var userName by UserTable.userName
        var parent by User optionalReferencedOn UserTable.parentId

        override fun equals(other: Any?): Boolean = other is User && id._value == other.id._value
        override fun hashCode(): Int = id._value?.hashCode() ?: System.identityHashCode(this)
        override fun toString(): String = ToStringBuilder(this)
            .add("id", id._value)
            .add("userName", userName)
            .add("parentId", parent?.id?._value)
            .toString()
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

    fun AbstractExposedTest.withOrdersTables(
        testDB: TestDB,
        statement: JdbcTransaction.(
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
            val order1 = Order.new {
                orderDate = LocalDate.of(2017, 1, 17)
            }
            val orderDetail1 = OrderDetail.new {
                order = order1
                lineNumber = 1
                description = "Tennis Ball"
                quantity = 3
            }
            val orderDetail2 = OrderDetail.new {
                order = order1
                lineNumber = 2
                description = "Tennis Racket"
                quantity = 1
            }

            val order2 = Order.new {
                orderDate = LocalDate.of(2017, 1, 18)
            }
            val orderDetail3 = OrderDetail.new {
                order = order2
                lineNumber = 1
                description = "Football"
                quantity = 2
            }

            val item1 = Item.new(22) {
                description = "Helmet"
            }
            val item2 = Item.new(33) {
                description = "First Base Glove"
            }
            val item3 = Item.new(44) {
                description = "Outfield Glove"
            }
            val item4 = Item.new(55) {
                description = "Catcher Glove"
            }

            val orderLine1 = OrderLine.new {
                order = order1
                item = item1
                lineNumber = 1
                quantity = 1
            }
            val orderLine2 = OrderLine.new {
                order = order1
                item = item2
                lineNumber = 2
                quantity = 1
            }
            val orderLine3 = OrderLine.new {
                order = order2
                item = item1
                lineNumber = 1
                quantity = 1
            }
            val orderLine4 = OrderLine.new {
                order = order2
                item = item3
                lineNumber = 2
                quantity = 1
            }
            val orderLine5 = OrderLine.new {
                order = order2
                item = null
                lineNumber = 3
                quantity = 6
            }

            val fred = User.new {
                userName = "Fred"
            }
            val barney = User.new {
                userName = "Barney"
            }
            User.new {
                userName = "Pebbles"
                parent = fred
            }
            User.new {
                userName = "Bamm Bamm"
                parent = barney
            }

            flushCache()
            entityCache.clear()

            statement(orders, orderDetails, items, orderLines, users)
        }
    }
}
