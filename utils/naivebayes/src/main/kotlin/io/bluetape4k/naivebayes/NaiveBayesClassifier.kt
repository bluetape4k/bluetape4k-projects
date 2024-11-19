package io.bluetape4k.naivebayes

import io.bluetape4k.logging.KLogging
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlin.math.exp
import kotlin.math.ln


/**
 * A `NaiveBayesClassifier` that associates each set of `F` features from an item `T` with a category `C`.
 * New sets of features `F` can then be used to predict a category `C`.
 *
 * @param F
 * @param C
 * @property observationLimit
 * @property k1
 * @property k2
 */
class NaiveBayesClassifier<F: Any, C: Any>(
    private val observationLimit: Int = Int.MAX_VALUE,
    val k1: Double = DEFAULT_K1,
    val k2: Double = DEFAULT_K2,
) {

    companion object: KLogging() {
        const val DEFAULT_K1: Double = 0.5
        const val DEFAULT_K2: Double = DEFAULT_K1 * 2.0
    }

    @Volatile
    private var probabilities: Map<FeatureProbability.Key<F, C>, FeatureProbability<F, C>> = mutableMapOf()

    private val _population: MutableList<BayesInput<F, C>> = mutableListOf()
    val population: List<BayesInput<F, C>> get() = _population.toList()

    private val modelStaler = atomic(false)
    private var modelStaled: Boolean by modelStaler

    /**
     * Adds an observation of features to a category
     */
    fun addObservation(category: C, features: Iterable<F>) {
        if (_population.size == observationLimit) {
            _population.removeAt(0)
        }
        _population += BayesInput(category, features.toSet())
        modelStaler.value = true
    }

    /**
     * Adds an observation of features to a category
     */
    fun addObservation(category: C, vararg features: F) {
        addObservation(category, features.toSet())
    }

    private fun rebuildModel() {
        probabilities = _population
            .flatMap { it.features }
            .distinct()
            .flatMap { f ->
                _population
                    .map { it.category }
                    .distinct()
                    .map { c -> FeatureProbability.Key(f, c) }
            }
            .map { it to FeatureProbability(it.feature, it.category, this) }
            .toMap()

        modelStaler.value = false
    }

    /**
     * Returns the categories that have been captured by the model so far.
     */
    val categories: Set<C>
        get() = probabilities.keys.map { it.category }.toSet()

    /**
     *  Predicts a category `C` for a given set of `F` features
     */
    suspend fun predict(vararg features: F): C? = predictWithProbability(features.toSet())?.category

    /**
     * Predicts a category `C` for a given set of `F` features
     */
    suspend fun predict(features: Iterable<F>): C? = predictWithProbability(features)?.category

    /**
     *  Predicts a category `C` for a given set of `F` features,
     *  but also returns the probability of that category being correct.
     */
    suspend fun predictWithProbability(features: Iterable<F>): CategoryProbability<C>? = coroutineScope {
        if (modelStaled) {
            rebuildModel()
        }

        val f = features.toSet()

        categories.asFlow()
            .filter { category: C ->
                population.any { it.category == category } && probabilities.values.any { it.feature in f }
            }
            .flatMapMerge { category: C ->
                val probIfCategory = async {
                    probabilities.values
                        .filter { it.category == category }
                        .sumOf {
                            if (it.feature in f) {
                                ln(it.probability)
                            } else {
                                ln(1.0 - it.probability)
                            }
                        }
                        .run { exp(this) }
                }

                val probIfNotCategory = async {
                    probabilities.values
                        .filter { it.category == category }
                        .sumOf {
                            if (it.feature in f) {
                                ln(it.notProbability)
                            } else {
                                ln(1.0 - it.notProbability)
                            }
                        }
                        .run { exp(this) }
                }

                flowOf(
                    CategoryProbability(
                        category = category,
                        probability = probIfCategory.await() / (probIfCategory.await() + probIfNotCategory.await())
                    )
                )
            }
            .buffer()
            .filter { it.probability >= 0.1 }
            .toList()
            .maxByOrNull { it.probability }
    }

    class FeatureProbability<F: Any, C: Any>(val feature: F, val category: C, nbc: NaiveBayesClassifier<F, C>) {

        data class Key<F, C>(val feature: F, val category: C)

        val probability: Double =
            (nbc.k1 + nbc.population.count { it.category == category && feature in it.features }) /
                    (nbc.k2 + nbc.population.count { it.category == category })

        val notProbability: Double =
            (nbc.k1 + nbc.population.count { it.category != category && feature in it.features }) /
                    (nbc.k2 + nbc.population.count { it.category != category })

        val key: Key<F, C> get() = Key(feature, category)
    }
}
