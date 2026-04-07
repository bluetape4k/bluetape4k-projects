package io.bluetape4k.examples.exposed.mvc

import io.bluetape4k.examples.exposed.mvc.domain.ProductEntity
import io.bluetape4k.examples.exposed.mvc.domain.Products
import io.bluetape4k.examples.exposed.mvc.repository.ProductJdbcRepository
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@SpringBootTest(classes = [DemoApplication::class])
@Transactional
class ProductJdbcRepositoryTest {

    companion object: KLogging()

    @Autowired
    private lateinit var productJdbcRepository: ProductJdbcRepository

    @BeforeEach
    fun setUp() {
        transaction { Products.deleteAll() }
    }

    @AfterEach
    fun tearDown() {
        transaction { Products.deleteAll() }
    }

    private fun createProduct(name: String, price: String, stock: Int): ProductEntity =
        transaction {
            ProductEntity.new {
                this.name = name
                this.price = BigDecimal(price)
                this.stock = stock
            }
        }

    @Test
    fun `save and findById`() {
        val product = createProduct("Kotlin Book", "39.99", 100)
        val found = productJdbcRepository.findById(product.id.value)
        found.isPresent.shouldBeTrue()
        found.get().name shouldBeEqualTo "Kotlin Book"
        found.get().price shouldBeEqualTo BigDecimal("39.99")
    }

    @Test
    fun `findById returns empty when not found`() {
        val found = productJdbcRepository.findById(-1L)
        found.isPresent.shouldBeFalse()
    }

    @Test
    fun `findAll returns all entities`() {
        createProduct("Book A", "10.00", 10)
        createProduct("Book B", "20.00", 20)
        createProduct("Book C", "30.00", 30)
        val all = productJdbcRepository.findAll()
        all shouldHaveSize 3
    }

    @Test
    fun `count returns total count`() {
        createProduct("Book A", "10.00", 10)
        createProduct("Book B", "20.00", 20)
        productJdbcRepository.count() shouldBeEqualTo 2L
    }

    @Test
    fun `existsById returns true when entity exists`() {
        val product = createProduct("Kotlin Book", "39.99", 100)
        productJdbcRepository.existsById(product.id.value).shouldBeTrue()
    }

    @Test
    fun `existsById returns false when entity does not exist`() {
        productJdbcRepository.existsById(-1L).shouldBeFalse()
    }

    @Test
    fun `deleteById removes entity`() {
        val product = createProduct("To Delete", "5.00", 1)
        productJdbcRepository.deleteById(product.id.value)
        productJdbcRepository.findById(product.id.value).isPresent.shouldBeFalse()
    }

    @Test
    fun `deleteAll removes all entities`() {
        createProduct("Book A", "10.00", 10)
        createProduct("Book B", "20.00", 20)
        productJdbcRepository.deleteAll()
        productJdbcRepository.count() shouldBeEqualTo 0L
    }

    @Test
    fun `deleteAllById removes specified entities`() {
        val a = createProduct("Book A", "10.00", 10)
        val b = createProduct("Book B", "20.00", 20)
        createProduct("Book C", "30.00", 30)

        productJdbcRepository.deleteAllById(listOf(a.id.value, b.id.value))
        productJdbcRepository.count() shouldBeEqualTo 1L
        productJdbcRepository.findAll()[0].name shouldBeEqualTo "Book C"
    }

    @Test
    fun `deleteAll with entities removes specified entities`() {
        val a = createProduct("Book A", "10.00", 10)
        val b = createProduct("Book B", "20.00", 20)
        createProduct("Book C", "30.00", 30)

        productJdbcRepository.deleteAll(listOf(a, b))
        productJdbcRepository.count() shouldBeEqualTo 1L
    }

    @Test
    fun `findAllById returns matching entities`() {
        val a = createProduct("Book A", "10.00", 10)
        val b = createProduct("Book B", "20.00", 20)
        createProduct("Book C", "30.00", 30)

        val found = productJdbcRepository.findAllById(listOf(a.id.value, b.id.value))
        found shouldHaveSize 2
        found.map { it.name }.toSet() shouldBeEqualTo setOf("Book A", "Book B")
    }

    @Test
    fun `findAll with Sort returns sorted list`() {
        createProduct("Book C", "30.00", 30)
        createProduct("Book A", "10.00", 10)
        createProduct("Book B", "20.00", 20)

        val sorted = productJdbcRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
        sorted.map { it.name } shouldBeEqualTo listOf("Book A", "Book B", "Book C")
    }

    @Test
    fun `findAll with Pageable returns page`() {
        repeat(5) { i -> createProduct("Book $i", "${10 + i}.00", 10 + i) }

        val page = productJdbcRepository.findAll(PageRequest.of(0, 3))
        page.content shouldHaveSize 3
        page.totalElements shouldBeEqualTo 5L
    }

    @Test
    fun `findAll with DSL op filters correctly`() {
        createProduct("Cheap", "5.00", 10)
        createProduct("Expensive", "100.00", 5)

        val cheap = productJdbcRepository.findAll { Products.price less BigDecimal("50.00") }
        cheap shouldHaveSize 1
        cheap[0].name shouldBeEqualTo "Cheap"
    }

    @Test
    fun `count with DSL op filters correctly`() {
        createProduct("Cheap", "5.00", 10)
        createProduct("Expensive", "100.00", 5)

        productJdbcRepository.count { Products.price less BigDecimal("50.00") } shouldBeEqualTo 1L
    }

    @Test
    fun `exists with DSL op returns correct result`() {
        createProduct("Kotlin Book", "39.99", 100)

        productJdbcRepository.exists { Products.name eq "Kotlin Book" }.shouldBeTrue()
        productJdbcRepository.exists { Products.name eq "Nobody" }.shouldBeFalse()
    }

    @Test
    fun `findByName returns matching products`() {
        createProduct("Spring Boot Guide", "49.99", 50)
        createProduct("Kotlin Book", "39.99", 100)

        val found = productJdbcRepository.findByName("Spring Boot Guide")
        found shouldHaveSize 1
        found[0].name shouldBeEqualTo "Spring Boot Guide"
    }

    @Test
    fun `findByPriceLessThan returns products under threshold`() {
        createProduct("Cheap Book", "9.99", 10)
        createProduct("Mid Book", "29.99", 20)
        createProduct("Expensive Book", "99.99", 5)

        val affordable = productJdbcRepository.findByPriceLessThan(BigDecimal("30.00"))
        affordable shouldHaveSize 2
        affordable.all { it.price < BigDecimal("30.00") }.shouldBeTrue()
    }

    @Test
    fun `extractId returns null for new entity and value for existing`() {
        val product = createProduct("Test Product", "19.99", 10)
        val id = productJdbcRepository.extractId(product)
        id.shouldNotBeNull()
        id shouldBeEqualTo product.id.value
    }
}
