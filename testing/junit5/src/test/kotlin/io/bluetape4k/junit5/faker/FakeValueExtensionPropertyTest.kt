package io.bluetape4k.junit5.faker

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.TestInstance

@FakeValueTest
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class FakeValueExtensionPropertyTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `inject fake value by provider`(
        @FakeValue(provider = FakeValueProvider.Name.FullName) fullName: String,
        @FakeValue(provider = FakeValueProvider.Name.FirstName) firstName: String,
        @FakeValue(provider = FakeValueProvider.Name.LastName) lastName: String,
    ) {
        log.debug { "fullName=$fullName" }
        log.debug { "firstName=$firstName" }
        log.debug { "lastName=$lastName" }

        fullName.shouldNotBeEmpty()
        firstName.shouldNotBeEmpty()
        lastName.shouldNotBeEmpty()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `inject fake value by random provider`(
        @FakeValue(provider = "number.randomDigit") intValue: Int,
        @FakeValue(provider = "number.randomDigitNotZero") nonZero: Int,
        @FakeValue(provider = "random.nextLong") longValue: Long,
        @FakeValue(provider = "random.nextDouble") doubleValue: Double,
    ) {
        log.debug { "int value = $intValue" }
        log.debug { "long value = $longValue" }
        log.debug { "double value = $doubleValue" }

        nonZero shouldBeGreaterThan 0
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `inject fake credit card`(
        @FakeValue(provider = "finance.creditCard") creditCard: String,
        @FakeValue(provider = "finance.bic") bic: String,
    ) {
        log.debug { "creditCard=$creditCard" }
        log.debug { "bic=$bic" }

        creditCard.shouldNotBeEmpty()
        creditCard.length shouldBeGreaterOrEqualTo 8
        bic.shouldNotBeEmpty()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `inject multiple usernames`(
        @FakeValue(provider = FakeValueProvider.Name.Username, type = String::class, size = 20) usernames: List<String>,
    ) {
        usernames.size shouldBeEqualTo 20
        usernames.all { it.isNotBlank() }.shouldBeTrue()
    }
}
