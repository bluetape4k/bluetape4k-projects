package io.bluetape4k.redis.lettuce.filter

/**
 * BloomFilter 설정 옵션.
 *
 * @param expectedInsertions 예상 삽입 원소 수
 * @param falseProbability 허용 오탐률 (0 < p < 1, 양 끝 제외)
 */
data class BloomFilterOptions(
    val expectedInsertions: Long = 1_000_000L,
    val falseProbability: Double = 0.03,
) {
    companion object {
        val Default = BloomFilterOptions()
    }

    init {
        require(expectedInsertions > 0) { "expectedInsertions must be positive" }
        require(falseProbability > 0.0 && falseProbability < 1.0) {
            "falseProbability must be in (0, 1) exclusive — p=0 and p=1 are mathematically invalid for ln(p)"
        }
    }
}
