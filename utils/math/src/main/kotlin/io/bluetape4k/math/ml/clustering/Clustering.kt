package io.bluetape4k.math.ml.clustering

import org.apache.commons.math3.ml.clustering.Clusterable
import org.apache.commons.math3.ml.clustering.DoublePoint
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer
import java.io.Serializable

/**
 * Clustering 을 수행할 입력 데이터
 *
 * @property item Item
 * @property location n-dimentional point
 */
data class ClusterInput<out T: Any>(
    val item: T,
    val location: DoubleArray,
): Clusterable, Serializable {

    override fun getPoint(): DoubleArray = location

    override fun equals(other: Any?): Boolean = when (other) {
        is ClusterInput<*> -> hashCode() == other.hashCode()
        else               -> false
    }

    override fun hashCode(): Int = item.hashCode()
}

/**
 * Clustering 된 군집의 중심점과 군집 요소를 나타냅니다
 *
 * @property center Cluster 의 중심 위치
 * @property points 해당 군집에 해당하는 요소 컬렉션
 */
data class Centroid<T: Any>(
    val center: DoublePoint,
    val points: List<T>,
): Serializable {

    operator fun contains(elem: T): Boolean = points.contains(elem)
}

/**
 * 지정한 정보로부터 2차원 정보를 추출하여 kMeans Clustering 을 수행합니다.
 */
fun Iterable<Pair<Double, Double>>.kMeansCluster(
    k: Int,
    maxIterations: Int = 100,
): List<Centroid<Pair<Double, Double>>> {
    return kMeansCluster(k, maxIterations, { it.first }, { it.second })
}

/**
 * 지정한 정보로부터 2차원 정보를 추출하여 kMeans Clustering 을 수행합니다.
 */
fun Sequence<Pair<Double, Double>>.kMeansCluster(
    k: Int,
    maxIterations: Int = 100,
): List<Centroid<Pair<Double, Double>>> =
    kMeansCluster(k, maxIterations, { it.first }, { it.second })

/**
 * 지정한 정보로부터 2차원 정보를 추출하여 kMeans Clustering 을 수행합니다.
 */
inline fun <T: Any> Iterable<T>.kMeansCluster(
    k: Int,
    maxIterations: Int = -1,
    xSelector: (T) -> Double,
    ySelector: (T) -> Double,
): List<Centroid<T>> {
    val inputs = map { ClusterInput(it, doubleArrayOf(xSelector(it), ySelector(it))) }

    return kMeansClusterOf<ClusterInput<T>>(k, maxIterations)
        .cluster(inputs)
        .map { cluster ->
            Centroid(
                center = cluster.center.point.let { doublePointOf(it[0], it[1]) },
                points = cluster.points.map { it.item }
            )
        }
}

/**
 * 지정한 정보로부터 2차원 정보를 추출하여 kMeans Clustering 을 수행합니다.
 */
inline fun <T: Any> Sequence<T>.kMeansCluster(
    k: Int,
    maxIterations: Int = 100,
    xSelector: (T) -> Double,
    ySelector: (T) -> Double,
): List<Centroid<T>> = asIterable().kMeansCluster(k, maxIterations, xSelector, ySelector)


fun Iterable<Pair<Double, Double>>.fuzzyKMeansCluster(k: Int, fuzziness: Double): List<Centroid<Pair<Double, Double>>> =
    fuzzyKMeansCluster(k, fuzziness, { it.first }, { it.second })

fun Sequence<Pair<Double, Double>>.fuzzyKMeansCluster(k: Int, fuzziness: Double): List<Centroid<Pair<Double, Double>>> =
    fuzzyKMeansCluster(k, fuzziness, { it.first }, { it.second })

/**
 * 2차원 정보를 추출하여 Fuzzy kMeans Clustering 을 수행합니다.
 *
 * @param k the number of clusters to split the data into
 * @param fuzziness the fuzziness factor, must be &gt; 1.0
 */
inline fun <T: Any> Iterable<T>.fuzzyKMeansCluster(
    k: Int,
    fuzziness: Double,
    xSelector: (T) -> Double,
    ySelector: (T) -> Double,
): List<Centroid<T>> {
    val inputs = map { ClusterInput(it, doubleArrayOf(xSelector(it), ySelector(it))) }

    return fuzzyKMeansClusterOf<ClusterInput<T>>(k, fuzziness)
        .cluster(inputs)
        .map { cluster ->
            Centroid(
                center = cluster.center.point.let { doublePointOf(it[0], it[1]) },
                points = cluster.points.map { it.item }
            )
        }
}

