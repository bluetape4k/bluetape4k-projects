package io.bluetape4k.examples.exposed.mvc.domain

import io.bluetape4k.exposed.dao.entityToStringBuilder
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.spring.data.exposed.jdbc.annotation.ExposedEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import java.io.Serializable

object Products: LongIdTable("products") {
    val name = varchar("name", 255)
    val price = decimal("price", 10, 2)
    val stock = integer("stock").default(0)
}

@ExposedEntity
class ProductEntity(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<ProductEntity>(Products)

    var name: String by Products.name
    var price: java.math.BigDecimal by Products.price
    var stock: Int by Products.stock

    override fun equals(other: Any?): Boolean = idEquals(other)
    override fun hashCode(): Int = idHashCode()
    override fun toString(): String = entityToStringBuilder()
        .add("name", name)
        .add("price", price)
        .add("stock", stock)
        .toString()
}

data class ProductRecord(
    val id: Long? = null,
    val name: String,
    val price: java.math.BigDecimal = java.math.BigDecimal.ZERO,
    val stock: Int = 0,
): Serializable

fun ProductEntity.toRecord() = ProductRecord(id.value, name, price, stock)
