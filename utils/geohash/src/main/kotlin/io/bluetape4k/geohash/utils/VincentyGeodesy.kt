package io.bluetape4k.geohash.utils

import io.bluetape4k.geohash.WGS84Point
import io.bluetape4k.logging.KLogging
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Vincenty의 지구측지법 알고리즘을 캡슐화합니다.
 *
 * 참고: [Vincenty's geodesy algorithm](https://en.wikipedia.org/wiki/Vincenty's_formulae)
 */
object VincentyGeodesy: KLogging() {

    const val equatorRadius = 6378137.0
    const val poleRadius = 6356752.3142
    const val f = 1 / 298.257223563
    const val degToRad = 0.0174532925199433

    const val equatorRadiusSquared = equatorRadius * equatorRadius
    const val poleRadiusSquared = poleRadius * poleRadius
    const val EPSILON: Double = 1e-12

    /**
     * Vincenty's formula를 사용 [point]를 [bearingInDegrees] 방향으로 [distanceInMeters] 만큼 이동한 [WGS84Point]를 반환합니다.
     *
     * @param point 시작 좌표
     * @param bearingInDegrees 각도 (0..360)
     * @param distanceInMeters: 이동할 거리 (미터)
     */
    fun moveInDirection(
        point: WGS84Point,
        bearingInDegrees: Double,
        distanceInMeters: Double,
    ): WGS84Point {
        require(bearingInDegrees in 0.0..360.0) { "direction must be in (0,360)" }

        val a = 6378137.0
        val b = 6356752.3142
        val f = 1 / 298.257223563 // WGS-84

        // ellipsiod
        val alpha1 = bearingInDegrees * degToRad
        val sinAlpha1 = sin(alpha1)
        val cosAlpha1 = cos(alpha1)

        val tanU1 = (1 - f) * tan(point.latitude * degToRad)
        val cosU1 = 1 / sqrt(1 + tanU1 * tanU1)
        val sinU1 = tanU1 * cosU1
        val sigma1 = atan2(tanU1, cosAlpha1)
        val sinAlpha = cosU1 * sinAlpha1
        val cosSqAlpha = 1 - sinAlpha * sinAlpha
        val uSq = cosSqAlpha * (a * a - b * b) / (b * b)
        val A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)))
        val B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)))

        var sinSigma = 0.0
        var cosSigma = 0.0
        var cos2SigmaM = 0.0
        var sigma = distanceInMeters / (b * A)
        var sigmaP = 2 * Math.PI

        while (abs(sigma - sigmaP) > EPSILON) {
            cos2SigmaM = cos(2 * sigma1 + sigma)
            sinSigma = sin(sigma)
            cosSigma = cos(sigma)
            val deltaSigma = (B
                    * sinSigma
                    * (cos2SigmaM + B
                    / 4
                    * (cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM) - (B / 6 * cos2SigmaM
                    * (-3 + 4 * sinSigma * sinSigma) * (-3 + 4 * cos2SigmaM * cos2SigmaM)))))
            sigmaP = sigma
            sigma = distanceInMeters / (b * A) + deltaSigma
        }
        val tmp = sinU1 * sinSigma - cosU1 * cosSigma * cosAlpha1
        val lat2 = atan2(
            sinU1 * cosSigma + cosU1 * sinSigma * cosAlpha1, (1 - f)
                    * sqrt(sinAlpha * sinAlpha + tmp * tmp)
        )
        val lambda = atan2(sinSigma * sinAlpha1, cosU1 * cosSigma - sinU1 * sinSigma * cosAlpha1)
        val C = f / 16 * cosSqAlpha * (4 + f * (4 - 3 * cosSqAlpha))
        val L = lambda - ((1 - C) * f * sinAlpha
                * (sigma + C * sinSigma * (cos2SigmaM + C * cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM))))
        val newLat = lat2 / degToRad
        var newLon: Double = point.longitude + L / degToRad
        newLon = if (newLon > 180.0) newLon - 360 else newLon
        newLon = if (newLon < -180.0) 360.0 + newLon else newLon
        return WGS84Point(newLat, newLon)
    }

    /**
     * Vincenty's formula를 사용하여 두 [WGS84Point] 사이의 거리를 계산합니다.
     *
     * @param start 첫번째 WGS84 좌표
     * @param end 두번째 WGS84 좌표
     * @return 두 좌표 사이의 거리 (미터)
     */
    fun distanceInMeters(start: WGS84Point, end: WGS84Point): Double {
        val a = 6378137.0
        val b = 6356752.3142
        val f = 1 / 298.257223563 // WGS-84
        // ellipsiod
        val L: Double = (end.longitude - start.longitude) * degToRad
        val U1 = atan((1 - f) * tan(start.latitude * degToRad))
        val U2 = atan((1 - f) * tan(end.latitude * degToRad))
        val sinU1 = sin(U1)
        val cosU1 = cos(U1)
        val sinU2 = sin(U2)
        val cosU2 = cos(U2)
        var cosSqAlpha: Double
        var sinSigma: Double
        var cos2SigmaM: Double
        var cosSigma: Double
        var sigma: Double
        var lambda = L
        var lambdaP: Double
        var iterLimit = 20.0

        do {
            val sinLambda = sin(lambda)
            val cosLambda = cos(lambda)
            sinSigma = sqrt(
                cosU2 * sinLambda * (cosU2 * sinLambda)
                        + (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda) * (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda)
            )
            if (sinSigma == 0.0) {
                return 0.0 // co-incident points
            }
            cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda
            sigma = atan2(sinSigma, cosSigma)
            val sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma
            cosSqAlpha = 1 - sinAlpha * sinAlpha
            cos2SigmaM = cosSigma - 2 * sinU1 * sinU2 / cosSqAlpha
            if (java.lang.Double.isNaN(cos2SigmaM)) {
                cos2SigmaM = 0.0 // equatorial line: cosSqAlpha=0
            }
            val C = f / 16 * cosSqAlpha * (4 + f * (4 - 3 * cosSqAlpha))
            lambdaP = lambda
            lambda = L + ((1 - C) * f * sinAlpha
                    * (sigma + C * sinSigma * (cos2SigmaM + C * cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM))))
        } while (abs(lambda - lambdaP) > EPSILON && --iterLimit > 0)

        if (iterLimit == 0.0) {
            return Double.NaN
        }

        val uSquared = cosSqAlpha * (a * a - b * b) / (b * b)
        val A = 1 + uSquared / 16384 * (4096 + uSquared * (-768 + uSquared * (320 - 175 * uSquared)))
        val B = uSquared / 1024 * (256 + uSquared * (-128 + uSquared * (74 - 47 * uSquared)))
        val deltaSigma = (B
                * sinSigma
                * (cos2SigmaM + B
                / 4
                * (cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM) - (B / 6 * cos2SigmaM
                * (-3 + 4 * sinSigma * sinSigma) * (-3 + 4 * cos2SigmaM * cos2SigmaM)))))

        return b * A * (sigma - deltaSigma)
    }
}
