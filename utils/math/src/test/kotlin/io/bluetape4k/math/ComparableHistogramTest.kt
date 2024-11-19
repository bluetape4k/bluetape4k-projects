package io.bluetape4k.math

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.ranges.DefaultClosedClosedRange
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

class ComparableHistogramTest {

    companion object: KLogging()

    private val valueVector = sequenceOf(0.0, 1.0, 3.0, 5.0, 11.0)
    private val groups = sequenceOf("A", "B", "B", "C", "C")

    private val grouped = groups.zip(valueVector)

    @Test
    fun `histogram by comparable number`() {
        val bins = sequenceOf(
            valueVector,
            valueVector.map { it + 100.0 },
            valueVector.map { it + 200.0 }
        ).flatMap { it }
            .zip(groups.repeat())

        log.debug { "bins=$bins" }

        val histogram = bins.binByComparable(
            incrementer = { it + 100.0 },
            valueMapper = { it.first },
            rangeStart = 0.0
        )
        log.debug { "bins=${histogram.bins}" }
        histogram.bins.size shouldBeEqualTo 3

        // range의 어떤 값이던 상관없다 (BinModel.get operator를 보라)
        histogram[5.0]!!.range shouldBeEqualTo DefaultClosedClosedRange(0.0, 100.0)
        histogram[105.0]!!.range shouldBeEqualTo DefaultClosedClosedRange(100.0, 200.0)
        histogram[205.0]!!.range shouldBeEqualTo DefaultClosedClosedRange(200.0, 300.0)

    }

    private fun <T> Sequence<T>.repeat(): Sequence<T> = sequence {
        while (true) {
            yieldAll(this@repeat)
        }
    }

    data class Sale(val accountId: Int, val date: LocalDate, val value: Double)

    @Test
    fun `slicing by ranges and bins by quarter`() {
        val sales = listOf(
            Sale(1, LocalDate.of(2016, 12, 3), 180.0),
            Sale(2, LocalDate.of(2016, 7, 4), 140.2),
            Sale(3, LocalDate.of(2016, 6, 3), 111.4),
            Sale(4, LocalDate.of(2016, 1, 5), 192.7),
            Sale(5, LocalDate.of(2016, 5, 4), 137.9),
            Sale(6, LocalDate.of(2016, 3, 6), 125.6),
            Sale(7, LocalDate.of(2016, 12, 4), 164.3),
            Sale(8, LocalDate.of(2016, 7, 11), 144.2)
        )

        // Histogram 을 만든다
        val byQuarter: BinModel<List<Sale>, Month> = sales.binByComparable(
            incrementer = { it + 1 },
            valueMapper = { it.date.month }
        )
        byQuarter.forEach {
            log.debug { it }
        }

        byQuarter[Month.MAY]!!.value shouldBeEqualTo listOf(sales[4])
    }

    @Test
    fun `slicing by ranges and bins by quarter and sum`() {
        val sales = listOf(
            Sale(1, LocalDate.of(2016, 12, 3), 180.0),
            Sale(2, LocalDate.of(2016, 7, 4), 140.2),
            Sale(3, LocalDate.of(2016, 6, 3), 111.4),
            Sale(4, LocalDate.of(2016, 1, 5), 192.7),
            Sale(5, LocalDate.of(2016, 5, 4), 137.9),
            Sale(6, LocalDate.of(2016, 3, 6), 125.6),
            Sale(7, LocalDate.of(2016, 12, 4), 164.3),
            Sale(8, LocalDate.of(2016, 7, 11), 144.2)
        )

        // Histogram 을 만든다
        val byQuarter: BinModel<Double, Month> = sales.binByComparable(
            incrementer = { it + 1L },
            valueMapper = { it.date.month },
            groupOp = { list -> list.asSequence().map { it.value }.sum() }
        )
        byQuarter.forEach { log.trace { it } }
        byQuarter[Month.MAY]!!.value shouldBeEqualTo 137.9
    }

    @Test
    fun `bin by double ranges`() {
        val sales = listOf(
            Sale(1, LocalDate.of(2016, 12, 3), 180.0),
            Sale(2, LocalDate.of(2016, 7, 4), 140.2),
            Sale(3, LocalDate.of(2016, 6, 3), 111.4),
            Sale(4, LocalDate.of(2016, 1, 5), 192.7),
            Sale(5, LocalDate.of(2016, 5, 4), 137.9),
            Sale(6, LocalDate.of(2016, 3, 6), 125.6),
            Sale(7, LocalDate.of(2016, 12, 4), 164.3),
            Sale(8, LocalDate.of(2016, 7, 11), 144.2)
        )

        // Histogram 을 만든다
        val binned: BinModel<List<Sale>, Double> = sales.binByDouble(
            binSize = 20.0,
            valueMapper = { it.value },
            rangeStart = 100.0
        )
        binned.forEach { log.trace { it } }
        binned[110.0]!!.value shouldContain sales[2]
    }
}
