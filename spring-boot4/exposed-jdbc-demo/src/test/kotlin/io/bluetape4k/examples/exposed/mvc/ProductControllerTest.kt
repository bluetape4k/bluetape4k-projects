package io.bluetape4k.examples.exposed.mvc

import io.bluetape4k.examples.exposed.mvc.domain.ProductRecord
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import java.math.BigDecimal

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ProductControllerTest {

    @Value("\${local.server.port}")
    private var port: Int = 0

    private lateinit var client: RestClient

    @BeforeEach
    fun setup() {
        client = RestClient.builder()
            .baseUrl("http://localhost:$port")
            .build()
    }

    @Test
    @Order(1)
    fun `GET products returns list`() {
        val response = client.get()
            .uri("/products")
            .retrieve()
            .toEntity<List<ProductRecord>>()
        response.statusCode shouldBeEqualTo HttpStatus.OK
        response.body.shouldNotBeNull()
    }

    @Test
    fun `POST product creates new entity`() {
        val dto = ProductRecord(name = "Test Product", price = BigDecimal("19.99"), stock = 10)
        val response = client.post()
            .uri("/products")
            .contentType(MediaType.APPLICATION_JSON)
            .body(dto)
            .retrieve()
            .toEntity<ProductRecord>()
        response.statusCode shouldBeEqualTo HttpStatus.CREATED
        response.body?.id.shouldNotBeNull()
        response.body?.name shouldBeEqualTo "Test Product"
    }

    @Test
    fun `GET product by id returns entity`() {
        val dto = ProductRecord(name = "Find Me", price = BigDecimal("9.99"), stock = 5)
        val created = client.post().uri("/products")
            .contentType(MediaType.APPLICATION_JSON).body(dto)
            .retrieve().toEntity<ProductRecord>().body!!

        val response = client.get()
            .uri("/products/${created.id}")
            .retrieve()
            .toEntity<ProductRecord>()
        response.statusCode shouldBeEqualTo HttpStatus.OK
        response.body?.name shouldBeEqualTo "Find Me"
    }

    @Test
    fun `PUT product updates entity`() {
        val dto = ProductRecord(name = "Original", price = BigDecimal("5.00"), stock = 1)
        val created = client.post().uri("/products")
            .contentType(MediaType.APPLICATION_JSON).body(dto)
            .retrieve().toEntity<ProductRecord>().body!!

        val updated = dto.copy(name = "Updated", price = BigDecimal("10.00"))
        val response = client.put()
            .uri("/products/${created.id}")
            .contentType(MediaType.APPLICATION_JSON)
            .body(updated)
            .retrieve()
            .toEntity<ProductRecord>()
        response.statusCode shouldBeEqualTo HttpStatus.OK
        response.body?.name shouldBeEqualTo "Updated"
    }

    @Test
    fun `DELETE product removes entity`() {
        val dto = ProductRecord(name = "To Delete", price = BigDecimal("1.00"), stock = 1)
        val created = client.post().uri("/products")
            .contentType(MediaType.APPLICATION_JSON).body(dto)
            .retrieve().toEntity<ProductRecord>().body!!

        val deleteResponse = client.delete()
            .uri("/products/${created.id}")
            .retrieve()
            .toBodilessEntity()
        deleteResponse.statusCode shouldBeEqualTo HttpStatus.NO_CONTENT
    }

    @Test
    fun `GET unknown product returns not found`() {
        val response = runCatching {
            client.get()
                .uri("/products/999999")
                .retrieve()
                .toEntity<ProductRecord>()
        }.exceptionOrNull() as? HttpClientErrorException

        response.shouldNotBeNull()
        response.statusCode shouldBeEqualTo HttpStatus.NOT_FOUND
    }

    @Test
    fun `GET products search by name returns filtered list`() {
        val dto = ProductRecord(name = "SearchableProduct", price = BigDecimal("15.00"), stock = 3)
        client.post().uri("/products")
            .contentType(MediaType.APPLICATION_JSON).body(dto)
            .retrieve().toBodilessEntity()

        val response = client.get()
            .uri("/products/search?name=SearchableProduct")
            .retrieve()
            .toEntity<List<ProductRecord>>()
        response.statusCode shouldBeEqualTo HttpStatus.OK
        response.body!!.any { it.name == "SearchableProduct" }.shouldBeTrue()
    }
}
