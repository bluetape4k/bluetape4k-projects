package io.bluetape4k.examples.exposed.webflux.domain

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import java.io.Serializable

object Products: LongIdTable("webflux_products") {
    val name = varchar("name", 255)
    val price = decimal("price", 10, 2)
    val stock = integer("stock").default(0)
}

data class ProductRecord(
    val id: Long? = null,
    val name: String,
    val price: java.math.BigDecimal = java.math.BigDecimal.ZERO,
    val stock: Int = 0,
): Serializable
