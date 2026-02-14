package io.bluetape4k.jackson3.crypto

import io.bluetape4k.codec.Base58
import io.bluetape4k.crypto.encrypt.AES
import io.bluetape4k.crypto.encrypt.RC4
import io.bluetape4k.jackson3.Jackson
import io.bluetape4k.jackson3.prettyWriteAsString
import io.bluetape4k.jackson3.writeAsString
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldNotContain
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import java.io.Serializable

@RandomizedTest
class JsonEncryptTest {

    companion object: KLogging() {
        private const val REPEAT_COUNT = 5
        private val faker = Fakers.faker
    }

    data class User(
        val username: String,

        @get:JsonEncrypt(encryptor = AES::class)
        val password: String,

        @get:JsonEncrypt(encryptor = RC4::class)
        val mobile: String,
    ): Serializable

    private val mapper: JsonMapper = Jackson.defaultJsonMapper

    private fun createUser(): User {
        return User(
            username = faker.name().name(),
            password = Base58.randomString(12),
            mobile = faker.phoneNumber().cellPhone()
        )
    }

    @Test
    fun `encrypt string property`() {
        val user = User(faker.name().name(), "mypassword", "010-8888-5555")
        log.debug { mapper.prettyWriteAsString(user) }

        val encrypted = mapper.writeAsString(user)!!
        encrypted shouldNotContain "mypassword"
        encrypted shouldNotContain "010-8888-5555"
    }

    @RepeatedTest(REPEAT_COUNT)
    fun `encrypt json property`() {
        verifyEncryptProperty(createUser())
    }

    @RepeatedTest(REPEAT_COUNT)
    fun `encrypt json property in list`() {
        val expected = List(20) { createUser() }
        verifyEncryptPropertyInCollection(expected)
    }

    @Test
    fun `encrypt json property in multi threadings`() {
        MultithreadingTester()
            .workers(2 * Runtimex.availableProcessors)
            .rounds(16)
            .add {
                verifyEncryptProperty(createUser())
            }
            .add {
                verifyEncryptPropertyInCollection(List(20) { createUser() })
            }
            .run()
    }

    @Test
    fun `encrypt json property in suspend jobs`() = runTest {
        SuspendedJobTester()
            .numThreads(2 * Runtimex.availableProcessors)
            .roundsPerJob(16 * 2 * Runtimex.availableProcessors)
            .add {
                verifyEncryptProperty(createUser())
            }
            .add {
                verifyEncryptPropertyInCollection(List(20) { createUser() })
            }
            .run()
    }

    @EnabledOnJre(JRE.JAVA_21)
    @Test
    fun `encrypt json property in virtual threads`() {
        StructuredTaskScopeTester()
            .rounds(16 * 2 * Runtimex.availableProcessors)
            .add {
                verifyEncryptProperty(createUser())
            }
            .add {
                verifyEncryptPropertyInCollection(List(20) { createUser() })
            }
            .run()
    }

    private fun verifyEncryptProperty(expected: User) {
        val encrypted = mapper.writeAsString(expected).shouldNotBeNull()

        val actual = mapper.readValue<User>(encrypted)
        actual shouldBeEqualTo expected
    }

    private fun verifyEncryptPropertyInCollection(expected: Collection<User>) {
        val encrypted = mapper.writeAsString(expected).shouldNotBeNull()

        val actual = mapper.readValue<List<User>>(encrypted)
        actual shouldBeEqualTo expected
    }
}
