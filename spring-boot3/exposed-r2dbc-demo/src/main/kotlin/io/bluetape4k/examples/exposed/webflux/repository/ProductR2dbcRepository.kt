package io.bluetape4k.examples.exposed.webflux.repository

import io.bluetape4k.examples.exposed.webflux.domain.ProductDto
import io.bluetape4k.examples.exposed.webflux.domain.Products
import io.bluetape4k.spring.data.exposed.r2dbc.repository.ExposedR2dbcRepository
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IdTable

/**
 * 상품 DTO에 대한 suspend CRUD Repository 입니다.
 */
interface ProductR2dbcRepository: ExposedR2dbcRepository<ProductDto, Long> {

    override val table: IdTable<Long> get() = Products

    override fun extractId(entity: ProductDto): Long? = entity.id

    override fun toDomain(row: ResultRow): ProductDto =
        ProductDto(
            id = row[Products.id].value,
            name = row[Products.name],
            price = row[Products.price],
            stock = row[Products.stock],
        )

    override fun toPersistValues(domain: ProductDto): Map<Column<*>, Any?> =
        mapOf(
            Products.name to domain.name,
            Products.price to domain.price,
            Products.stock to domain.stock,
        )
}
