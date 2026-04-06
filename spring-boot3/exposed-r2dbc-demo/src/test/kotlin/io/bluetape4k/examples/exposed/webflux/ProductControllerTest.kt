package io.bluetape4k.examples.exposed.webflux

import io.bluetape4k.examples.exposed.webflux.domain.ProductRecord
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import java.math.BigDecimal
import kotlin.test.fail

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ProductControllerTest {

    @Value("\${local.server.port}")
    private var port: Int = 0

    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setup() {
        webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
    }

    @Test
    @Order(1)
    fun `GET products returns list`() {
        val products = awaitProducts()
        products.size shouldBeEqualTo 3
    }

    @Test
    fun `POST product creates new entity`() {
        val dto = ProductRecord(name = "New Product", price = BigDecimal("15.00"), stock = 10)
        webTestClient.post().uri("/products")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(dto)
            .exchange()
            .expectStatus().isCreated
            .expectBody<ProductRecord>()
            .consumeWith { result ->
                result.responseBody?.id.shouldNotBeNull()
                result.responseBody?.name shouldBeEqualTo "New Product"
            }
    }

    @Test
    fun `GET product by id returns entity`() {
        // 먼저 생성
        val dto = ProductRecord(name = "Findable", price = BigDecimal("5.00"), stock = 1)
        val created = webTestClient.post().uri("/products")
            .contentType(MediaType.APPLICATION_JSON).bodyValue(dto)
            .exchange().expectBody<ProductRecord>().returnResult().responseBody!!

        webTestClient.get().uri("/products/${created.id}")
            .exchange()
            .expectStatus().isOk
            .expectBody<ProductRecord>()
            .consumeWith { result ->
                result.responseBody?.name shouldBeEqualTo "Findable"
            }
    }

    @Test
    fun `PUT product updates entity`() {
        val dto = ProductRecord(name = "Before Update", price = BigDecimal("5.00"), stock = 1)
        val created = webTestClient.post().uri("/products")
            .contentType(MediaType.APPLICATION_JSON).bodyValue(dto)
            .exchange().expectBody<ProductRecord>().returnResult().responseBody!!

        val updated = dto.copy(name = "After Update", price = BigDecimal("10.00"))
        webTestClient.put().uri("/products/${created.id}")
            .contentType(MediaType.APPLICATION_JSON).bodyValue(updated)
            .exchange()
            .expectStatus().isOk
            .expectBody<ProductRecord>()
            .consumeWith { result ->
                result.responseBody?.name shouldBeEqualTo "After Update"
            }
    }

    @Test
    fun `DELETE product removes entity`() {
        val dto = ProductRecord(name = "To Be Deleted", price = BigDecimal("1.00"), stock = 1)
        val created = webTestClient.post().uri("/products")
            .contentType(MediaType.APPLICATION_JSON).bodyValue(dto)
            .exchange().expectBody<ProductRecord>().returnResult().responseBody!!

        webTestClient.delete().uri("/products/${created.id}")
            .exchange()
            .expectStatus().isNoContent()

        webTestClient.get().uri("/products/${created.id}")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `GET missing product returns 404`() {
        webTestClient.get().uri("/products/999999")
            .exchange()
            .expectStatus().isNotFound
    }

    private fun awaitProducts(): List<ProductRecord> {
        repeat(30) {
            val result = webTestClient.get().uri("/products")
                .exchange()
                .expectStatus().isOk
                .expectBodyList<ProductRecord>()
                .returnResult()
                .responseBody ?: emptyList()
            if (result.size == 3) {
                return result
            }
            Thread.sleep(100)
        }
        fail("seed products were not initialized in time")
    }
}
