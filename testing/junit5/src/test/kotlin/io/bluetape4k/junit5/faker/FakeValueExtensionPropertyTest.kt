package io.bluetape4k.junit5.faker

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
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
        fullName.shouldNotBeEmpty()
        firstName.shouldNotBeEmpty()
        lastName.shouldNotBeEmpty()

        log.trace { "fullName=$fullName" }
        log.trace { "firstName=$firstName" }
        log.trace { "lastName=$lastName" }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `inject fake value by random provider`(
        @FakeValue(provider = "number.randomDigit") intValue: Int,
        @FakeValue(provider = "number.randomDigitNotZero") nonZero: Int,
        @FakeValue(provider = "random.nextLong") longValue: Long,
        @FakeValue(provider = "random.nextDouble") doubleValue: Double,
    ) {
        nonZero shouldBeGreaterThan 0
        log.trace { "int value = $intValue" }
        log.trace { "long value = $longValue" }
        log.trace { "double value = $doubleValue" }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `inject fake credit card`(
        @FakeValue(provider = "finance.creditCard") creditCard: String,
        @FakeValue(provider = "finance.bic") bic: String,
    ) {
        creditCard.shouldNotBeEmpty()
        creditCard.length shouldBeGreaterOrEqualTo 8
        bic.shouldNotBeEmpty()

        log.trace { "creditCard=$creditCard" }
        log.trace { "bic=$bic" }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `inject multiple usernames`(
        @FakeValue(provider = FakeValueProvider.Name.Username, type = String::class, size = 20) usernames: List<String>,
    ) {
        usernames.size shouldBeEqualTo 20
        usernames.all { it.isNotBlank() }.shouldBeTrue()
    }
}
