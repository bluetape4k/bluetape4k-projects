package io.bluetape4k.vertx.sqlclient.schema

import io.bluetape4k.vertx.sqlclient.getIntOrNull
import io.vertx.sqlclient.templates.RowMapper

@JvmField
val OrderRecordRowMapper = RowMapper { row ->
    OrderRecord(
        itemId = row.getInteger("item_id"),
        orderId = row.getInteger("order_id"),
        quantity = row.getInteger("quantity"),
        description = row.getString("description")
    )
}

@JvmField
val UserRowMapper = RowMapper {
    User(
        userId = it.getInteger("user_id"),
        userName = it.getString("user_name"),
        parentId = it.getInteger("parent_id")
    )
}

@JvmField
val OrderLineRowMapper = RowMapper {
    OrderLine(
        orderId = it.getIntOrNull("order_id"),
        itemId = it.getIntOrNull("item_id"),
        lineNumber = it.getIntOrNull("line_number"),
        quantity = it.getIntOrNull("quantity")
    )
}
