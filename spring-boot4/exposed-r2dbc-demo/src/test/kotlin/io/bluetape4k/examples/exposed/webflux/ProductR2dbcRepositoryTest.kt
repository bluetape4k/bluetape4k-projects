package io.bluetape4k.examples.exposed.webflux

import io.bluetape4k.examples.exposed.webflux.domain.ProductDto
import io.bluetape4k.examples.exposed.webflux.domain.Products
import io.bluetape4k.examples.exposed.webflux.repository.ProductR2dbcRepository
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

@SpringBootTest(
    classes = [WebfluxDemoApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class ProductR2dbcRepositoryTest {

    companion object : KLoggingChannel()

    @Autowired
    private lateinit var productRepository: ProductR2dbcRepository

    @Autowired
    private lateinit var r2dbcDatabase: R2dbcDatabase

    @AfterEach
    fun tearDown(): Unit = runBlocking {
        suspendTransaction(r2dbcDatabase) { Products.deleteAll() }
    }

    private suspend fun createProduct(name: String, price: String, stock: Int): ProductDto =
        productRepository.save(
            ProductDto(id = null, name = name, price = BigDecimal(price), stock = stock)
        )

    @Test
    fun `save and findByIdOrNull`() = runTest {
        val product = createProduct("Kotlin Book", "39.99", 100)
        val id = product.id.shouldNotBeNull()

        val found = productRepository.findByIdOrNull(id)
        found.shouldNotBeNull()
        found.name shouldBeEqualTo "Kotlin Book"
        found.price shouldBeEqualTo BigDecimal("39.99")
    }

    @Test
    fun `findByIdOrNull returns null when not found`() = runTest {
        productRepository.findByIdOrNull(-1L).shouldBeNull()
    }

    @Test
    fun `findAllAsList returns all entities`() = runSuspendIO {
        createProduct("Book A", "10.00", 10)
        createProduct("Book B", "20.00", 20)
        createProduct("Book C", "30.00", 30)

        val all = productRepository.findAllAsList()
        all shouldHaveSize 3
    }

    @Test
    fun `findAll as Flow returns all entities`() = runTest {
        createProduct("Book A", "10.00", 10)
        createProduct("Book B", "20.00", 20)

        val all = productRepository.findAll().toList()
        all shouldHaveSize 2
    }

    @Test
    fun `count returns total count`() = runTest {
        createProduct("Book A", "10.00", 10)
        createProduct("Book B", "20.00", 20)
        productRepository.count() shouldBeEqualTo 2L
    }

    @Test
    fun `existsById returns true when entity exists`() = runTest {
        val product = createProduct("Kotlin Book", "39.99", 100)
        productRepository.existsById(product.id!!).shouldBeTrue()
    }

    @Test
    fun `existsById returns false when entity does not exist`() = runTest {
        productRepository.existsById(-1L).shouldBeFalse()
    }

    @Test
    fun `deleteById removes entity`() = runTest {
        val product = createProduct("To Delete", "5.00", 1)
        val id = product.id!!
        productRepository.deleteById(id)
        productRepository.findByIdOrNull(id).shouldBeNull()
    }

    @Test
    fun `deleteAll removes all entities`() = runTest {
        createProduct("Book A", "10.00", 10)
        createProduct("Book B", "20.00", 20)
        productRepository.deleteAll()
        productRepository.count() shouldBeEqualTo 0L
    }

    @Test
    fun `deleteAllById removes specified entities`() = runTest {
        val a = createProduct("Book A", "10.00", 10)
        val b = createProduct("Book B", "20.00", 20)
        createProduct("Book C", "30.00", 30)

        productRepository.deleteAllById(listOf(a.id!!, b.id!!))
        productRepository.count() shouldBeEqualTo 1L
        productRepository.findAllAsList()[0].name shouldBeEqualTo "Book C"
    }

    @Test
    fun `deleteAll with Iterable removes specified entities`() = runTest {
        val a = createProduct("Book A", "10.00", 10)
        val b = createProduct("Book B", "20.00", 20)
        createProduct("Book C", "30.00", 30)

        productRepository.deleteAll(listOf(a, b))
        productRepository.count() shouldBeEqualTo 1L
    }

    @Test
    fun `deleteAll with Flow removes specified entities`() = runTest {
        val a = createProduct("Book A", "10.00", 10)
        val b = createProduct("Book B", "20.00", 20)
        createProduct("Book C", "30.00", 30)

        productRepository.deleteAll(flowOf(a, b))
        productRepository.count() shouldBeEqualTo 1L
    }

    @Test
    fun `saveAll with Iterable saves all entities`() = runTest {
        val dtos = listOf(
            ProductDto(id = null, name = "Book A", price = BigDecimal("10.00"), stock = 10),
            ProductDto(id = null, name = "Book B", price = BigDecimal("20.00"), stock = 20),
        )
        val saved = productRepository.saveAll(dtos).toList()
        saved shouldHaveSize 2
        saved.all { it.id != null }.shouldBeTrue()
        productRepository.count() shouldBeEqualTo 2L
    }

    @Test
    fun `saveAll with Flow saves all entities`() = runTest {
        val dtoFlow = listOf(
            ProductDto(id = null, name = "Book A", price = BigDecimal("10.00"), stock = 10),
            ProductDto(id = null, name = "Book B", price = BigDecimal("20.00"), stock = 20),
        ).asFlow()
        val saved = productRepository.saveAll(dtoFlow).toList()
        saved shouldHaveSize 2
        saved.all { it.id != null }.shouldBeTrue()
        productRepository.count() shouldBeEqualTo 2L
    }

    @Test
    fun `findAllById with Iterable returns matching entities`() = runTest {
        val a = createProduct("Book A", "10.00", 10)
        val b = createProduct("Book B", "20.00", 20)
        createProduct("Book C", "30.00", 30)

        val found = productRepository.findAllById(listOf(a.id!!, b.id!!)).toList()
        found shouldHaveSize 2
        found.map { it.name }.toSet() shouldBeEqualTo setOf("Book A", "Book B")
    }

    @Test
    fun `findAllById with Flow returns matching entities`() = runTest {
        val a = createProduct("Book A", "10.00", 10)
        val b = createProduct("Book B", "20.00", 20)
        createProduct("Book C", "30.00", 30)

        val found = productRepository.findAllById(flowOf(a.id!!, b.id!!)).toList()
        found shouldHaveSize 2
        found.map { it.name }.toSet() shouldBeEqualTo setOf("Book A", "Book B")
    }

    @Test
    fun `streamAll opens its own transaction and streams rows`() = runTest {
        createProduct("Book A", "10.00", 10)
        createProduct("Book B", "20.00", 20)

        val all = productRepository.streamAll().toList()
        all shouldHaveSize 2
    }

    @Test
    fun `extractId returns null for new entity`() = runTest {
        val newProduct = ProductDto(id = null, name = "New", price = BigDecimal("9.99"), stock = 1)
        productRepository.extractId(newProduct).shouldBeNull()
    }

    @Test
    fun `extractId returns id for existing entity`() = runTest {
        val saved = createProduct("Existing", "9.99", 1)
        val id = productRepository.extractId(saved)
        id.shouldNotBeNull()
        id shouldBeEqualTo saved.id
    }
}
