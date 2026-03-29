package io.bluetape4k.examples.exposed.mvc.controller

import io.bluetape4k.examples.exposed.mvc.domain.ProductDto
import io.bluetape4k.examples.exposed.mvc.domain.ProductEntity
import io.bluetape4k.examples.exposed.mvc.domain.toDto
import io.bluetape4k.examples.exposed.mvc.repository.ProductJdbcRepository
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/products")
/**
 * Exposed 기반 상품 CRUD API를 제공한다.
 *
 * Exposed DAO 엔티티는 트랜잭션 경계 밖으로 넘기지 않고,
 * 각 요청에서 DTO 변환까지 하나의 transaction 안에서 마무리한다.
 */
class ProductController(
    private val productJdbcRepository: ProductJdbcRepository,
) {

    @GetMapping
    fun findAll(): List<ProductDto> =
        transaction { productJdbcRepository.findAll().map { it.toDto() } }

    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): ResponseEntity<ProductDto> {
        val entity = transaction {
            productJdbcRepository.findById(id).orElse(null)?.toDto()
        }
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(entity)
    }

    @PostMapping
    fun create(@RequestBody dto: ProductDto): ResponseEntity<ProductDto> {
        val created = transaction {
            ProductEntity.new {
                name = dto.name
                price = dto.price
                stock = dto.stock
            }.toDto()
        }
        return ResponseEntity.created(URI.create("/products/${created.id}")).body(created)
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody dto: ProductDto): ResponseEntity<ProductDto> {
        val entity = transaction {
            productJdbcRepository.findById(id).orElse(null)?.apply {
                name = dto.name
                price = dto.price
                stock = dto.stock
            }?.toDto()
        }
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(entity)
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        val deleted = transaction {
            productJdbcRepository.findById(id).orElse(null)?.let {
                it.delete()
                true
            } ?: false
        }
        if (!deleted) return ResponseEntity.notFound().build()
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/search")
    fun findByName(name: String): List<ProductDto> =
        transaction { productJdbcRepository.findByName(name).map { it.toDto() } }
}
