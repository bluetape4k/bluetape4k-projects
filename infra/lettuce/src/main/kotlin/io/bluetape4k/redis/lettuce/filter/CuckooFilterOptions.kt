package io.bluetape4k.redis.lettuce.filter

/**
 * Cuckoo Filter 구성 옵션입니다.
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
        require(capacity > 0) { "capacity must be positive" }
        require(bucketSize in 1..8) { "bucketSize must be in [1, 8]" }
        require(maxIterations > 0) { "maxIterations must be positive" }
    }
}
