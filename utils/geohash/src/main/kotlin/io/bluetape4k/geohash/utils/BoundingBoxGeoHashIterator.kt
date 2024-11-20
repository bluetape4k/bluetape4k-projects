package io.bluetape4k.geohash.utils

import io.bluetape4k.geohash.GeoHash

fun boundingBoxGeoHashIteratorOf(boundingBox: TwoGeoHashBoundingBox): BoundingBoxGeoHashIterator {
    return BoundingBoxGeoHashIterator(boundingBox)
}

fun boundingBoxGeoHashIteratorOf(southWest: GeoHash, northEast: GeoHash): BoundingBoxGeoHashIterator {
    return BoundingBoxGeoHashIterator(twoGeoHashBoundingBoxOf(southWest, northEast))
}

class BoundingBoxGeoHashIterator(val boundingBox: TwoGeoHashBoundingBox): Iterator<GeoHash> {

    private var current: GeoHash? = boundingBox.southWestCorner

    override fun hasNext(): Boolean {
        return current != null
    }

    override fun next(): GeoHash {
        if (!hasNext()) {
            throw NoSuchElementException()
        }

        val rv: GeoHash = current ?: throw NoSuchElementException()

        if (rv == boundingBox.northEastCorner) {
            current = null
        } else {
            current = rv.next()
            while (hasNext() && !boundingBox.boundingBox.contains(current!!.originatingPoint)) {
                current = current?.next()
            }
        }

        return rv
    }
}
