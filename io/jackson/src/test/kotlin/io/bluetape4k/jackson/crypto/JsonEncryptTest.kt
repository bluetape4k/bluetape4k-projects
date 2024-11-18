package io.bluetape4k.jackson.crypto

import com.fasterxml.jackson.module.kotlin.readValue
import io.bluetape4k.crypto.encrypt.AES
import io.bluetape4k.crypto.encrypt.RC4
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.jackson.prettyWriteAsString
import io.bluetape4k.jackson.writeAsString
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotContain
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

@RandomizedTest
class JsonEncryptTest {

    companion object: KLogging() {
        private const val REPEAT_COUNT = 5
        private val faker = Fakers.faker
    }

    data class User(
        val username: String,

        @field:JsonEncrypt(encryptor = AES::class)
        val password: String,

        @get:JsonEncrypt(encryptor = RC4::class)
        val mobile: String,
    )

    private val mapper = Jackson.defaultJsonMapper

    private fun createUser(): User {
        return User(
            username = faker.internet().username(),
            password = faker.internet().password(),
            mobile = faker.phoneNumber().cellPhone()
        )
    }

    @Test
    fun `encrypt string property`() {
        val user = User(faker.internet().username(), "mypassword", "010-8955-0581")
        log.debug { mapper.prettyWriteAsString(user) }

        val encrypted = mapper.writeAsString(user)!!
        encrypted shouldNotContain "mypassword"
        encrypted shouldNotContain "010-8955-5081"
    }

    @RepeatedTest(REPEAT_COUNT)
    fun `encrypt json property`() {
        val expected = createUser()
        val encrypted = mapper.writeValueAsString(expected)
        log.debug { "encrypted=$encrypted" }

        val actual = mapper.readValue<User>(encrypted)
        log.debug { "actual=$actual" }
        actual shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_COUNT)
    fun `encrypt json property in list`() {
        val expected = List(20) { createUser() }
        val encrypted = mapper.writeValueAsString(expected)
        log.debug { "encrypted=$encrypted" }

        val actuals = mapper.readValue<List<User>>(encrypted)
        actuals shouldBeEqualTo expected
    }
}
