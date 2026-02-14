package io.bluetape4k.jackson.mask

import com.fasterxml.jackson.databind.json.JsonMapper
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.jackson.prettyWriteAsString
import io.bluetape4k.jackson.writeAsString
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import java.io.Serializable
import java.math.BigDecimal

@RandomizedTest
class JsonMaskerTest {

    companion object: KLogging() {
        private const val REPEAT_COUNT = 5
        private val faker = Fakers.faker
    }

    private val mapper: JsonMapper = Jackson.defaultJsonMapper

    @Test
    fun `masking field with @JsonMasker`() {
        val user = User("debop", "010-5555-5555", BigDecimal.TEN)
        log.debug { mapper.prettyWriteAsString(user) }
        verifyJsonMasker(user)
    }

    @RepeatedTest(REPEAT_COUNT)
    fun `masking field with @JsonMasker with random object`(@RandomValue user: User) {
        log.debug { mapper.prettyWriteAsString(user) }
        verifyJsonMasker(user)
    }

    @Test
    fun `masking filed with @JsonMasker in multi threadings`() {
        MultithreadingTester()
            .workers(2 * Runtimex.availableProcessors)
            .rounds(16)
            .add {
                verifyJsonMasker(newUser())
            }
            .run()
    }

    @Test
    fun `masking field with @JsonMasker in suspended jobs`() = runTest {
        SuspendedJobTester()
            .workers(2 * Runtimex.availableProcessors)
            .rounds(16 * 2 * Runtimex.availableProcessors)
            .add {
                verifyJsonMasker(newUser())
            }
            .run()
    }

    @EnabledOnJre(JRE.JAVA_21)
    @Test
    fun `masking field with @JsonMasker in virtual threads`() {
        StructuredTaskScopeTester()
            .rounds(16 * 2 * Runtimex.availableProcessors)
            .add {
                verifyJsonMasker(newUser())
            }
            .run()
    }

    private fun verifyJsonMasker(user: User) {
        val jsonText = mapper.writeAsString(user).shouldNotBeNull()
        jsonText shouldContain "masked: personal information"   // mobile
        jsonText shouldContain JsonMasker.DEFAULT_MASKED_STRING  // salary
    }

    private fun newUser(): User = User(
        name = faker.name().fullName(),
        mobile = faker.phoneNumber().cellPhone(),
        salary = BigDecimal(faker.number().randomDouble(2, 1000, 100000)),
        rsu = faker.number().randomNumber(5),
        address = faker.address().fullAddress()
    )

    data class User(
        val name: String,

        @get:JsonMasker("masked: personal information")
        val mobile: String,

        @get:JsonMasker
        val salary: BigDecimal,

        @get:JsonMasker("masked: confidential information")
        val rsu: Long = 0L,

        @get:JsonMasker
        val address: String? = null,
    ): Serializable


}
