package io.bluetape4k.vertx.sqlclient.schema

import org.mybatis.dynamic.sql.AliasableSqlTable
import java.io.Serializable
import java.time.LocalDate


object JoinSchema {

    class OrderMasterTable: AliasableSqlTable<OrderMasterTable>("OrderMaster", JoinSchema::OrderMasterTable) {
        val orderId = column<Int>("order_id")
        val orderDate = column<LocalDate>("order_date")
    }

    class OrderDetailTable: AliasableSqlTable<OrderDetailTable>("OrderDetail", JoinSchema::OrderDetailTable) {
        val orderId = column<Int>("order_id")
        val lineNumber = column<Int>("line_number")
        val description = column<String>("description")
        val quantity = column<Int>("quantity")
    }

    class ItemMasterTable: AliasableSqlTable<ItemMasterTable>("ItemMaster", JoinSchema::ItemMasterTable) {
        val itemId = column<Int>("item_id")
        val description = column<String>("description")
    }

    class OrderLineTable: AliasableSqlTable<OrderLineTable>("OrderLine", JoinSchema::OrderLineTable) {
        val orderId = column<Int>("order_id")
        val itemId = column<Int>("item_id")
        val lineNumber = column<Int>("line_number")
        val quantity = column<Int>("quantity")
    }

    class UsersTable: AliasableSqlTable<UsersTable>("Users", JoinSchema::UsersTable) {
        val userId = column<Int>("user_id")
        val userName = column<String>("user_name")
        val parentId = column<Int>("parent_id")
    }

    val orderMaster = OrderMasterTable()
    val orderDetail = OrderDetailTable()
    val itemMaster = ItemMasterTable()
    val orderLine = OrderLineTable()
    val user = UsersTable()
}


data class OrderMaster(
    val orderId: Int? = null,
    var orderDate: LocalDate? = null,
    val details: MutableList<OrderDetail> = mutableListOf(),
): Comparable<OrderMaster>, Serializable {
    override fun compareTo(other: OrderMaster): Int {
        return orderId?.compareTo(other.orderId ?: 0) ?: 0
    }
}

data class OrderDetail(
    var orderId: Int? = null,
    var lineNumber: Int? = null,
    var description: String? = null,
    var quantity: Int? = null,
): Comparable<OrderDetail>, Serializable {
    override fun compareTo(other: OrderDetail): Int {
        return orderId?.compareTo(other.orderId ?: 0)
            ?: lineNumber?.compareTo(other.lineNumber ?: 0)
            ?: 0
    }
}

data class ItemMaster(
    var itemId: Int? = null,
    var description: String? = null,
): Comparable<ItemMaster>, Serializable {
    override fun compareTo(other: ItemMaster): Int {
        return itemId?.compareTo(other.itemId ?: 0) ?: 0
    }
}

data class OrderLine(
    val orderId: Int? = null,
    var itemId: Int? = null,
    var lineNumber: Int? = null,
    var quantity: Int? = null,
): Comparable<OrderLine>, Serializable {
    override fun compareTo(other: OrderLine): Int {
        return orderId?.compareTo(other.orderId ?: 0)
            ?: itemId?.compareTo(other.itemId ?: 0)
            ?: lineNumber?.compareTo(other.lineNumber ?: 0)
            ?: 0
    }
}

data class User(
    var userId: Int? = null,
    var userName: String? = null,
    var parentId: Int? = null,
): Comparable<User>, Serializable {
    override fun compareTo(other: User): Int {
        return userId?.compareTo(other.userId ?: 0)
            ?: userName?.compareTo(other.userName.orEmpty())
            ?: 0
    }
}

data class OrderRecord(
    val itemId: Int?,
    val orderId: Int?,
    val quantity: Int?,
    val description: String?,
): Comparable<OrderRecord>, Serializable {
    override fun compareTo(other: OrderRecord): Int {
        return orderId?.compareTo(other.orderId ?: 0)
            ?: itemId?.compareTo(other.itemId ?: 0)
            ?: 0
    }
}
