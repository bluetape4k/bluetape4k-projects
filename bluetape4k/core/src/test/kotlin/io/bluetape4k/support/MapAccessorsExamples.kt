package io.bluetape4k.support

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class MapAccessorsExamples {

    companion object: KLogging() {
        @JvmStatic
        private val faker = Fakers.faker
    }

    private val map = mapOf<String, Any>(
        "name" to "Debop",
        "age" to 54,
        "isAdult" to true
    )

    @Test
    fun `map value get property delegate`() {
        val name by map
        val age by map
        val isAdult by map


        name shouldBeEqualTo "Debop"
        age shouldBeEqualTo 54
        isAdult shouldBeEqualTo true

        name.javaClass.kotlin shouldBeEqualTo String::class
        age.javaClass.kotlin shouldBeEqualTo Int::class
        isAdult.javaClass.kotlin shouldBeEqualTo Boolean::class
    }

    @Test
    fun `unknown key raise exception`() {
        val unknownKey by map

        assertFailsWith<NoSuchElementException> {
            unknownKey.shouldNotBeNull()
        }
    }

    private val mutableMap = mutableMapOf<String, Any>(
        "name" to "Debop",
        "age" to 54,
        "isAdult" to true
    )

    @Test
    fun `map value set property delegate`() {
        var name by mutableMap
        var age by mutableMap

        name shouldBeEqualTo "Debop"
        name = "Steve"
        name shouldBeEqualTo "Steve"

        age shouldBeEqualTo 54
        age = 80
        age shouldBeEqualTo 80
    }

    @Test
    fun `map property delegation example`() {
        val map = mutableMapOf<String, String>(
            "id" to faker.idNumber().ssnValid(),
            "name" to faker.internet().username(),
        )

        val user = User(map)
        user.id shouldBeEqualTo map["id"]
        user.name shouldBeEqualTo map["name"]

        user.name = "Debop"
        map["name"] shouldBeEqualTo user.name
    }

    data class User(private val map: MutableMap<String, String>) {
        var id by map
        var name by map
    }
}
