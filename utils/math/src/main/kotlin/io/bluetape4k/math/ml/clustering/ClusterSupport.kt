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


fun <T: Clusterable> kMeansClusterOf(
    k: Int,
    maxInterations: Int = -1,
    measure: DistanceMeasure = DistanceMeasureMethod.Euclidean.measurer,
    random: RandomGenerator = JDKRandomGenerator(),
    emptyStrategy: EmptyClusterStrategy = EmptyClusterStrategy.LARGEST_VARIANCE,
): KMeansPlusPlusClusterer<T> {
    return KMeansPlusPlusClusterer(k, maxInterations, measure, random, emptyStrategy)
}

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

fun <T: Clusterable> multiKMeansClusterOf(
    clusterer: KMeansPlusPlusClusterer<T>,
    numTrials: Int,
    evaluator: ClusterEvaluator<T> = SumOfClusterVariances(clusterer.distanceMeasure),
): MultiKMeansPlusPlusClusterer<T> {
    return MultiKMeansPlusPlusClusterer(clusterer, numTrials, evaluator)
}

fun <T: Clusterable> dbScanClusterOf(
    maximumRadius: Double,
    minPoints: Int,
    measure: DistanceMeasure = DistanceMeasureMethod.Euclidean.measurer,
): DBSCANClusterer<T> {
    return DBSCANClusterer(maximumRadius, minPoints, measure)
}
