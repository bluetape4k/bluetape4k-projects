package io.bluetape4k.naivebayes

data class BayesInput<F: Any, C: Any>(val category: C, val features: Set<F>)

data class CategoryProbability<C: Any>(val category: C, val probability: Double)

/**
 * Returns a [NaiveBayesClassifier] that associates each set of `F` features from an item `T` with a category `C`.
 * New sets of features `F` can then be used to predict a category `C`.
 */
fun <T: Any, F: Any, C: Any> Iterable<T>.toNaiveBayesClassifier(
    featuresSelector: (T) -> Iterable<F>,
    categorySelector: (T) -> C,
    observationLimit: Int = Int.MAX_VALUE,
    k1: Double = NaiveBayesClassifier.DEFAULT_K1,
    k2: Double = NaiveBayesClassifier.DEFAULT_K2,
): NaiveBayesClassifier<F, C> {
    return NaiveBayesClassifier<F, C>(observationLimit, k1, k2).also { nbc ->
        this@toNaiveBayesClassifier.forEach { elem ->
            nbc.addObservation(categorySelector(elem), featuresSelector(elem))
        }
    }
}

fun <T: Any, F: Any, C: Any> Sequence<T>.toNaiveBayesClassifier(
    featuresSelector: (T) -> Iterable<F>,
    categorySelector: (T) -> C,
    observationLimit: Int = Int.MAX_VALUE,
    k1: Double = NaiveBayesClassifier.DEFAULT_K1,
    k2: Double = NaiveBayesClassifier.DEFAULT_K2,
): NaiveBayesClassifier<F, C> {
    return NaiveBayesClassifier<F, C>(observationLimit, k1, k2).also { nbc ->
        this@toNaiveBayesClassifier.forEach { elem ->
            nbc.addObservation(categorySelector(elem), featuresSelector(elem))
        }
    }
}

fun <T: Any, F: Any, C: Any> naiveBayesClassifierOf(
    collection: Iterable<T>,
    featuresSelector: (T) -> Iterable<F>,
    categorySelector: (T) -> C,
    observationLimit: Int = Int.MAX_VALUE,
    k1: Double = NaiveBayesClassifier.DEFAULT_K1,
    k2: Double = NaiveBayesClassifier.DEFAULT_K2,
): NaiveBayesClassifier<F, C> {
    return NaiveBayesClassifier<F, C>(observationLimit, k1, k2).also { nbc ->
        collection.forEach { elem ->
            nbc.addObservation(categorySelector(elem), featuresSelector(elem))
        }
    }
}

fun <T: Any, F: Any, C: Any> naiveBayesClassifierOf(
    sequence: Sequence<T>,
    featuresSelector: (T) -> Sequence<F>,
    categorySelector: (T) -> C,
    observationLimit: Int = Int.MAX_VALUE,
    k1: Double = NaiveBayesClassifier.DEFAULT_K1,
    k2: Double = NaiveBayesClassifier.DEFAULT_K2,
): NaiveBayesClassifier<F, C> {
    return NaiveBayesClassifier<F, C>(observationLimit, k1, k2).also { nbc ->
        sequence.forEach { elem ->
            nbc.addObservation(categorySelector(elem), featuresSelector(elem).asIterable())
        }
    }
}
