package io.bluetape4k.math.commons

import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.RepeatedTest
import kotlin.random.Random

class CorrelationTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
        private const val SAMPLE_SIZE = 1000
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `calc correlation with one sequence`() {
        val values = fastList(SAMPLE_SIZE) { Random.nextDouble(-10.0, 10.0) }
        val coefficient = values.correlationCoefficient(values)
        log.debug { "coefficient=$coefficient" }
        coefficient.approximateEqual(1.0, 1e-10).shouldBeTrue()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `calc correlation with sequences that has different size`() {
        val values = fastList(SAMPLE_SIZE) { Random.nextDouble(-10.0, 10.0) }
        val firstHalf = values.take(values.size / 2)

        val coefficient = values.correlationCoefficient(firstHalf)
        log.debug { "coefficient=$coefficient" }
        coefficient.approximateEqual(1.0, 1e-1).shouldBeTrue()
        coefficient.approximateEqual(1.0, 1e-10).shouldBeFalse()

        val lastHalf = values.drop(values.size / 2)

        val coefficient2 = values.correlationCoefficient(lastHalf)
        log.debug { "coefficient2=$coefficient2" }
        coefficient2.approximateEqual(1.0, 1e-10).shouldBeFalse()
    }
}
