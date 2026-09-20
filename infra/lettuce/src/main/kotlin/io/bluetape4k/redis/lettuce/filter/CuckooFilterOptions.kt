package io.bluetape4k.redis.lettuce.filter

import io.bluetape4k.support.requireInRange
import io.bluetape4k.support.requirePositiveNumber

/**
 * Cuckoo Filter 구성 옵션입니다.
 *
 * ```kotlin
 * val opts = CuckooFilterOptions(capacity = 500_000L, bucketSize = 4)
 * val cuckooFilter = LettuceCuckooFilter(connection, "my-cuckoo", opts)
 * cuckooFilter.tryInit()
 * cuckooFilter.insert("hello")
 * val exists = cuckooFilter.contains("hello")
 * // exists == true
 * cuckooFilter.delete("hello")
 * ```
 *
 * @property capacity 최대 원소 수
 * @property bucketSize 버킷당 슬롯 수. `1..8` 범위만 허용합니다.
 * @property maxIterations kick-out 재배치 최대 반복 횟수
 */
data class CuckooFilterOptions(
    val capacity: Long = 1_000_000L,
    val bucketSize: Int = 4,
    val maxIterations: Int = 500,
) {
    companion object {
        @JvmField
        val Default = CuckooFilterOptions()
    }

    init {
        capacity.requirePositiveNumber("capacity")
        bucketSize.requireInRange(1, 8, "bucketSize")
        maxIterations.requirePositiveNumber("maxIterations")
    }
}
