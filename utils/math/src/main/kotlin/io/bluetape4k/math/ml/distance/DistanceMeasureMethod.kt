package io.bluetape4k.math.ml.distance

import org.apache.commons.math3.ml.clustering.DoublePoint
import org.apache.commons.math3.ml.distance.CanberraDistance
import org.apache.commons.math3.ml.distance.ChebyshevDistance
import org.apache.commons.math3.ml.distance.DistanceMeasure
import org.apache.commons.math3.ml.distance.EarthMoversDistance
import org.apache.commons.math3.ml.distance.EuclideanDistance
import org.apache.commons.math3.ml.distance.ManhattanDistance

/**
 * 거리 측정 방법
 *
 * ```kotlin
 * val a = doubleArrayOf(0.0, 0.0)
 * val b = doubleArrayOf(3.0, 4.0)
 * val dist = DistanceMeasureMethod.Euclidean.compute(a, b)   // 5.0
 * ```
 *
 * @property measurer 거리 측정자
 */
enum class DistanceMeasureMethod(val measurer: DistanceMeasure) {
    Canberra(CanberraDistance()),
    Chebyshev(ChebyshevDistance()),
    EarthMovers(EarthMoversDistance()),
    Euclidean(EuclideanDistance()),
    Manhattan(ManhattanDistance());


    /**
     * 두 좌표 배열의 거리를 계산합니다.
     *
     * ```kotlin
     * val dist = DistanceMeasureMethod.Euclidean.compute(doubleArrayOf(0.0, 0.0), doubleArrayOf(3.0, 4.0))
     * // dist == 5.0
     * ```
     *
     * @param a 시작점
     * @param b 끝 점
     * @return 두 점의 거리
     */
    fun compute(a: DoubleArray, b: DoubleArray): Double {
        return measurer.compute(a, b)
    }

    /**
     * 두 DoublePoint의 거리를 계산합니다.
     *
     * ```kotlin
     * val a = doublePointOf(0.0, 0.0)
     * val b = doublePointOf(3.0, 4.0)
     * val dist = DistanceMeasureMethod.Euclidean.compute(a, b)   // 5.0
     * ```
     *
     * @param a 시작점
     * @param b 끝 점
     * @return 두 점의 거리
     */
    fun compute(a: DoublePoint, b: DoublePoint): Double =
        measurer.compute(a.point, b.point)

    companion object {
        fun parse(measureMethod: String): DistanceMeasureMethod? =
            entries.find { it.name.equals(measureMethod, true) }
    }
}
