package io.bluetape4k.examples.cache.lettuce.controller

import io.bluetape4k.examples.cache.lettuce.domain.Product
import io.bluetape4k.examples.cache.lettuce.repository.ProductRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/products")
/**
 * Hibernate second-level cache 예제를 위한 상품 CRUD API이다.
 */
class ProductController(private val productRepository: ProductRepository) {

    @GetMapping
    fun getAllProducts(): List<Product> = productRepository.findAll()

    @GetMapping("/{id}")
    fun getProduct(@PathVariable id: Long): ResponseEntity<Product> =
        productRepository.findById(id)
            .map { ResponseEntity.ok(it) }
            .orElse(ResponseEntity.notFound().build())

    @PostMapping
    fun createProduct(@RequestBody product: Product): Product =
        productRepository.save(product)

    @PutMapping("/{id}")
    fun updateProduct(@PathVariable id: Long, @RequestBody product: Product): ResponseEntity<Product> {
        if (!productRepository.existsById(id)) return ResponseEntity.notFound().build()
        return ResponseEntity.ok(productRepository.save(product.copy(id = id)))
    }

    @DeleteMapping("/{id}")
    fun deleteProduct(@PathVariable id: Long): ResponseEntity<Void> {
        if (!productRepository.existsById(id)) return ResponseEntity.notFound().build()
        productRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }
}
