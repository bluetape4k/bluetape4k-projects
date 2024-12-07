package io.bluetape4k.workshop.quarkus.rest

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.quarkus.tests.restassured.bodyAs
import io.bluetape4k.quarkus.tests.restassured.bodyAsList
import io.bluetape4k.workshop.quarkus.model.Fruit
import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.jboss.resteasy.reactive.RestResponse
import org.junit.jupiter.api.Test
import kotlin.random.Random

@QuarkusTest
class FruitResourceTest {

    companion object: KLogging() {
        private const val BASE_PATH = "/fruits"
    }

    @Test
    fun `get all fruits`() {
        When {
            get(BASE_PATH)
        } Then {
            contentType(ContentType.JSON)
            statusCode(RestResponse.StatusCode.OK)
        } Extract {
            val fruits = bodyAsList<Fruit>()
            fruits.shouldNotBeEmpty()
            fruits.forEach { fruit ->
                log.debug { fruit }
            }
        }
    }

    @Test
    fun `find fruit by name`() {
        val name = "Banana"
        Given {
            pathParam("name", name)
        } When {
            get("$BASE_PATH/{name}")
        } Then {
            contentType(ContentType.JSON)
            statusCode(RestResponse.StatusCode.OK)
        } Extract {
            val fruit = bodyAs<Fruit>()
            fruit.name shouldBeEqualTo name
        }
    }

    @Test
    fun `find fruit by name which not exists`() {
        val name = "Not-Exists"
        Given {
            pathParam("name", name)
        } When {
            get("$BASE_PATH/{name}")
        } Then {
            statusCode(RestResponse.StatusCode.NO_CONTENT)
        }
    }

    @Test
    fun `add new fruit`() {
        val fruit = Fruit("Grape-${Random.nextLong()}", "Juicy fruit")

        Given {
            contentType(ContentType.JSON)
            body(fruit)
        } When {
            post(BASE_PATH)
        } Then {
            contentType(ContentType.JSON)
            statusCode(RestResponse.StatusCode.OK)
        } Extract {
            val saved = bodyAs<Fruit>()
            saved.name shouldBeEqualTo fruit.name
            saved.description shouldBeEqualTo fruit.description
        }
    }
}
