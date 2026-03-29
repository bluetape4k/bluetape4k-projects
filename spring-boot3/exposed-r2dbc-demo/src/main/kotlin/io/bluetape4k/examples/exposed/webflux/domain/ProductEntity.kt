package io.bluetape4k.examples.exposed.webflux.domain

import io.bluetape4k.exposed.core.HasIdentifier
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object Products : LongIdTable("webflux_products") {
    val name = varchar("name", 255)
    val price = decimal("price", 10, 2)
    val stock = integer("stock").default(0)
}

data class ProductDto(
    override val id: Long? = null,
    val name: String,
    val price: java.math.BigDecimal,
    val stock: Int = 0,
) : HasIdentifier<Long>
