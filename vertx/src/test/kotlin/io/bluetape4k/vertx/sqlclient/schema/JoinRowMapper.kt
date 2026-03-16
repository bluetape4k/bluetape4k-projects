package io.bluetape4k.vertx.sqlclient.schema

import io.bluetape4k.vertx.sqlclient.getIntOrNull
import io.bluetape4k.vertx.sqlclient.getStringOrNull
import io.vertx.sqlclient.templates.RowMapper

@JvmField
val OrderRecordRowMapper = RowMapper { row ->
    OrderRecord(
        itemId = row.getIntOrNull("item_id"),
        orderId = row.getIntOrNull("order_id"),
        quantity = row.getIntOrNull("quantity"),
        description = row.getStringOrNull("description")
    )
}

@JvmField
val UserRowMapper = RowMapper {
    User(
        userId = it.getIntOrNull("user_id"),
        userName = it.getStringOrNull("user_name"),
        parentId = it.getIntOrNull("parent_id")
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
