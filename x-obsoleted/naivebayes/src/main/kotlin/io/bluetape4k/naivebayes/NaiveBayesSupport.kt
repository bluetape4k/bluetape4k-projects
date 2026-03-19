package io.bluetape4k.naivebayes

/**
 * 나이브 베이즈 학습용 단일 관측치입니다.
 *
 * ## 동작/계약
 * - [category]와 [features]를 그대로 보관하는 불변 값 객체입니다.
 * - 학습 시 중복 feature는 [Set] 특성에 따라 제거되어 저장됩니다.
 *
 * ```kotlin
 * val input = BayesInput(category = "spam", features = setOf("free", "offer"))
 * // input.features.size == 2
 * ```
 */
data class BayesInput<F: Any, C: Any>(val category: C, val features: Set<F>)

/**
 * 카테고리별 예측 확률 결과를 나타냅니다.
 *
 * ## 동작/계약
 * - [probability]는 `0.0..1.0` 범위 확률로 사용됩니다.
 * - 계산/정규화 규칙은 [NaiveBayesClassifier] 구현을 따릅니다.
 *
 * ```kotlin
 * val cp = CategoryProbability("spam", 0.91)
 * // cp.probability > 0.9
 * ```
 */
data class CategoryProbability<C: Any>(val category: C, val probability: Double)

/**
 * Iterable 데이터를 학습시켜 [NaiveBayesClassifier]를 생성합니다.
 *
 * ## 동작/계약
 * - 컬렉션을 순회하며 각 항목을 [featuresSelector], [categorySelector]로 관측치로 변환합니다.
 * - 변환된 관측치는 생성된 분류기에 순차적으로 추가됩니다.
 * - 입력 컬렉션을 mutate하지 않으며 새 분류기 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val nbc = listOf("spam free", "hello world").toNaiveBayesClassifier(
 *     featuresSelector = { it.split(" ") },
 *     categorySelector = { if ("spam" in it) "spam" else "ham" },
 * )
 * // nbc.predict("free") == "spam"
 * ```
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

/**
 * Sequence 데이터를 학습시켜 [NaiveBayesClassifier]를 생성합니다.
 *
 * ## 동작/계약
 * - 시퀀스를 순회해 관측치를 구성하고 분류기에 추가합니다.
 * - 시퀀스는 한 번 소비되며 입력 시퀀스를 mutate하지 않습니다.
 *
 * ```kotlin
 * val nbc = sequenceOf("spam free", "hello world").toNaiveBayesClassifier(
 *     featuresSelector = { it.split(" ") },
 *     categorySelector = { if ("spam" in it) "spam" else "ham" },
 * )
 * // nbc.predict("free") == "spam"
 * ```
 */
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

/**
 * Iterable 데이터로 [NaiveBayesClassifier]를 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 새 분류기를 생성하고 모든 항목을 학습시킵니다.
 * - [observationLimit], [k1], [k2]는 생성된 분류기 파라미터로 전달됩니다.
 *
 * ```kotlin
 * val nbc = naiveBayesClassifierOf(
 *     collection = listOf("a b", "spam c"),
 *     featuresSelector = { it.split(" ") },
 *     categorySelector = { if ("spam" in it) "spam" else "ham" },
 * )
 * // nbc.predict("spam") == "spam"
 * ```
 */
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

/**
 * Sequence 데이터로 [NaiveBayesClassifier]를 생성합니다.
 *
 * ## 동작/계약
 * - 시퀀스를 순회하며 학습 데이터를 즉시 추가합니다.
 * - 시퀀스는 한 번 소비되며 재사용되지 않습니다.
 *
 * ```kotlin
 * val nbc = naiveBayesClassifierOf(
 *     sequence = sequenceOf("ham one", "spam offer"),
 *     featuresSelector = { it.split(" ").asSequence() },
 *     categorySelector = { if ("spam" in it) "spam" else "ham" },
 * )
 * // nbc.predict("offer") == "spam"
 * ```
 */
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
