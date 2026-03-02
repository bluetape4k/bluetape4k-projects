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
        /**
         * 주어진 박스에서 랜덤 샘플링용 [BoundingBoxSampler]를 생성합니다.
         *
         * ## 동작/계약
         * - `southWestCorner.stepsBetween(northEastCorner)` 값을 샘플 상한으로 사용합니다.
         * - 샘플 수가 `Int.MAX_VALUE`를 초과하면 [IllegalArgumentException]이 발생합니다.
         *
         * ```kotlin
         * val sampler = BoundingBoxSampler(bbox, seed = 1L)
         * val sample = sampler.next()
         * // sample != null
         * ```
         */
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
     * 다음 샘플 [GeoHash]를 반환하고, 더 이상 없으면 `null`을 반환합니다.
     *
     * ## 동작/계약
     * - 이미 반환한 인덱스는 재사용하지 않습니다.
     * - 박스 외부에 해당하는 해시는 재시도 후 재귀 호출로 건너뜁니다.
     * - 내부 `alreadyUsed` 상태를 갱신하는 mutate 메서드입니다.
     *
     * ```kotlin
     * val first = sampler.next()
     * val second = sampler.next()
     * // first != second
     * ```
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
