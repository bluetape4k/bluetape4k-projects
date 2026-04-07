package io.bluetape4k.exposed.lettuce.domain

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import java.math.BigDecimal

object ItemTable: LongIdTable("items") {
    val name = varchar("name", 255)
    val price = decimal("price", 10, 2)
}

data class ItemDto(
    val id: Long,
    val name: String,
    val price: BigDecimal,
): java.io.Serializable
