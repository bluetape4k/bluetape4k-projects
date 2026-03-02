package io.bluetape4k.coroutines

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireInOpenRange
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 코루틴 환경에서 안전하게 사용할 수 있는 고정 크기 링 버퍼입니다.
 *
 * ## 동작/계약
 * - `push()`는 버퍼가 가득 찬 경우 가장 오래된 값을 덮어씁니다.
 * - `get()/snapshot()/push()`는 내부 `Mutex`로 직렬화되어 동시 접근 시 일관성을 보장합니다.
 * - 버퍼 용량은 `buffer.size`로 고정되며, `size`는 `0..buffer.size` 범위를 유지합니다.
 * - `iterator()`는 내부적으로 `runBlocking`을 사용해 스냅샷을 만든 뒤 반복자를 반환합니다.
 *
 * ```kotlin
 * val ring = SuspendRingBuffer.boxing<Int>(3)
 * ring.push(1); ring.push(2); ring.push(3); ring.push(4)
 * val snapshot = ring.snapshot()
 * // snapshot == [2, 3, 4]
 * ```
 */
@Suppress("UNCHECKED_CAST")
class SuspendRingBuffer<T: Any>(
    private val buffer: MutableList<T?>,
    private var startIndex: Int = 0,
    _size: Int = 0,
): Iterable<T?> by buffer {

    companion object: KLogging() {
        /**
         * 초기값으로 채워진 고정 크기 링 버퍼를 생성합니다.
         *
         * ## 동작/계약
         * - 생성 시 용량 `size`의 리스트를 allocate 하고 모든 칸을 `empty` 값으로 채웁니다.
         * - 초기 `size`는 `0`이며, 값은 `push()` 호출 시점부터 유효 데이터로 간주됩니다.
         * - `size <= 0`이면 `requirePositiveNumber("size")` 검증에 의해 예외가 발생합니다.
         *
         * ```kotlin
         * val ring = SuspendRingBuffer(size = 4, empty = 0)
         * // ring.size == 0
         * ```
         * @param size 버퍼 용량이며 1 이상의 값이어야 합니다.
         * @param empty 내부 버퍼를 채울 초기 플레이스홀더 값입니다.
         */
        @JvmStatic
        operator fun <T: Any> invoke(size: Int, empty: T): SuspendRingBuffer<T> {
            size.requirePositiveNumber("size")
            val list = MutableList(size) { empty } as MutableList<T?>
            return SuspendRingBuffer(list)
        }

        /**
         * nullable 슬롯 기반의 빈 링 버퍼를 생성합니다.
         *
         * ## 동작/계약
         * - 생성 시 용량 `size`의 nullable 리스트를 allocate 하고 모든 칸을 `null`로 초기화합니다.
         * - 초기 `size`는 `0`이며, `snapshot()`은 추가된 값만 순서대로 반환합니다.
         * - `size <= 0`이면 `requirePositiveNumber("size")` 검증에 의해 예외가 발생합니다.
         *
         * ```kotlin
         * val ring = SuspendRingBuffer.boxing<Int>(2)
         * // ring.size == 0
         * ```
         * @param size 버퍼 용량이며 1 이상의 값이어야 합니다.
         */
        fun <T: Any> boxing(size: Int): SuspendRingBuffer<T> {
            size.requirePositiveNumber("size")
            val list: MutableList<T?> = MutableList(size) { null }
            return SuspendRingBuffer(list)
        }
    }

    private val mutex: Mutex = Mutex()

    /**
     * 현재 저장된 원소 개수입니다.
     */
    var size: Int = _size
        private set

    /**
     * 버퍼가 가득 찼는지 여부를 반환합니다.
     */
    val isFull: Boolean get() = size == buffer.size

    /**
     * 현재 버퍼 관점의 상대 인덱스로 값을 조회합니다.
     *
     * ## 동작/계약
     * - `index`는 항상 `0 <= index < size` 범위를 만족해야 합니다.
     * - 범위를 벗어나면 `requireInOpenRange(0, size, "index")` 검증에 의해 예외가 발생합니다.
     * - 조회는 `Mutex` 임계 구역에서 수행되어 동시 push 중에도 일관된 값을 반환합니다.
     *
     * ```kotlin
     * val value = ring.get(0)
     * // value == ring.snapshot().first()
     * ```
     * @param index 현재 저장 구간(`0..size-1`) 기준의 상대 인덱스입니다.
     */
    suspend fun get(index: Int): T = mutex.withLock {
        index.requireInOpenRange(0, size, "index")
        buffer[startIndex.forward(index)] as T
    }

    /**
     * 스냅샷 기반 반복자를 반환합니다.
     *
     * ## 동작/계약
     * - 내부적으로 `snapshot()` 결과 리스트를 만들고 그 반복자를 반환합니다.
     * - 동기 함수 시그니처를 유지하기 위해 `runBlocking`을 사용합니다.
     * - 반복 도중 원본 버퍼가 변경되어도 반복 대상은 스냅샷에 고정됩니다.
     *
     * ```kotlin
     * val it = ring.iterator()
     * // it == ring.snapshot().iterator()
     * ```
     */
    override fun iterator(): Iterator<T> {
        return runBlocking { snapshot().iterator() }
    }

    /**
     * 현재 버퍼 내용을 논리 순서대로 복사한 리스트를 반환합니다.
     *
     * ## 동작/계약
     * - 결과 리스트는 새로 allocate 되며 원본 버퍼와 독립적입니다.
     * - 시간 복잡도는 `O(size)`이며, `size == 0`이면 `emptyList()`를 반환합니다.
     * - 복사는 `Mutex` 임계 구역에서 수행되어 일관된 시점을 보장합니다.
     *
     * ```kotlin
     * val result = snapshot()
     * // result == [저장된 요소들의 현재 순서]
     * ```
     */
    suspend fun snapshot(): List<T> = mutex.withLock {
        if (size == 0) {
            return@withLock emptyList()
        }

        val result = ArrayList<T>(size)
        repeat(size) { offset ->
            result += buffer[startIndex.forward(offset)] as T
        }
        result
    }

    /**
     * 버퍼 끝에 새 값을 추가합니다.
     *
     * ## 동작/계약
     * - 버퍼가 비어 있지 않으면 마지막 위치 다음 슬롯에 값을 기록합니다.
     * - 버퍼가 가득 찬 상태에서 추가하면 가장 오래된 값이 덮어써지고 시작 인덱스가 한 칸 이동합니다.
     * - 연산은 `Mutex` 임계 구역에서 수행되며 시간 복잡도는 `O(1)`입니다.
     *
     * ```kotlin
     * ring.push(10)
     * val result = ring.snapshot()
     * // result == [기존 요소..., 10]
     * ```
     * @param element 버퍼에 추가할 값입니다.
     */
    suspend fun push(element: T) {
        mutex.withLock {
            buffer[startIndex.forward(size)] = element

            if (isFull) startIndex = startIndex.forward(1)
            else size++
        }
    }

    private fun Int.forward(n: Int): Int = (this + n) % buffer.size

    override fun toString(): String {
        return buffer.joinToString(prefix = "[", separator = ", ", postfix = "]")
    }
}
