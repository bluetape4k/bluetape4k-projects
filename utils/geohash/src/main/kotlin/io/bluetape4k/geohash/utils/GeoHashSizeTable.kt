package io.bluetape4k.geohash.utils

import io.bluetape4k.geohash.BoundingBox
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import kotlin.math.pow

object GeoHashSizeTable: KLogging() {

    private const val NUM_BITS = 64

    val dLat = DoubleArray(NUM_BITS) { 180.0 / 2.0.pow(it / 2) }
    val dLon = DoubleArray(NUM_BITS) { 360.0 / 2.0.pow((it + 1) / 2) }

    fun numberOfBitsForOverlappingGeoHash(bbox: BoundingBox): Int {
        var bits = 63
        val height = bbox.getLatitudeSize()
        val width = bbox.getLongitudeSize()

        log.trace { "height=$height, width=$width, bbox=$bbox" }

        while ((dLat[bits] < height || dLon[bits] < width) && bits > 0) {
            bits--
        }
        return bits
    }
}
