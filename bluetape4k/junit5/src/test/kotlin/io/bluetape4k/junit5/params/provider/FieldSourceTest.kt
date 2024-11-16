package io.bluetape4k.junit5.params.provider

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.params.ParameterizedTest

class FieldSourceTest {

    val arguments = listOf(
        argumentOf(null, true),
        argumentOf("", true),
        argumentOf(" ", true),
        argumentOf("not blank", false)
    )

    @ParameterizedTest(name = "isBlank should return `{1}` for `{0}`")
    @FieldSource("arguments")
    fun `isBlank should return true for null or blank string variable`(input: String?, expected: Boolean) {
        input.isNullOrBlank() shouldBeEqualTo expected
    }

    @ParameterizedTest(name = "isBlank should return `{1}` for `{0}`")
    @FieldSource("arguments")
    fun `isBlank should return true for null or blank string variable 2`(input: String?, expected: Boolean) {
        input.isNullOrBlank() shouldBeEqualTo expected
    }
}
