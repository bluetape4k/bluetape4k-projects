package io.bluetape4k.examples.exposed.mvc.repository

import io.bluetape4k.examples.exposed.mvc.domain.ProductEntity
import io.bluetape4k.spring.data.exposed.jdbc.repository.ExposedJdbcRepository

interface ProductJdbcRepository : ExposedJdbcRepository<ProductEntity, Long> {

    fun findByName(name: String): List<ProductEntity>
    fun findByPriceLessThan(price: java.math.BigDecimal): List<ProductEntity>
}
