package io.bluetape4k.probabilistic.bloomfilter

/**
 * 코루틴 호출 지점에서 사용할 수 있는 Bloom Filter 계약입니다.
 *
 * 현재 인메모리 구현은 I/O를 수행하지 않는 비블로킹 메모리 연산이며, dispatcher 전환을 수행하지 않습니다.
 */
interface SuspendBloomFilter<T: Any> {

    /** 필터 생성 시 가정한 예상 삽입 원소 수입니다. */
    val expectedInsertions: Long

    /** 필터 생성 시 목표로 한 오탐률입니다. */
    val falsePositiveProbability: Double

    /** 내부 bitset 크기입니다. */
    val bitSize: Long

    /** 원소 하나당 계산하는 해시 offset 개수입니다. */
    val hashFunctionCount: Int

    /** 현재 켜진 bit 개수입니다. */
    val bitCount: Long

    /** 필터가 비어 있는지 여부입니다. */
    val isEmpty: Boolean get() = bitCount == 0L

    /** 원소 포함 가능성을 검사합니다. */
    suspend fun mightContain(element: T): Boolean

    /**
     * 원소를 필터에 추가합니다.
     *
     * @return 하나 이상의 bit가 새로 켜졌으면 `true`
     */
    suspend fun put(element: T): Boolean

    /** 현재 bit 포화도를 기준으로 삽입 원소 수를 근사 계산합니다. */
    suspend fun approximateElementCount(): Long

    /** 현재 bit 포화도를 기준으로 기대 오탐률을 계산합니다. */
    suspend fun expectedFpp(): Double

    /** 필터 상태를 초기화합니다. */
    suspend fun clear()
}
