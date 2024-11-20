package io.bluetape4k.geohash.queries

import io.bluetape4k.geohash.GeoHash
import io.bluetape4k.geohash.WGS84Point

interface GeoHashQuery {

    operator fun contains(hash: GeoHash): Boolean

    operator fun contains(point: WGS84Point): Boolean

    fun getSearchHashes(): List<GeoHash>

    fun getWktBox(): String
}
