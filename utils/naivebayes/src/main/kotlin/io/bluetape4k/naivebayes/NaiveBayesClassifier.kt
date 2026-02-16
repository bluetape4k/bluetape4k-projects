package io.bluetape4k.naivebayes

import io.bluetape4k.logging.KLogging
import kotlinx.atomicfu.atomic
import kotlin.math.exp
import kotlin.math.ln
import kotlin.reflect.KProperty1


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
    val population: List<BayesInput<F, C>> get() = _population

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
        modelStaled = true
    }

    /**
     * Adds an observation of features to a category
     */
    fun addObservation(category: C, vararg features: F) {
        addObservation(category, features.toSet())
    }

    private fun rebuildModel() {
        probabilities = _population.flatMap { it.features }.distinct()
            .flatMap { f ->
                _population.map { it.category }.distinct()
                    .map { c -> FeatureProbability.Key(f, c) }
            }
            .associateWith { FeatureProbability(it.feature, it.category, this) }

        modelStaled = false
    }

    /**
     * Returns the categories that have been captured by the model so far.
     */
    val categories: Set<C>
        get() = probabilities.keys.map { it.category }.toSet()

    /**
     *  Predicts a category `C` for a given set of `F` features
     */
    fun predict(vararg features: F): C? = predictWithProbability(features.toSet())?.category

    /**
     * Predicts a category `C` for a given set of `F` features
     */
    fun predict(features: Iterable<F>): C? = predictWithProbability(features)?.category

    /**
     *  Predicts a category `C` for a given set of `F` features,
     *  but also returns the probability of that category being correct.
     */
    fun predictWithProbability(features: Iterable<F>): CategoryProbability<C>? {
        if (modelStaled) {
            rebuildModel()
        }

        val f = features.toSet()

        return categories
            .asSequence()
            .filter { category: C ->
                population.any { it.category == category } && probabilities.values.any { it.feature in f }
            }
            .map { category: C ->
                val probIfCategory = calcProbability(
                    probabilities.values,
                    category,
                    features,
                    FeatureProbability<F, C>::probability
                )

                val probIfNotCategory = calcProbability(
                    probabilities.values,
                    category,
                    features,
                    FeatureProbability<F, C>::notProbability
                )

                CategoryProbability(
                    category = category,
                    probability = probIfCategory / (probIfCategory + probIfNotCategory)
                )
            }
            .filter { it.probability >= 0.1 }
            .maxByOrNull { it.probability }
    }

    private fun calcProbability(
        porobabilities: Collection<FeatureProbability<F, C>>,
        category: C,
        features: Iterable<F>,
        props: KProperty1<FeatureProbability<F, C>, Double>,
    ): Double {
        return porobabilities
            .filter { it.category == category }
            .sumOf {
                if (it.feature in features) {
                    ln(props.get(it))
                } else {
                    ln(1.0 - props.get(it))
                }
            }.let { exp(it) }
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
