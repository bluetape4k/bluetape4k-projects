package io.bluetape4k.math.commons

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.assertions.shouldBeEqualTo
import org.apache.commons.math3.stat.ranking.NaNStrategy
import org.apache.commons.math3.stat.ranking.NaturalRanking
import org.apache.commons.math3.stat.ranking.TiesStrategy
import org.junit.jupiter.api.Test

class RankingTest {

    companion object: KLogging()

    private fun DoubleArray.ranking(): Map<Double, Int> {
        val ranks = NaturalRanking(NaNStrategy.MINIMAL, TiesStrategy.MAXIMUM).rank(this)
        return this
            .mapIndexed { index, value ->
                value to (ranks.size - ranks[index]).toInt()
            }
            .toMap()
    }

    @Test
    fun `ranking double array`() {
        val scores = doubleArrayOf(
            20.0, 17.0, 30.0, 42.3, 17.0, 50.0,
            Double.NaN, Double.NEGATIVE_INFINITY, 17.0, Double.POSITIVE_INFINITY
        )

        val scoreAndRanks = scores.ranking()

        scoreAndRanks[Double.POSITIVE_INFINITY]!! shouldBeEqualTo 0
        scoreAndRanks[50.0]!! shouldBeEqualTo 1
        scoreAndRanks[20.0]!! shouldBeEqualTo 4
        scoreAndRanks[Double.NaN]!! shouldBeEqualTo 8
        scoreAndRanks[Double.NEGATIVE_INFINITY]!! shouldBeEqualTo 8
    }

    @Test
    fun `ranking double sequence`() {
        val scores = sequenceOf(
            20.0, 17.0, 30.0, 42.3, 17.0, 50.0,
            Double.NaN, Double.NEGATIVE_INFINITY, 17.0, Double.POSITIVE_INFINITY
        )

        val scoreAndRanks = scores.ranking()
        scoreAndRanks.forEach {
            log.trace { "score=${it.key}, rank=${it.value}" }
        }
        scoreAndRanks[Double.POSITIVE_INFINITY]!! shouldBeEqualTo 0
        scoreAndRanks[50.0]!! shouldBeEqualTo 1
        scoreAndRanks[20.0]!! shouldBeEqualTo 4
        scoreAndRanks[Double.NaN]!! shouldBeEqualTo 8
        scoreAndRanks[Double.NEGATIVE_INFINITY]!! shouldBeEqualTo 8
    }

    @Test
    fun `ranking object value`() {
        val scores = sequenceOf(20.0, 17.0, 30.0, 42.3, 17.0, 50.0)
        val objects = scores.map { it.toBigDecimal() }
        val scoreAndRanks = objects.ranking { it.toDouble() }

        scoreAndRanks.forEach {
            log.trace { "score=${it.key}, rank=${it.value}" }
        }
        scoreAndRanks[50.0.toBigDecimal()]!! shouldBeEqualTo 0
        scoreAndRanks[20.0.toBigDecimal()]!! shouldBeEqualTo 3
    }

    // ----- Iterable.ranking -----

    @Test
    fun `ranking iterable of doubles`() {
        val scores = listOf(
            20.0, 17.0, 30.0, 42.3, 17.0, 50.0,
            Double.NaN, Double.NEGATIVE_INFINITY, 17.0, Double.POSITIVE_INFINITY
        )

        val scoreAndRanks = scores.ranking()
        scoreAndRanks.forEach {
            log.trace { "score=${it.key}, rank=${it.value}" }
        }

        scoreAndRanks[Double.POSITIVE_INFINITY]!! shouldBeEqualTo 0
        scoreAndRanks[50.0]!! shouldBeEqualTo 1
        scoreAndRanks[20.0]!! shouldBeEqualTo 4
    }

    @Test
    fun `ranking iterable object by value selector`() {
        data class Student(val name: String, val score: Double)

        val students = listOf(
            Student("Alice", 90.0),
            Student("Bob", 70.0),
            Student("Charlie", 80.0),
        )

        val rankMap = students.ranking { it.score }
        rankMap.forEach {
            log.trace { "student=${it.key.name}, rank=${it.value}" }
        }

        rankMap[students[0]] shouldBeEqualTo 0  // Alice (90) - 1위
        rankMap[students[1]] shouldBeEqualTo 2  // Bob (70) - 3위
        rankMap[students[2]] shouldBeEqualTo 1  // Charlie (80) - 2위
    }
}
