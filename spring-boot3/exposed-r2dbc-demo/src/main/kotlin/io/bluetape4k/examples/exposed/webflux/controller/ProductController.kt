package io.bluetape4k.examples.exposed.webflux.controller

import io.bluetape4k.examples.exposed.webflux.domain.ProductRecord
import io.bluetape4k.examples.exposed.webflux.repository.ProductR2dbcRepository
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/products")
/**
 * Exposed R2DBC 기반 상품 CRUD API이다.
 */
class ProductController(
    private val productRepository: ProductR2dbcRepository,
) {

    @GetMapping
    suspend fun findAll(): List<ProductRecord> =
        productRepository.findAllAsList()

    @GetMapping("/{id}")
    suspend fun findById(@PathVariable id: Long): ProductRecord =
        productRepository.findByIdOrNull(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: $id")

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun create(@RequestBody dto: ProductRecord): ProductRecord =
        productRepository.save(dto.copy(id = null))

    @PutMapping("/{id}")
    suspend fun update(@PathVariable id: Long, @RequestBody dto: ProductRecord): ProductRecord =
        suspendTransaction {
            val existing = productRepository.findByIdOrNull(id)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: $id")
            productRepository.save(dto.copy(id = existing.id ?: id))
        }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun delete(@PathVariable id: Long) {
        suspendTransaction {
            val existing = productRepository.findByIdOrNull(id)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: $id")
            productRepository.deleteById(existing.id ?: id)
        }
    }
}
