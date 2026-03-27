package io.bluetape4k.redis.lettuce.filter

/**
 * CuckooFilter 설정 옵션.
 *
 * @param capacity 최대 원소 수 (버킷 수 = capacity / bucketSize)
 * @param bucketSize 버킷당 슬롯 수 (1..8)
 * @param maxIterations kick-out 재배치 최대 반복 횟수
 */
data class CuckooFilterOptions(
    val capacity: Long = 1_000_000L,
    val bucketSize: Int = 4,
    val maxIterations: Int = 500,
) {
    companion object {
        val Default = CuckooFilterOptions()
    }

    init {
        require(capacity > 0) { "capacity must be positive" }
        require(bucketSize in 1..8) { "bucketSize must be in [1, 8]" }
        require(maxIterations > 0) { "maxIterations must be positive" }
    }
}
