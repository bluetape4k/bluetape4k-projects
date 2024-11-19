package io.bluetape4k.math

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.math.model.Gender.FEMALE
import io.bluetape4k.math.model.Gender.MALE
import io.bluetape4k.math.model.Item
import io.bluetape4k.math.model.Patient
import io.bluetape4k.math.model.SaleDate
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertTrue

class NumberStatisticsTest {

    companion object: KLogging()

    private val doubleVector = sequenceOf(1.0, 3.0, 5.0, 11.0)
    private val intVector = doubleVector.map { it.toInt() }
    private val longVector = doubleVector.map { it.toLong() }
    private val bigDecimalVector = doubleVector.map { it.toBigDecimal() }

    private val vectors: Sequence<Sequence<Number>> = sequenceOf(doubleVector, intVector, longVector, bigDecimalVector)
    private val groups = sequenceOf("A", "A", "B", "B")

    @Test
    fun `verify descriptiveStatistics`() {
        vectors.forEach {
            with(it.descriptiveStatistics()) {
                mean shouldBeEqualTo 5.0
                min shouldBeEqualTo 1.0
                max shouldBeEqualTo 11.0
                size shouldBeEqualTo 4
            }
        }
    }

    @Test
    fun `verify descriptiveBy`() {
        vectors.forEach { vector ->
            groups.zip(vector).descriptiveStatisticsBy().let {
                assertTrue { it["A"]!!.mean == 2.0 && it["B"]!!.mean == 8.0 }
            }

            val ziped = groups.zip(vector).descriptiveStatisticsBy(
                keySelector = { it.first },
                valueMapper = { it.second }
            )
            // assertTrue { it["A"]!!.mean == 2.0 && it["B"]!!.mean == 8.0 }
            ziped["A"]!!.mean shouldBeEqualTo 2.0
            ziped["B"]!!.mean shouldBeEqualTo 8.0
        }
    }

    @Test
    fun `geometric for vector`() {
        vectors.forEach { vector ->
            vector.geometricMean() shouldBeEqualTo 3.584024634215721
        }
    }

    @Test
    fun `median for vector`() {
        vectors.forEach { vector ->
            vector.median() shouldBeEqualTo 4.0
        }

        doubleVector.take(3).median() shouldBeEqualTo 3.0
    }

    @Test
    fun `medianBy for vector`() {
        vectors.forEach { vector ->
            groups.zip(vector).medianBy().let {
                assertTrue { it["A"]!! == 2.0 && it["B"]!! == 8.0 }
            }

            val ziped = groups.zip(vector).medianBy(
                { it.first },
                { it.second }
            )
            ziped["A"] shouldBeEqualTo 2.0
            ziped["B"] shouldBeEqualTo 8.0
        }
    }

    @Test
    fun `percentile for vector`() {
        vectors.forEach { vector ->
            vector.percentile(50.0) shouldBeEqualTo 4.0
        }
    }

    private val list = listOf(
        Item("Alpha", 4.0),
        Item("Beta", 6.0),
        Item("Gamma", 7.2),
        Item("Delta", 9.2),
        Item("Epsilon", 6.8),
        Item("Zeta", 2.4),
        Item("Iota", 8.8)
    )

    @Test
    fun `sumBy for object`() {
        val sumByLengths = list.sumBy(keySelector = { it.name.length }, valueMapper = { it.value })
        log.debug { "Sums by lengths: $sumByLengths" }
        sumByLengths.keys shouldBeEqualTo setOf(4, 5, 7)
    }

    @Test
    fun `averageBy for object`() {
        val avgByLength = list.averageBy(keySelector = { it.name.length }, valueMapper = { it.value })
        log.debug { "Average by lengths: $avgByLength" }
        avgByLength.keys shouldBeEqualTo setOf(4, 5, 7)
    }

    @Test
    fun `stdevBy for object`() {
        val avgByLength = list.stdevBy(keySelector = { it.name.length }, valueMapper = { it.value })
        log.debug { "Average by lengths: $avgByLength" }
        avgByLength.keys shouldBeEqualTo setOf(4, 5, 7)
    }

    @Test
    fun `variance for vector`() {
        val expected = 18.666666666666668

        vectors.forEach { vector ->
            vector.variance() shouldBeEqualTo expected
        }
    }

    @Test
    fun `sumOfSquares for vector`() {
        val expected = 156.0

        vectors.forEach { vector ->
            vector.sumOfSquares() shouldBeEqualTo expected
        }
    }

    @Test
    fun `stdev for vector`() {
        val expected = 4.320493798938574

        vectors.forEach { vector ->
            vector.stdev() shouldBeEqualTo expected
        }
    }

    @Test
    fun `stdevBy for vector`() {
        val expected = mapOf("A" to 1.4142135623730951, "B" to 4.242640687119285)

        vectors.forEach { vector ->
            groups.zip(vector).stdevBy() shouldBeEqualTo expected

            val stdev = groups.zip(vector).stdevBy(
                { it.first },
                { it.second },
            )
            stdev shouldBeEqualTo expected
        }
    }

