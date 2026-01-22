package io.bluetape4k.geohash.utils

import io.bluetape4k.geohash.BoundingBox
import io.bluetape4k.geohash.GeoHash
import io.bluetape4k.geohash.geoHashOfLongValue
import io.bluetape4k.geohash.geoHashWithBits
import io.bluetape4k.geohash.geoHashWithCharacters
import io.bluetape4k.logging.KLogging
import java.io.Serializable

fun twoGeoHashBoundingBoxOf(southWest: GeoHash, northEast: GeoHash): TwoGeoHashBoundingBox {
    require(southWest.significantBits() == northEast.significantBits()) {
        "Does it make sense to iterate between hashes that have different precisions?"
    }
    val southWestCorner = geoHashOfLongValue(southWest.longValue, southWest.significantBits())
    val northEastCorner = geoHashOfLongValue(northEast.longValue, northEast.significantBits())
    val boundingBox = BoundingBox(
        southWestCorner.boundingBox.southLatitude,
        northEastCorner.boundingBox.northLatitude,
        southWestCorner.boundingBox.westLongitude,
        northEastCorner.boundingBox.eastLongitude
    )
    return TwoGeoHashBoundingBox(southWestCorner, northEastCorner, boundingBox)
}

fun twoGeoHashWithCharacters(bbox: BoundingBox, numberOfCharacter: Int): TwoGeoHashBoundingBox {
    val southWest = geoHashWithCharacters(bbox.southLatitude, bbox.westLongitude, numberOfCharacter)
    val northEast = geoHashWithCharacters(bbox.northLatitude, bbox.eastLongitude, numberOfCharacter)

    return twoGeoHashBoundingBoxOf(southWest, northEast)
}

fun twoGeoHashWithBits(bbox: BoundingBox, numberOfBits: Int): TwoGeoHashBoundingBox {
    val southWest = geoHashWithBits(bbox.southLatitude, bbox.westLongitude, numberOfBits)
    val northEast = geoHashWithBits(bbox.northLatitude, bbox.eastLongitude, numberOfBits)

    return twoGeoHashBoundingBoxOf(southWest, northEast)
}

data class TwoGeoHashBoundingBox(
    val southWestCorner: GeoHash,
    val northEastCorner: GeoHash,
    val boundingBox: BoundingBox,
): Serializable {

    companion object: KLogging()

    fun toBase32(): String {
        return southWestCorner.toBase32() + northEastCorner.toBase32()
    }
}
