package io.bluetape4k.spring.util

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.text.NumberFormat

class NumberUtilsSupportTest {

    companion object: KLogging()

    @Test
    fun `parse string to number`() {
        "123".parseNumber<Int>() shouldBeEqualTo 123
        "123".parseNumber<Long>() shouldBeEqualTo 123L
        "123.4".parseNumber<Float>() shouldBeEqualTo 123.4F
        "123.4".parseNumber<Double>() shouldBeEqualTo 123.4
    }

    @Test
    fun `parse string to number with number format`() {
        "123.4".parseNumber<Int>(NumberFormat.getNumberInstance()) shouldBeEqualTo 123
        "123.4".parseNumber<Long>(NumberFormat.getNumberInstance()) shouldBeEqualTo 123L
        "123.4".parseNumber<Float>(NumberFormat.getNumberInstance()) shouldBeEqualTo 123.4F
        "123.4".parseNumber<Double>(NumberFormat.getNumberInstance()) shouldBeEqualTo 123.4
    }

    @Test
    fun `convert other number format`() {
        123.convertAs<Long>() shouldBeEqualTo 123L
        123.4.convertAs<Long>() shouldBeEqualTo 123L
        123.convertAs<Double>() shouldBeEqualTo 123.0
    }
}
