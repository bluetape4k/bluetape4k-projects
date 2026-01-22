package io.bluetape4k.geohash.utils

import io.bluetape4k.geohash.GeoHash
import io.bluetape4k.geohash.stepsBetween
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import java.util.*

class BoundingBoxSampler private constructor(
    val boundingBox: TwoGeoHashBoundingBox,
    private val maxSamples: Int,
    private val rand: Random,
) {
    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(
            bbox: TwoGeoHashBoundingBox,
            seed: Long = System.currentTimeMillis(),
        ): BoundingBoxSampler {
            val maxSamples = bbox.southWestCorner.stepsBetween(bbox.northEastCorner)
            require(maxSamples <= Int.MAX_VALUE) {
                "The number of samples is too large. It must be less than ${Int.MAX_VALUE}"
            }
            return BoundingBoxSampler(
                bbox,
                maxSamples.toInt(),
                Random(seed)
            )
        }
    }

    private val alreadyUsed = hashSetOf<Int>()

    private val maxAttempts = maxSamples * 8

    /**
     * Return next [GeoHash], or NULL if all samples have been returned
     *
     * @return
     */
    fun next(): GeoHash? {
        if (alreadyUsed.size >= maxSamples) {
            return null
        }

        var idx = rand.nextInt(maxSamples + 1)
        var attempt = 0
        while (alreadyUsed.contains(idx) && attempt++ < maxAttempts) {
            idx = rand.nextInt(maxSamples + 1)
        }
        if (alreadyUsed.contains(idx)) {
            log.debug { "임의의 idx 값을 얻는 시도를 너무 많이 해서 중단합니다. maxSamples=$maxSamples, attempt=$attempt" }
            return null
        }
        alreadyUsed.add(idx)
        val gh = boundingBox.southWestCorner.next(idx)
        if (!boundingBox.boundingBox.contains(gh.originatingPoint)) {
            return next()
        }
        return gh
    }
}