inline fun <T: Any> Sequence<T>.fuzzyKMeansCluster(
    k: Int,
    fuzziness: Double,
    xSelector: (T) -> Double,
    ySelector: (T) -> Double,
): List<Centroid<T>> = asIterable().fuzzyKMeansCluster(k, fuzziness, xSelector, ySelector)


fun Iterable<Pair<Double, Double>>.multiKMeansCluster(
    k: Int,
    maxIteration: Int,
    numTrials: Int,
): List<Centroid<Pair<Double, Double>>> =
    multiKMeansCluster(k, maxIteration, numTrials, { it.first }, { it.second })

fun Sequence<Pair<Double, Double>>.multiKMeansCluster(
    k: Int,
    maxIteration: Int,
    numTrials: Int,
): List<Centroid<Pair<Double, Double>>> =
    multiKMeansCluster(k, maxIteration, numTrials, { it.first }, { it.second })


/**
 * 2차원 정보를 이용하여 Multi kMeans Clustering 을 수행합니다.
 *
 * @param k the number of clusters to split the data into
 * @param maxIterations
 */
inline fun <T: Any> Iterable<T>.multiKMeansCluster(
    k: Int,
    maxIterations: Int,
    numTrials: Int,
    xSelector: (T) -> Double,
    ySelector: (T) -> Double,
): List<Centroid<T>> {
    val inputs = map { ClusterInput(it, doubleArrayOf(xSelector(it), ySelector(it))) }

    return KMeansPlusPlusClusterer<ClusterInput<T>>(k, maxIterations)
        .let { kMeansCluster ->
            multiKMeansClusterOf(kMeansCluster, numTrials)
                .cluster(inputs)
                .map { cluster ->
                    Centroid(
                        center = cluster.center.point.let { doublePointOf(it[0], it[1]) },
                        points = cluster.points.map { it.item }
                    )
                }
        }
}

/**
 * 2차원 정보를 이용하여 Multi kMeans Clustering 을 수행합니다.
 *
 * @param k the number of clusters to split the data into
 * @param maxIterations
 */
inline fun <T: Any> Sequence<T>.multiKMeansCluster(
    k: Int,
    maxIterations: Int,
    numTrials: Int,
    xSelector: (T) -> Double,
    ySelector: (T) -> Double,
): List<Centroid<T>> =
    asIterable().multiKMeansCluster(k, maxIterations, numTrials, xSelector, ySelector)


fun Iterable<Pair<Double, Double>>.dbScanCluster(
    maximumRadius: Double,
    minPoints: Int,
): List<Centroid<Pair<Double, Double>>> =
    dbScanCluster(maximumRadius, minPoints, { it.first }, { it.second })

fun Sequence<Pair<Double, Double>>.dbScanCluster(
    maximumRadius: Double,
    minPoints: Int,
): List<Centroid<Pair<Double, Double>>> =
    dbScanCluster(maximumRadius, minPoints, { it.first }, { it.second })

/**
 * 지정한 정보로부터 2차원 정보를 추출하여 kMeans Clustering 을 수행합니다.
 */
inline fun <T: Any> Iterable<T>.dbScanCluster(
    maximumRadius: Double,
    minPoints: Int,
    xSelector: (T) -> Double,
    ySelector: (T) -> Double,
): List<Centroid<T>> {
    val inputs = map { ClusterInput(it, doubleArrayOf(xSelector(it), ySelector(it))) }

    return dbScanClusterOf<ClusterInput<T>>(maximumRadius, minPoints)
        .cluster(inputs)
        .map { cluster ->
            Centroid(
                center = doublePointOf(-1.0, -1.0),
                points = cluster.points.map { it.item }
            )
        }
}

/**
 * 지정한 정보로부터 2차원 정보를 추출하여 kMeans Clustering 을 수행합니다.
 */
inline fun <T: Any> Sequence<T>.dbScanCluster(
    maximumRadius: Double,
    minPoints: Int,
    xSelector: (T) -> Double,
    ySelector: (T) -> Double,
): List<Centroid<T>> = asIterable().dbScanCluster(maximumRadius, minPoints, xSelector, ySelector)
