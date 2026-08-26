package io.bluetape4k.math

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.collections.repeat
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BigDecimalHistogramTest {

    companion object: KLogging()

    private val valueVector = sequenceOf(0.0, 1.0, 3.0, 5.0, 11.0).map { it.toBigDecimal() }
    private val groups = sequenceOf("A", "B", "B", "C", "C")

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
        val firstRange = histogram[5.0.toBigDecimal()].shouldNotBeNull().range
        firstRange.first shouldBeEqualTo 0.0.toBigDecimal()
        firstRange.last shouldBeEqualTo 100.0.toBigDecimal()

        val secondRange = histogram[105.0.toBigDecimal()].shouldNotBeNull().range
        secondRange.first shouldBeEqualTo 100.0.toBigDecimal()
        secondRange.last shouldBeEqualTo 200.0.toBigDecimal()

        val thirdRange = histogram[205.0.toBigDecimal()].shouldNotBeNull().range
        thirdRange.first shouldBeEqualTo 200.0.toBigDecimal()
        thirdRange.last shouldBeEqualTo 300.0.toBigDecimal()
    }

    @Test
    fun `binByBigDecimal rejects non progressing bin sizes`() {
        val values = listOf(BigDecimal.ZERO, BigDecimal.ONE)

        listOf(BigDecimal.ZERO, BigDecimal("-1.0")).forEach { binSize ->
            assertFailsWith<IllegalArgumentException> {
                values.binByBigDecimal(binSize = binSize, valueMapper = { it })
            }
        }
    }
}