    @Test
    fun `normalize for vector`() {
        val expected = doubleArrayOf(
            -0.9258200997725514,
            -0.4629100498862757,
            0.0,
            1.3887301496588271
        )

        vectors.forEach { vector ->
            vector.normalize() shouldBeEqualTo expected
        }
    }

    @Test
    fun `kurtosis for vector`() {
        val expected = 1.4999999999999947
        vectors.forEach { vector ->
            vector.kurtosis() shouldBeEqualTo expected
        }
    }

    @Test
    fun `skewness for vector`() {
        val expected = 1.1903401282789945
        vectors.forEach { vector ->
            vector.skewness() shouldBeEqualTo expected
        }
    }

    @Test
    fun `normalizeBy for vector`() {
        val expected = mapOf(
            "A" to doubleArrayOf(-0.7071067811865475, 0.7071067811865475),
            "B" to doubleArrayOf(-0.7071067811865476, 0.7071067811865476)
        )

        vectors.forEach { vector ->
            val norm = groups.zip(vector).normalizeBy()
            norm["A"]!! shouldBeEqualTo expected["A"]!!
            norm["B"]!! shouldBeEqualTo expected["B"]!!
        }
    }

    @Test
    fun `geometricMeanBy for vector`() {
        val expected = mapOf("A" to 1.7320508075688774, "B" to 7.416198487095664)

        vectors.forEach { vector ->
            groups.zip(vector).geometricMeanBy() shouldBeEqualTo expected

            val ziped = groups.zip(vector).geometricMeanBy(
                { it.first },
                { it.second },
            )
            ziped shouldBeEqualTo expected
        }
    }

    @Test
    fun `simple regression`() {
        doubleVector.zip(doubleVector.map { it * 2 })
            .simpleRegression().slope shouldBeEqualTo 2.0

        intVector.zip(intVector.map { it * 2 })
            .simpleRegression().slope shouldBeEqualTo 2.0

        longVector.zip(longVector.map { it * 2 })
            .simpleRegression().slope shouldBeEqualTo 2.0

        bigDecimalVector.zip(bigDecimalVector.map { it * 2.toBigDecimal() })
            .simpleRegression().slope shouldBeEqualTo 2.0

        doubleVector.zip(intVector.map { it * 2 })
            .simpleRegression(
                xSelector = { it.first },
                ySelector = { it.second }
            ).slope shouldBeEqualTo 2.0
    }


    @Test
    fun `class data regression`() {

        val salesDates = listOf(
            SaleDate(LocalDate.of(2017, 1, 1), 1080),
            SaleDate(LocalDate.of(2017, 1, 2), 2010),
            SaleDate(LocalDate.of(2017, 1, 3), 1020),
            SaleDate(LocalDate.of(2017, 1, 4), 907),
            SaleDate(LocalDate.of(2017, 1, 5), 805),
            SaleDate(LocalDate.of(2017, 1, 6), 2809),
            SaleDate(LocalDate.of(2017, 1, 7), 2600)
        )

        val regression = salesDates.simpleRegression(
            xSelector = { it.date.dayOfYear.toDouble() },
            ySelector = { it.sales.toDouble() }
        )
        log.debug { "slope=${regression.slope}" }
    }


    @Test
    fun `percentile by extension methods`() {
        val patients = listOf(
            Patient("John", "Simone", MALE, LocalDate.of(1989, 1, 7), 4500),
            Patient("Sarah", "Marley", FEMALE, LocalDate.of(1970, 2, 5), 6700),
            Patient("Jessica", "Arnold", FEMALE, LocalDate.of(1980, 3, 9), 3400),
            Patient("Sam", "Beasley", MALE, LocalDate.of(1981, 4, 17), 8800),
            Patient("Dan", "Forney", MALE, LocalDate.of(1985, 9, 13), 5400),
            Patient("Lauren", "Michaels", FEMALE, LocalDate.of(1975, 8, 21), 5000),
            Patient("Michael", "Erlich", MALE, LocalDate.of(1985, 12, 17), 4100),
            Patient("Jason", "Miles", MALE, LocalDate.of(1991, 11, 1), 3900),
            Patient("Rebekah", "Earley", FEMALE, LocalDate.of(1985, 2, 18), 4600),
            Patient("James", "Larson", MALE, LocalDate.of(1974, 4, 10), 5100),
            Patient("Dan", "Ulrech", MALE, LocalDate.of(1991, 7, 11), 6000),
            Patient("Heather", "Eisner", FEMALE, LocalDate.of(1994, 3, 6), 6000),
            Patient("Jasper", "Martin", MALE, LocalDate.of(1971, 7, 1), 6000)
        )

        fun Collection<Patient>.wbccPercentileByGender(percentile: Double) =
            percentileBy(
                percentile = percentile,
                keySelector = { it.gender },
                valueMapper = { it.whiteBloodCellCount.toDouble() })

        val percentiles = listOf(1.0, 25.0, 50.0, 75.0, 95.0, 99.0, 100.0)

        val percentileByGender = percentiles.map { it to patients.wbccPercentileByGender(it) }

        percentileByGender.forEach {
            log.debug { it }
        }
    }
}
