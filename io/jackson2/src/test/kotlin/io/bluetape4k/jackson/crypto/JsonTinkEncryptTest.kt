package io.bluetape4k.jackson.crypto

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.bluetape4k.codec.Base58
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.jackson.prettyWriteAsString
import io.bluetape4k.jackson.writeAsString
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
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE
import java.io.Serializable

@RandomizedTest
class JsonTinkEncryptTest {

    companion object: KLogging() {
        private const val REPEAT_COUNT = 5
        private val faker = Fakers.faker
    }

    data class User(
        val username: String,

        @field:JsonTinkEncrypt(algorithm = TinkEncryptAlgorithm.AES256_GCM)
        val password: String,

        @get:JsonTinkEncrypt(algorithm = TinkEncryptAlgorithm.DETERMINISTIC_AES256_SIV)
        val mobile: String,
    ): Serializable

    private val mapper: JsonMapper = Jackson.defaultJsonMapper

    private fun createUser(): User = User(
        username = faker.name().name(),
        password = Base58.randomString(8),
        mobile = faker.phoneNumber().cellPhone()
    )

    @Test
    fun `Tink 암호화 - 문자열 필드가 암호화되어 직렬화된다`() {
        val user = User(faker.name().name(), "mypassword", "010-5555-5555")
        log.debug { mapper.prettyWriteAsString(user) }

        val encrypted = mapper.writeAsString(user).shouldNotBeNull()
        encrypted shouldNotContain "mypassword"
        encrypted shouldNotContain "010-5555-5555"
    }

    @RepeatedTest(REPEAT_COUNT)
    fun `Tink 암호화 - 라운드트립 직렬화 복호화가 동작한다`() {
        val expected = createUser()
        verifyEncryptProperty(expected)
    }

    @RepeatedTest(REPEAT_COUNT)
    fun `Tink 암호화 - 컬렉션 라운드트립이 동작한다`() {
        val expected = List(10) { createUser() }
        verifyEncryptPropertyInCollection(expected)
    }

    @Test
    fun `Tink 암호화 - 멀티스레드 환경에서 동작한다`() {
        MultithreadingTester()
            .workers(Runtimex.availableProcessors)
            .rounds(4)
            .add {
                verifyEncryptProperty(createUser())
            }
            .add {
                verifyEncryptPropertyInCollection(List(10) { createUser() })
            }
            .run()
    }

    @Test
    fun `Tink 암호화 - 코루틴 suspend job 환경에서 동작한다`() = runTest {
        SuspendedJobTester()
            .workers(Runtimex.availableProcessors)
            .rounds(4 * Runtimex.availableProcessors)
            .add {
                verifyEncryptProperty(createUser())
            }
            .add {
                verifyEncryptPropertyInCollection(List(10) { createUser() })
            }
            .run()
    }

    @EnabledForJreRange(min = JRE.JAVA_21)
    @Test
    fun `Tink 암호화 - 가상 스레드 환경에서 동작한다`() {
        StructuredTaskScopeTester()
            .rounds(4 * Runtimex.availableProcessors)
            .add {
                verifyEncryptProperty(createUser())
            }
            .add {
                verifyEncryptPropertyInCollection(List(10) { createUser() })
            }
            .run()
    }

    private fun verifyEncryptProperty(expected: User) {
        val encrypted = mapper.writeValueAsString(expected)

        val actual = mapper.readValue<User>(encrypted)
        actual shouldBeEqualTo expected
    }

    private fun verifyEncryptPropertyInCollection(expected: Collection<User>) {
        val encrypted = mapper.writeValueAsString(expected)

        val actual = mapper.readValue<List<User>>(encrypted)
        actual shouldBeEqualTo expected
    }
}
