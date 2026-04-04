package io.bluetape4k.math.ml.clustering

import io.bluetape4k.math.ml.distance.DistanceMeasureMethod
import org.apache.commons.math3.ml.clustering.Clusterable
import org.apache.commons.math3.ml.clustering.DBSCANClusterer
import org.apache.commons.math3.ml.clustering.FuzzyKMeansClusterer
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer.EmptyClusterStrategy
import org.apache.commons.math3.ml.clustering.MultiKMeansPlusPlusClusterer
import org.apache.commons.math3.ml.clustering.evaluation.ClusterEvaluator
import org.apache.commons.math3.ml.clustering.evaluation.SumOfClusterVariances
import org.apache.commons.math3.ml.distance.DistanceMeasure
import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator

/**
 * K-Means++ 클러스터러를 생성합니다.
 *
 * ```kotlin
 * val clusterer = kMeansClusterOf<DoublePoint>(k = 3)
 * val clusters = clusterer.cluster(points)
 * // clusters.size == 3
 * ```
 */
fun <T: Clusterable> kMeansClusterOf(
    k: Int,
    maxInterations: Int = -1,
    measure: DistanceMeasure = DistanceMeasureMethod.Euclidean.measurer,
    random: RandomGenerator = JDKRandomGenerator(),
    emptyStrategy: EmptyClusterStrategy = EmptyClusterStrategy.LARGEST_VARIANCE,
): KMeansPlusPlusClusterer<T> {
    return KMeansPlusPlusClusterer(k, maxInterations, measure, random, emptyStrategy)
}

/**
 * 퍼지 K-Means 클러스터러를 생성합니다.
 *
 * ```kotlin
 * val clusterer = fuzzyKMeansClusterOf<DoublePoint>(k = 3, fuzziness = 2.0)
 * val clusters = clusterer.cluster(points)
 * // clusters.size == 3
 * ```
 */
fun <T: Clusterable> fuzzyKMeansClusterOf(
    k: Int,
    fuzziness: Double = 2.0,
    maxInterations: Int = -1,
    measure: DistanceMeasure = DistanceMeasureMethod.Euclidean.measurer,
    epsilon: Double = 1e-3,
    random: RandomGenerator = JDKRandomGenerator(),
): FuzzyKMeansClusterer<T> {
    return FuzzyKMeansClusterer(k, fuzziness, maxInterations, measure, epsilon, random)
}

/**
 * Multi K-Means++ 클러스터러를 생성합니다. 여러 번 시도하여 최적의 클러스터링을 반환합니다.
 *
 * ```kotlin
 * val base = kMeansClusterOf<DoublePoint>(k = 3)
 * val clusterer = multiKMeansClusterOf(clusterer = base, numTrials = 10)
 * val clusters = clusterer.cluster(points)
 * ```
 */
fun <T: Clusterable> multiKMeansClusterOf(
    clusterer: KMeansPlusPlusClusterer<T>,
    numTrials: Int,
    evaluator: ClusterEvaluator<T> = SumOfClusterVariances(clusterer.distanceMeasure),
): MultiKMeansPlusPlusClusterer<T> {
    return MultiKMeansPlusPlusClusterer(clusterer, numTrials, evaluator)
}

/**
 * DBSCAN 클러스터러를 생성합니다. 밀도 기반 클러스터링 알고리즘입니다.
 *
 * ```kotlin
 * val clusterer = dbScanClusterOf<DoublePoint>(maximumRadius = 0.5, minPoints = 3)
 * val clusters = clusterer.cluster(points)
 * ```
 */
fun <T: Clusterable> dbScanClusterOf(
    maximumRadius: Double,
    minPoints: Int,
    measure: DistanceMeasure = DistanceMeasureMethod.Euclidean.measurer,
): DBSCANClusterer<T> {
    return DBSCANClusterer(maximumRadius, minPoints, measure)
}
