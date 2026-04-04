package io.bluetape4k.math

import io.bluetape4k.ranges.ClosedOpenRange
import io.bluetape4k.ranges.DefaultClosedOpenRange
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.Random

/**
 * 리스트에서 랜덤으로 하나의 요소를 반환합니다. 요소가 없으면 예외를 발생합니다.
 *
 * ```kotlin
 * val result = listOf(1, 2, 3).randomFirst()   // 랜덤 요소 (1, 2 또는 3)
 * ```
 */
fun <T: Any> List<T>.randomFirst(): T =
    randomFirstOrNull() ?: throw NoSuchElementException("No elements found!")

/**
 * 리스트에서 랜덤으로 하나의 요소를 반환합니다. 요소가 없으면 null을 반환합니다.
 *
 * ```kotlin
 * val result = listOf(1, 2, 3).randomFirstOrNull()   // 랜덤 요소 또는 null
 * val nullResult = emptyList<Int>().randomFirstOrNull()   // null
 * ```
 */
fun <T: Any> List<T>.randomFirstOrNull(): T? {
    if (isEmpty()) return null
    val random = ThreadLocalRandom.current().nextInt(0, size)
    return this[random]
}

/**
 * 시퀀스에서 랜덤으로 하나의 요소를 반환합니다. 요소가 없으면 예외를 발생합니다.
 *
 * ```kotlin
 * val result = sequenceOf(1, 2, 3).randomFirst()   // 랜덤 요소
 * ```
 */
fun <T: Any> Sequence<T>.randomFirst(): T = toList().randomFirst()

/**
 * 시퀀스에서 랜덤으로 하나의 요소를 반환합니다. 요소가 없으면 null을 반환합니다.
 *
 * ```kotlin
 * val result = sequenceOf(1, 2, 3).randomFirstOrNull()   // 랜덤 요소 또는 null
 * ```
 */
fun <T: Any> Sequence<T>.randomFirstOrNull(): T? = toList().randomFirstOrNull()

/**
 * Iterable에서 랜덤으로 하나의 요소를 반환합니다. 요소가 없으면 예외를 발생합니다.
 *
 * ```kotlin
 * val result = listOf(1, 2, 3).randomFirst()   // 랜덤 요소
 * ```
 */
fun <T: Any> Iterable<T>.randomFirst(): T = toList().randomFirst()

/**
 * Iterable에서 랜덤으로 하나의 요소를 반환합니다. 요소가 없으면 null을 반환합니다.
 *
 * ```kotlin
 * val result = listOf(1, 2, 3).randomFirstOrNull()   // 랜덤 요소 또는 null
 * ```
 */
fun <T: Any> Iterable<T>.randomFirstOrNull(): T? = toList().randomFirstOrNull()

/**
 * `sampleSize` 만큼의 unique 한 random 요소를 반환한다
 *
 * ```kotlin
 * val result = listOf(1, 2, 3, 4, 5).randomDistinct(3)
 * // 중복 없이 3개의 랜덤 요소 (예: [2, 4, 1])
 * ```
 */
fun <T: Any> List<T>.randomDistinct(sampleSize: Int): List<T> {
    if (isEmpty()) return emptyList()
    val cappedSampleSize = sampleSize.coerceIn(1, size)

    val random = ThreadLocalRandom.current()
    return (0..Int.MAX_VALUE).asSequence()
        .map {
            random.nextInt(0, size)
        }
        .distinct()
        .take(cappedSampleSize)
        .map { this[it] }
        .toList()
}

/**
 * 시퀀스에서 `sampleSize` 만큼의 unique 한 random 요소를 반환한다
 *
 * ```kotlin
 * val result = sequenceOf(1, 2, 3, 4, 5).randomDistinct(3)
 * // 중복 없이 3개의 랜덤 요소
 * ```
 */
fun <T: Any> Sequence<T>.randomDistinct(sampleSize: Int): List<T> = toList().randomDistinct(sampleSize)

/**
 * Iterable에서 `sampleSize` 만큼의 unique 한 random 요소를 반환한다
 *
 * ```kotlin
 * val result = listOf(1, 2, 3, 4, 5).randomDistinct(3)
 * // 중복 없이 3개의 랜덤 요소
 * ```
 */
fun <T: Any> Iterable<T>.randomDistinct(sampleSize: Int): List<T> = toList().randomDistinct(sampleSize)

/**
 * `sampleSize` 만큼의 random 요소를 반환한다 (중복 허용)
 *
 * ```kotlin
 * val result = listOf(1, 2, 3).random(5)
 * // 중복 가능한 5개의 랜덤 요소 (예: [2, 1, 3, 2, 1])
 * ```
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
        .toList()
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
 *
 * ```kotlin
 * val coin = WeightedCoin(0.7)   // 70% 확률로 true
 * val result = coin.flip()       // 70% 확률로 true, 30% 확률로 false
 * ```
 */
class WeightedCoin(val trueProbability: Double) {

    init {
        assert(trueProbability in 0.0..1.0) {
            "trueProbability[$trueProbability] must be in 0.0 .. 1.0"
        }
    }

    /**
     * 가중치에 따라 true 또는 false를 반환합니다.
     *
     * ```kotlin
     * val coin = WeightedCoin(0.5)
     * val result = coin.flip()   // 50% 확률로 true
     * ```
     */
    fun flip(): Boolean = Random.nextDouble(0.0, 1.0) <= trueProbability
}

/**
 * Simulates a weighted TRUE/FALSE coin flip, with a percentage of probability towards TRUE
 * In other words, this is a Probability Density Function (PDF) for discrete TRUE/FALSE values
 *
 * ```kotlin
 * val result = weightedCoinFlip(0.7)   // 70% 확률로 true
 * ```
 */
fun weightedCoinFlip(trueProbability: Double): Boolean {
    assert(trueProbability in 0.0..1.0) { "trueProbability[$trueProbability] must be in 0.0 .. 1.0" }
    return ThreadLocalRandom.current().nextDouble(0.0, 1.0) <= trueProbability
}

/**
 *  Assigns a probabilty to each distinct `T` item, and randomly selects `T` values given those probabilities.
 *  In other words, this is a Probability Density Function (PDF) for discrete `T` values
 *
 * ```kotlin
 * val dice = WeightedDice("rock" to 0.33, "paper" to 0.33, "scissors" to 0.34)
 * val result = dice.roll()   // "rock", "paper" 또는 "scissors" 중 하나가 확률에 따라 선택됨
 * ```
 */
class WeightedDice<T: Any> private constructor(probabilities: Map<T, Double>) {

    companion object {
        /**
         * 값-확률 쌍 목록으로 [WeightedDice] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val dice = WeightedDice("A" to 0.5, "B" to 0.5)
         * ```
         */
        operator fun <E: Any> invoke(vararg values: Pair<E, Double>): WeightedDice<E> {
            assert(values.isNotEmpty()) { "values is empty." }
            return WeightedDice(values.toMap())
        }

        /**
         * 확률 맵으로 [WeightedDice] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val dice = WeightedDice(mapOf("A" to 0.3, "B" to 0.7))
         * ```
         */
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
            .toMap()
    }

    /**
     * 확률에 따라 랜덤으로 하나의 값을 선택합니다.
     *
     * ```kotlin
     * val dice = WeightedDice("A" to 0.7, "B" to 0.3)
     * val result = dice.roll()   // 70% 확률로 "A", 30% 확률로 "B"
     * ```
     */
    fun roll(): T = Random.nextDouble(0.0, sum).let { rnd ->
        rangedDistribution
            .asSequence()
            .first { rng ->
                rnd in rng.value
            }
            .key
    }
}
