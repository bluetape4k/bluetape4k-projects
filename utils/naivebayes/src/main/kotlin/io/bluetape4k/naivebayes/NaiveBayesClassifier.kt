package io.bluetape4k.naivebayes

import io.bluetape4k.logging.KLogging
import kotlinx.atomicfu.atomic
import kotlin.math.exp
import kotlin.math.ln
import kotlin.reflect.KProperty1


/**
 * feature 집합으로 카테고리를 예측하는 나이브 베이즈 분류기입니다.
 *
 * ## 동작/계약
 * - 관측치는 [addObservation]으로 누적되며 [observationLimit]을 넘으면 가장 오래된 항목이 제거됩니다.
 * - 예측 호출 시 모델이 stale 상태면 내부 확률 모델을 재구성합니다.
 * - 확률 계산은 로그 합산 후 지수화하는 방식으로 수행됩니다.
 *
 * ```kotlin
 * val nbc = NaiveBayesClassifier<String, String>()
 * nbc.addObservation("spam", listOf("free", "offer"))
 * nbc.addObservation("ham", listOf("hello", "friend"))
 * // nbc.predict("free") == "spam"
 * ```
 *
 * @property observationLimit 유지할 최대 관측치 수
 * @property k1 라플라스 계열 스무딩 상수 1
 * @property k2 라플라스 계열 스무딩 상수 2
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
     * 카테고리와 feature 집합 관측치를 추가합니다.
     *
     * ## 동작/계약
     * - [observationLimit] 도달 시 가장 오래된 관측치 1건을 제거한 후 추가합니다.
     * - 입력 feature는 [Set]으로 변환되어 중복이 제거됩니다.
     * - 추가 후 모델은 stale 상태가 되어 다음 예측 시 재구성됩니다.
     *
     * ```kotlin
     * nbc.addObservation("spam", listOf("free", "offer", "free"))
     * // nbc.population.last().features.size == 2
     * ```
     */
    fun addObservation(category: C, features: Iterable<F>) {
        if (_population.size == observationLimit) {
            _population.removeAt(0)
        }
        _population += BayesInput(category, features.toSet())
        modelStaled = true
    }

    /**
     * 가변 인자 feature로 관측치를 추가합니다.
     *
     * ## 동작/계약
     * - 내부적으로 [addObservation] Iterable 버전에 위임합니다.
     * - 중복 feature는 제거됩니다.
     *
     * ```kotlin
     * nbc.addObservation("ham", "hello", "world")
     * // nbc.population.isNotEmpty() == true
     * ```
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

    /** 현재 모델에 학습된 카테고리 집합입니다. */
    val categories: Set<C>
        get() = probabilities.keys.map { it.category }.toSet()

    /**
     * feature 가변 인자로 카테고리를 예측합니다.
     *
     * ## 동작/계약
     * - [predictWithProbability] 결과의 카테고리만 반환합니다.
     * - 예측 불가 시 null을 반환합니다.
     */
    fun predict(vararg features: F): C? = predictWithProbability(features.toSet())?.category

    /**
     * feature 컬렉션으로 카테고리를 예측합니다.
     *
     * ## 동작/계약
     * - [predictWithProbability] 결과의 카테고리만 반환합니다.
     * - 예측 불가 시 null을 반환합니다.
     */
    fun predict(features: Iterable<F>): C? = predictWithProbability(features)?.category

    /**
     * feature 컬렉션으로 예측 카테고리와 확률을 함께 반환합니다.
     *
     * ## 동작/계약
     * - 모델 stale 상태면 내부 확률 모델을 재구성합니다.
     * - 각 카테고리 확률을 계산한 뒤 임계치(`>= 0.1`) 이상 후보 중 최대 확률을 반환합니다.
     * - 유효 후보가 없으면 null을 반환합니다.
     *
     * ```kotlin
     * val cp = nbc.predictWithProbability(listOf("free", "offer"))
     * // cp == null || cp.probability >= 0.1
     * ```
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

    /**
     * 단일 feature-카테고리 조합의 사후 확률 정보를 보관합니다.
     *
     * ## 동작/계약
     * - 생성 시점의 분류기 관측치를 기준으로 [probability], [notProbability]를 계산합니다.
     * - 계산 후 값은 불변이며 분류기 상태 변경과 자동 동기화되지 않습니다.
     *
     * ```kotlin
     * val fp = NaiveBayesClassifier.FeatureProbability("free", "spam", nbc)
     * // fp.probability > 0.0
     * ```
     */
    class FeatureProbability<F: Any, C: Any>(val feature: F, val category: C, nbc: NaiveBayesClassifier<F, C>) {

        /** feature-카테고리 복합 키입니다. */
        data class Key<F, C>(val feature: F, val category: C)

        /** 카테고리일 때 feature가 나타날 확률입니다. */
        val probability: Double =
            (nbc.k1 + nbc.population.count { it.category == category && feature in it.features }) /
                    (nbc.k2 + nbc.population.count { it.category == category })

        /** 카테고리가 아닐 때 feature가 나타날 확률입니다. */
        val notProbability: Double =
            (nbc.k1 + nbc.population.count { it.category != category && feature in it.features }) /
                    (nbc.k2 + nbc.population.count { it.category != category })

        /** 현재 확률 항목의 키를 반환합니다. */
        val key: Key<F, C> get() = Key(feature, category)
    }
}
