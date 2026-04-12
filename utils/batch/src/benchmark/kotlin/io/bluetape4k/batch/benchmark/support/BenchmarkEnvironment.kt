package io.bluetape4k.batch.benchmark.support

import io.bluetape4k.support.requirePositiveNumber

/**
 * 키셋 기반 파티션 범위를 나타냅니다.
 *
 * @property minKeyExclusive 시작 키의 배타적 하한
 * @property maxKeyInclusive 종료 키의 포함 상한
 */
internal data class KeyRange(
    val minKeyExclusive: Long?,
    val maxKeyInclusive: Long?,
)

/**
 * 벤치마크 실행 환경의 공통 계약입니다.
 */
internal interface BenchmarkEnvironment : AutoCloseable {
    val database: BenchmarkDatabase

    /**
     * 현재 소스 테이블의 최소/최대 키를 반환합니다.
     *
     * 소스 테이블이 비어 있지 않아야 하며, 반환되는 `first <= second` (minKey <= maxKey) 조건을
     * 만족해야 합니다. 이 조건이 충족되지 않으면 [partitionRanges]가 예외를 던집니다.
     */
    fun minMaxKey(): Pair<Long, Long>

    /**
     * 현재 키 범위를 병렬 처리용 파티션으로 분할합니다.
     *
     * 각 파티션은 `key > minKeyExclusive && key <= maxKeyInclusive` 조건으로 사용할 수 있습니다.
     * 첫 번째 파티션은 하한이 없으므로 `minKeyExclusive`가 `null`일 수 있습니다.
     *
     * **사전 조건**: 소스 테이블이 비어 있지 않아야 하며, [minMaxKey]가 반환하는 키 범위가
     * `minKey <= maxKey`를 만족해야 합니다. 빈 테이블이나 역전된 범위는 [IllegalStateException]을
     * 던집니다.
     *
     * @param parallelism 분할할 파티션 수
     * @return 계산된 키 범위 목록
     * @throws IllegalArgumentException parallelism 이 0 이하인 경우
     * @throws IllegalStateException 소스 테이블이 비어 있거나 키 범위가 유효하지 않은 경우
     */
    fun partitionRanges(parallelism: Int): List<KeyRange> {
        parallelism.requirePositiveNumber("parallelism")

        val (minKey, maxKey) = minMaxKey()
        check(maxKey >= minKey) {
            "유효하지 않은 키 범위: minKey=$minKey > maxKey=$maxKey. 소스 테이블이 비어 있거나 minMaxKey()가 잘못된 범위를 반환했습니다."
        }
        val totalKeys = maxKey - minKey + 1
        totalKeys.requirePositiveNumber("totalKeys")

        val partitionCount = minOf(parallelism.toLong(), totalKeys).toInt()
        val baseSize = totalKeys / partitionCount
        val remainder = totalKeys % partitionCount

        var nextMin = minKey
        var previousMax: Long? = null

        return MutableList(partitionCount) { index ->
            val currentSize = baseSize + if (index.toLong() < remainder) 1 else 0
            val currentMax = nextMin + currentSize - 1
            KeyRange(previousMax, currentMax).also {
                previousMax = currentMax
                nextMin = currentMax + 1
            }
        }
    }
}

/**
 * JDBC 기반 벤치마크 실행 환경 계약입니다.
 */
internal interface JdbcBenchmarkEnvironment : BenchmarkEnvironment {
    fun resetSchema()
    fun truncateWorkingTables()
    fun seedSourceRows(dataSize: Int)
    fun runEndToEnd(dataSize: Int, poolSize: Int, parallelism: Int): Int
}

/**
 * R2DBC 기반 벤치마크 실행 환경 계약입니다.
 */
internal interface R2dbcBenchmarkEnvironment : BenchmarkEnvironment {
    suspend fun resetSchema()
    suspend fun truncateWorkingTables()
    suspend fun seedSourceRows(dataSize: Int)
    suspend fun runEndToEnd(dataSize: Int, poolSize: Int, parallelism: Int): Int
}
