package io.bluetape4k.redis.lettuce.filter

/**
 * Bloom Filter 구성 옵션입니다.
 *
 * @property expectedInsertions 예상 삽입 원소 수
 * @property falseProbability 허용 오탐률. `(0, 1)` 범위의 배타 구간만 허용합니다.
 */
data class BloomFilterOptions(
    val expectedInsertions: Long = 1_000_000L,
    val falseProbability: Double = 0.03,
) {
    companion object {
        @JvmField
        val Default = BloomFilterOptions()
    }

    init {
        require(expectedInsertions > 0) { "expectedInsertions must be positive" }
        require(falseProbability > 0.0 && falseProbability < 1.0) {
            "falseProbability must be in (0, 1) exclusive — p=0 and p=1 are mathematically invalid for ln(p)"
        }
    }
}
