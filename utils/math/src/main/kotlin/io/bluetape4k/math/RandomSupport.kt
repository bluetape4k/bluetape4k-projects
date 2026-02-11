package io.bluetape4k.math

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.collections.eclipse.toUnifiedMap
import io.bluetape4k.ranges.ClosedOpenRange
import io.bluetape4k.ranges.DefaultClosedOpenRange
import org.eclipse.collections.impl.list.mutable.FastList
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.Random

fun <T: Any> List<T>.randomFirst(): T =
    randomFirstOrNull() ?: throw NoSuchElementException("No elements found!")

fun <T: Any> List<T>.randomFirstOrNull(): T? {
    if (isEmpty()) return null
    val random = ThreadLocalRandom.current().nextInt(0, size)
    return this[random]
}

fun <T: Any> Sequence<T>.randomFirst(): T = toList().randomFirst()
fun <T: Any> Sequence<T>.randomFirstOrNull(): T? = toList().randomFirstOrNull()
fun <T: Any> Iterable<T>.randomFirst(): T = toList().randomFirst()
fun <T: Any> Iterable<T>.randomFirstOrNull(): T? = toList().randomFirstOrNull()

/**
 * `sampleSize` 만큼의 unique 한 random 요소를 반환한다
 */
fun <T: Any> List<T>.randomDistinct(sampleSize: Int): List<T> {
    if (isEmpty()) return emptyList()
    val cappedSampleSize = sampleSize.coerceIn(1, size)

    val random = ThreadLocalRandom.current()
    val result = FastList.newList<T>(sampleSize)
    return (0..Int.MAX_VALUE).asSequence()
        .map {
            random.nextInt(0, size)
        }
        .distinct()
        .take(cappedSampleSize)
        .map { this[it] }
        .toFastList(result)
}

fun <T: Any> Sequence<T>.randomDistinct(sampleSize: Int): List<T> = toList().randomDistinct(sampleSize)
fun <T: Any> Iterable<T>.randomDistinct(sampleSize: Int): List<T> = toList().randomDistinct(sampleSize)

/**
 * `sampleSize` 만큼의 random 요소를 반환한다
 */
fun <T: Any> List<T>.random(sampleSize: Int): List<T> {
    if (isEmpty()) return emptyList()
    val cappedSampleSize = sampleSize.coerceIn(1, size)

    val random = ThreadLocalRandom.current()
    return (0..Int.MAX_VALUE).asSequence()
        .map {
            random.nextInt(0, size)
        }
        .take(cappedSampleSize)
        .map { this[it] }
        .toFastList()
}

/**
 * `sampleSize` 만큼의 random 요소를 반환한다
 */
fun <T: Any> Sequence<T>.random(sampleSize: Int): List<T> = toList().random(sampleSize)

/**
 * `sampleSize` 만큼의 random 요소를 반환한다
 */
fun <T: Any> Iterable<T>.random(sampleSize: Int): List<T> = toList().random(sampleSize)

/**
 * Simulates a weighted TRUE/FALSE coin flip, with a percentage of probability towards TRUE
 * In other words, this is a Probability Density Function (PDF) for discrete TRUE/FALSE values
 */
class WeightedCoin(val trueProbability: Double) {

    init {
        assert(trueProbability in 0.0..1.0) {
            "trueProbability[$trueProbability] must be in 0.0 .. 1.0"
        }
    }

    fun flip(): Boolean = Random.nextDouble(0.0, 1.0) <= trueProbability
}

/**
 * Simulates a weighted TRUE/FALSE coin flip, with a percentage of probability towards TRUE
 * In other words, this is a Probability Density Function (PDF) for discrete TRUE/FALSE values
 */
fun weightedCoinFlip(trueProbability: Double): Boolean {
    assert(trueProbability in 0.0..1.0) { "trueProbability[$trueProbability] must be in 0.0 .. 1.0" }
    return ThreadLocalRandom.current().nextDouble(0.0, 1.0) <= trueProbability
}

/**
 *  Assigns a probabilty to each distinct `T` item, and randomly selects `T` values given those probabilities.
 *  In other words, this is a Probability Density Function (PDF) for discrete `T` values
 */
class WeightedDice<T: Any> private constructor(probabilities: Map<T, Double>) {

    companion object {
        operator fun <E: Any> invoke(vararg values: Pair<E, Double>): WeightedDice<E> {
            assert(values.isNotEmpty()) { "values is empty." }
            return WeightedDice(values.toUnifiedMap())
        }

        operator fun <E: Any> invoke(probabilities: Map<E, Double>): WeightedDice<E> {
            assert(probabilities.isNotEmpty()) { "probabilities is empty." }
            return WeightedDice(probabilities)
        }
    }

    private val sum: Double = probabilities.values.sum()

    private val rangedDistribution: Map<T, ClosedOpenRange<Double>> = probabilities.let { map ->
        var binStart = 0.0

        map.asSequence()
            .sortedBy { it.value }
            .map { it.key to DefaultClosedOpenRange(binStart, it.value + binStart) }
            .onEach { binStart = it.second.endExclusive }
            .toUnifiedMap()
    }

    fun roll(): T = Random.nextDouble(0.0, sum).let { rnd ->
        rangedDistribution
            .asSequence()
            .first { rng ->
                rnd in rng.value
            }
            .key
    }
}
