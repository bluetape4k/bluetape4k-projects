package io.bluetape4k.math

import io.bluetape4k.collections.repeat
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.ranges.toClosedClosedRange
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class DoubleHistogramTest {

    companion object: KLogging()

    private val valueVector = sequenceOf(0.0, 1.0, 3.0, 5.0, 11.0)
    private val groups = sequenceOf("A", "B", "B", "C", "C")
    private val grouped = groups.zip(valueVector)


    @Test
    fun `histogram by double`() {
        val bins: Sequence<Pair<Double, String>> = sequenceOf(
            valueVector,
            valueVector.map { it + 100.0 },
            valueVector.map { it + 200.0 }
        )
            .flatMap { it }
            .zip(groups.repeat())

        log.trace { "bins=${bins.joinToString()}" }


        val histogram: BinModel<List<Pair<Double, String>>, Double> = bins.binByDouble(
            binSize = 100.0,
            valueMapper = { it.first },
            rangeStart = 0.0
        )
        histogram.bins.forEach { bin ->
            log.trace { "bin=$bin" }
        }
        histogram.bins.size shouldBeEqualTo 3

        // range의 어떤 값이던 상관없다 (BinModel.get operator를 보라)
        histogram[5.0]!!.range shouldBeEqualTo (0.0..100.0).toClosedClosedRange()
        histogram[105.0]!!.range shouldBeEqualTo (100.0..200.0).toClosedClosedRange()
        histogram[205.0]!!.range shouldBeEqualTo (200.0..300.0).toClosedClosedRange()
    }
}
