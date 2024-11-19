package io.bluetape4k.math.commons

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class NormTest {

    companion object: KLogging()

    @Test
    fun `norm for double array`() {
        val valueArray = doubleArrayOf(-1.0, 0.0, 1.0, 2.0, 3.0)
        valueArray.norm() shouldBeEqualTo 15.0
    }

    @Test
    fun `norm for double sequence`() {
        val valueArray = sequenceOf(-1.0, 0.0, 1.0, 2.0, 3.0)
        valueArray.norm() shouldBeEqualTo 15.0
    }
}
