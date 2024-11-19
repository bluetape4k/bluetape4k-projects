package io.bluetape4k.math

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertTrue

class BigDecimalHistogramTest {

    companion object: KLogging()

    private val valueVector = sequenceOf(0.0, 1.0, 3.0, 5.0, 11.0).map { it.toBigDecimal() }
    private val groups = sequenceOf("A", "B", "B", "C", "C")

    private val grouped = groups.zip(valueVector)

    @Test
    fun `histogram by BigDecimal`() {
        val bins: Sequence<Pair<BigDecimal, String>> = sequenceOf(
            valueVector,
            valueVector.map { it + 100.0.toBigDecimal() },
            valueVector.map { it + 200.0.toBigDecimal() }
        ).flatMap { it }
            .zip(groups.repeat())

        log.debug { "bins=$bins" }

        val histogram: BinModel<List<Pair<BigDecimal, String>>, BigDecimal> = bins.binByBigDecimal(
            binSize = 100.0.toBigDecimal(),
            valueMapper = { it.first },
            rangeStart = 0.0.toBigDecimal()
        )
        log.debug { "bins=${histogram.bins}" }
        histogram.bins.size shouldBeEqualTo 3

        // range의 어떤 값이던 상관없다 (BinModel.get operator를 보라)
        assertTrue {
            histogram[5.0.toBigDecimal()]!!.range.let {
                it.first == 0.0.toBigDecimal() && it.last == 100.0.toBigDecimal()
            }
        }
        assertTrue {
            histogram[105.0.toBigDecimal()]!!.range.let {
                it.first == 100.0.toBigDecimal() && it.last == 200.0.toBigDecimal()
            }
        }
        assertTrue {
            histogram[205.0.toBigDecimal()]!!.range.let {
                it.first == 200.0.toBigDecimal() && it.last == 300.0.toBigDecimal()
            }
        }
    }

    private fun <T> Sequence<T>.repeat(): Sequence<T> = sequence {
        while (true) {
            yieldAll(this@repeat)
        }
    }
}
