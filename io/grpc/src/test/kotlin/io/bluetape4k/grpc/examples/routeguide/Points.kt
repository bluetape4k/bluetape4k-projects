package io.bluetape4k.grpc.examples.routeguide

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_IN_M = 6_371_000

private fun Int.toRadians() = Math.toRadians(this.toDouble())

private fun Point.toRadians(): Pair<Double, Double> = Pair(latitude.toRadians(), longitude.toRadians())

internal infix fun Point.distanceTo(other: Point): Int {
    val p1 = toRadians()
    val p2 = other.toRadians()

    val dLat = p2.first - p1.first
    val dLon = p2.second - p1.second

    val a = sin(dLat / 2.0).pow(2) + cos(p1.first) * cos(p2.first) * sin(dLon / 2.0).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1.0 - a))

    return (EARTH_RADIUS_IN_M * c).roundToInt()
}

internal operator fun Rectangle.contains(p: Point): Boolean {
    val lowLat = minOf(lo.latitude, hi.latitude)
    val lowLon = minOf(lo.longitude, hi.longitude)
    val hiLat = maxOf(lo.latitude, hi.latitude)
    val hiLon = maxOf(lo.longitude, hi.longitude)

    return p.longitude in lowLon..hiLon && p.latitude in lowLat..hiLat
}

private fun Int.normalizeCoordinate(): Double = this.toDouble() / 1.0e7

internal fun Point.toStr(): String {
    val lat = latitude.normalizeCoordinate()
    val lon = longitude.normalizeCoordinate()

    return "$lat, $lon"
}

internal fun Feature.exists(): Boolean = name.isNotBlank()

internal fun pointOf(lat: Int, lon: Int): Point =
    Point.newBuilder()
        .apply {
            latitude = lat
            longitude = lon
        }
        .build()
