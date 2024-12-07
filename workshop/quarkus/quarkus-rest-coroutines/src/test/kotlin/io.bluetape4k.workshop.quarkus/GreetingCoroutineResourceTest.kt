package io.bluetape4k.workshop.quarkus

import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.quarkus.tests.restassured.bodyAs
import io.bluetape4k.quarkus.tests.restassured.bodyAsList
import io.bluetape4k.workshop.quarkus.model.Greeting
import io.quarkus.test.junit.QuarkusTest
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.amshove.kluent.shouldBeEqualTo
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.Test

@QuarkusTest
class GreetingCoroutineResourceTest {

    companion object: KLogging() {
        private const val BASE_PATH = "/coroutine"
    }

    @Test
    fun `call suspend as non-blocking`() {
        When {
            get(BASE_PATH)
        } Then {
            statusCode(200)
            body(CoreMatchers.`is`("hello quarkus!"))
        }
    }

    @Test
    fun `call sequential remote hello`() {
        When {
            get("$BASE_PATH/sequential-hello")
        } Then {
            statusCode(200)
            body(CoreMatchers.containsString("hello"))
        }
    }

    @Test
    fun `greeting with suspend`() {
        val name = TimebasedUuid.Reordered.nextId().encodeBase62()

        Given {
            pathParam("name", name)
        } When {
            get("$BASE_PATH/greeting/{name}")
        } Then {
            statusCode(200)
            body("message", CoreMatchers.equalTo("Hello $name"))
        } Extract {
            val greeting = bodyAs<Greeting>()
            greeting.message shouldBeEqualTo "Hello $name"
        }
    }

    @Test
    fun `sequential greetings`() {
        val name = TimebasedUuid.Reordered.nextId().encodeBase62()

        Given {
            pathParam("name", name)
        } When {
            get("$BASE_PATH/sequential-greeting/{name}")
        } Then {
            statusCode(200)
        } Extract {
            val greetings = bodyAsList<Greeting>()
            greetings.forEachIndexed { index, greeting ->
                greeting.message shouldBeEqualTo "Hello $name"
            }
        }
    }

    @Test
    fun `greetings as flow`() {
        val count = 4
        val name = TimebasedUuid.Reordered.nextId().encodeBase62()

        Given {
            pathParam("count", count)
            pathParam("name", name)
        } When {
            get("$BASE_PATH/greeting/{count}/{name}")
        } Then {
            statusCode(200)
            repeat(count) {
                body(CoreMatchers.containsString("Hello $name - $it"))
            }
        } Extract {
            val greetings = bodyAsList<Greeting>()
            log.debug { greetings }
            greetings.forEachIndexed { index, greeting ->
                greeting.message shouldBeEqualTo "Hello $name - $index"
            }
        }
    }

    @Test
    fun `greetings as server side events`() {
        val count = 4
        val name = TimebasedUuid.Reordered.nextId().encodeBase62()

        Given {
            pathParam("count", count)
            pathParam("name", name)
        } When {
            get("$BASE_PATH/stream/{count}/{name}")
        } Then {
            statusCode(200)
            repeat(count) {
                body(CoreMatchers.containsString("Hello $name - $it"))
            }
        } Extract {
            val bodyStr = body().asPrettyString()
            log.info { "body=\n$bodyStr" }
        }
    }
}
