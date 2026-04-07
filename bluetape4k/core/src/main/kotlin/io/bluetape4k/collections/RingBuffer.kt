package io.bluetape4k.collections

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 제한된 크기를 가지는 링 버퍼 (Ring Buffer).
 *
 * 최대 [maxSize]개의 요소를 저장할 수 있으며, 초과 시 가장 오래된 요소가 덮어씌워집니다.
 * 모든 변경/읽기 연산은 [ReentrantLock]으로 보호되어 thread-safe 합니다.
 *
 * 예제:
 * ```kotlin
 * val buffer = RingBuffer<String>(maxSize = 3)
 * buffer.add("a")
 * buffer.add("b")
 * buffer.add("c")
 * buffer.add("d")  // "a"가 제거되고 "d" 추가
 * buffer.toList()  // ["b", "c", "d"]
 * ```
 *
 * @param E 요소 타입
 * @param maxSize 버퍼의 최대 크기 (1 이상)
 */
class RingBuffer<E>(val maxSize: Int): Iterable<E> {

    init {
        require(maxSize > 0) { "maxSize must be positive" }
    }

    @Suppress("UNCHECKED_CAST")
    private val array: Array<Any?> = arrayOfNulls(maxSize)
    private val lock = ReentrantLock()

    private var read = 0
    private var write = 0
    private var _size = 0

    /**
     * 버퍼에 저장된 요소의 수를 반환합니다.
     *
     * 예제:
     * ```kotlin
     * val buffer = RingBuffer<Int>(maxSize = 4)
     * buffer.addAll(1, 2, 3)
     * buffer.size  // 3
     * ```
     */
    val size: Int get() = lock.withLock { _size }

    /**
     * 버퍼가 비어 있는지 여부를 반환합니다.
     *
     * 예제:
     * ```kotlin
     * val buffer = RingBuffer<Int>(maxSize = 4)
     * buffer.isEmpty  // true
     * buffer.add(1)
     * buffer.isEmpty  // false
     * ```
     */
    val isEmpty: Boolean get() = lock.withLock { _size == 0 }

    /**
     * 버퍼에 [item]을 추가합니다.
     * 버퍼가 가득 찬 경우, 가장 오래된 요소가 덮어씌워집니다.
     *
     * 예제:
     * ```kotlin
     * val buffer = RingBuffer<String>(maxSize = 3)
     * buffer.add("a")
     * buffer.add("b")
     * buffer.add("c")
     * buffer.add("d")  // "a"가 제거되고 "d" 추가 (overflow)
     * buffer.toList()  // ["b", "c", "d"]
     * ```
     *
     * @param item 추가할 요소
     * @return 항상 true
     */
    fun add(item: E): Boolean = lock.withLock {
        array[write] = item
        write = (write + 1) % maxSize

        if (_size == maxSize) {
            read = (read + 1) % maxSize
        } else {
            _size++
        }
        true
    }

    /**
     * 여러 요소를 순서대로 추가합니다.
     *
     * 예제:
     * ```kotlin
     * val buffer = RingBuffer<Int>(maxSize = 4)
     * buffer.addAll(1, 2, 3)
     * buffer.toList()  // [1, 2, 3]
     * buffer.addAll(4, 5)  // maxSize 초과 시 오래된 요소 제거
     * buffer.toList()  // [2, 3, 4, 5]
     * ```
     *
     * @param elements 추가할 요소들
     * @return 항상 true
     */
    fun addAll(vararg elements: E): Boolean = lock.withLock {
        elements.forEach { addInternal(it) }
        true
    }

    /**
     * 컬렉션의 모든 요소를 순서대로 추가합니다.
     *
     * 예제:
     * ```kotlin
     * val buffer = RingBuffer<String>(maxSize = 3)
     * buffer.addAll(listOf("a", "b", "c", "d"))  // maxSize 초과 시 오래된 요소 제거
     * buffer.toList()  // ["b", "c", "d"]
     * ```
     *
     * @param elements 추가할 요소 컬렉션
     * @return 항상 true
     */
    fun addAll(elements: Collection<E>): Boolean = lock.withLock {
        elements.forEach { addInternal(it) }
        true
    }

    /**
     * 지정한 [index] 위치의 요소를 반환합니다.
     * index 0이 가장 오래된 (먼저 추가된) 요소입니다.
     *
     * 예제:
     * ```kotlin
     * val buffer = RingBuffer<String>(maxSize = 4)
     * buffer.addAll(listOf("a", "b", "c"))
     * buffer[0]  // "a" (가장 오래된 요소)
     * buffer[2]  // "c" (가장 최근 요소)
     * buffer[3]  // IndexOutOfBoundsException 발생
     * ```
     *
     * @param index 0부터 시작하는 인덱스
     * @throws IndexOutOfBoundsException [index]가 범위를 벗어나는 경우
     */
    operator fun get(index: Int): E = lock.withLock {
        if (index < 0 || index >= _size) {
            throw IndexOutOfBoundsException(index.toString())
        }
        @Suppress("UNCHECKED_CAST")
        array[(read + index) % maxSize] as E
    }

    /**
     * 지정한 [index] 위치의 요소를 [elem]으로 교체합니다.
     *
     * 예제:
     * ```kotlin
     * val buffer = RingBuffer<String>(maxSize = 4)
     * buffer.addAll(listOf("a", "b", "c"))
     * buffer[1] = "x"
     * buffer.toList()  // ["a", "x", "c"]
     * ```
     *
     * @param index 교체할 위치
     * @param elem 새 요소
     * @throws IndexOutOfBoundsException [index]가 범위를 벗어나는 경우
     */
    operator fun set(index: Int, elem: E): Unit = lock.withLock {
        if (index !in 0..<_size) {
            throw IndexOutOfBoundsException(index.toString())
        }
        array[(read + index) % maxSize] = elem
    }

    /**
     * 버퍼의 앞에서 [n]개의 요소를 제거합니다.
     *
     * 예제:
     * ```kotlin
     * val buffer = RingBuffer<Int>(maxSize = 5)
     * buffer.addAll(1, 2, 3, 4, 5)
     * buffer.drop(2)
     * buffer.toList()  // [3, 4, 5]
     *
     * buffer.drop(10)  // n >= size이면 전체 제거
     * buffer.isEmpty   // true
     * ```
     *
     * @param n 제거할 요소 수
     * @return 이 버퍼 자신
     */
    fun drop(n: Int): RingBuffer<E> = lock.withLock {
        if (n >= _size) {
            clearInternal()
        } else {
            read = (read + n) % maxSize
            _size -= n
        }
        this
    }

    /**
     * [predicate] 조건을 만족하는 요소를 제거합니다.
     *
     * 예제:
     * ```kotlin
     * val buffer = RingBuffer<Int>(maxSize = 6)
     * buffer.addAll(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9))
     * buffer.toList()        // [4, 5, 6, 7, 8, 9]
     * buffer.removeIf { it % 3 == 0 }  // 3의 배수 제거
     * buffer.toList()        // [4, 5, 7, 8]
     * ```
     *
     * @param predicate 제거 조건
     * @return 하나 이상 제거되었으면 true
     */
    fun removeIf(predicate: (E) -> Boolean): Boolean = lock.withLock {
        var removeCount = 0
        var j = 0

        repeat(_size) {
            @Suppress("UNCHECKED_CAST")
            val elem = array[(read + it) % maxSize] as E
            if (predicate(elem)) {
                removeCount++
            } else {
                if (j < it) {
                    array[(read + j) % maxSize] = elem
                }
                j++
            }
        }

        _size -= removeCount
        write = (read + _size) % maxSize
        removeCount > 0
    }

    /**
     * 버퍼의 가장 앞(오래된) 요소를 제거하고 반환합니다.
     *
     * 예제:
     * ```kotlin
     * val buffer = RingBuffer<String>(maxSize = 4)
     * buffer.addAll(listOf("a", "b", "c"))
     * buffer.next()   // "a" (제거 후 반환)
     * buffer.next()   // "b"
     * buffer.size     // 1
     * ```
     *
     * @return 제거된 요소
     * @throws NoSuchElementException 버퍼가 비어 있는 경우
     */
    fun next(): E = lock.withLock {
        if (_size == 0) {
            throw NoSuchElementException("RingBuffer is empty")
        }
        val oldRead = read

        @Suppress("UNCHECKED_CAST")
        val result = array[oldRead] as E
        array[oldRead] = null
        read = (read + 1) % maxSize
        _size--
        result
    }

    /**
     * 버퍼의 모든 요소를 제거합니다.
     *
     * 예제:
     * ```kotlin
     * val buffer = RingBuffer<String>(maxSize = 4)
     * buffer.addAll(listOf("a", "b", "c"))
     * buffer.size     // 3
     * buffer.clear()
     * buffer.size     // 0
     * buffer.isEmpty  // true
     * ```
     */
    fun clear(): Unit = lock.withLock {
        clearInternal()
    }

    /**
     * 버퍼의 요소를 순서대로 (오래된 순) [List]로 반환합니다.
     *
     * 예제:
     * ```kotlin
     * val buffer = RingBuffer<Int>(maxSize = 5)
     * buffer.addAll(1, 2, 3, 4, 5)
     * buffer.toList()  // [1, 2, 3, 4, 5]
     *
     * buffer.add(6)    // overflow: 1 제거
     * buffer.toList()  // [2, 3, 4, 5, 6]
     * ```
     *
     * @return 요소 리스트
     */
    fun toList(): List<E> = lock.withLock {
        val list = ArrayList<E>(_size)
        repeat(_size) {
            @Suppress("UNCHECKED_CAST")
            list.add(array[(read + it) % maxSize] as E)
        }
        list
    }

    /**
     * 버퍼의 요소를 [Array]로 반환합니다.
     *
     * @return 요소 배열
     */
    @PublishedApi
    internal fun toListInternal(): List<E> = toList()

    inline fun <reified T> toArray(): Array<T?> {
        val snapshot = toListInternal()
        val result = arrayOfNulls<T>(snapshot.size)
        @Suppress("UNCHECKED_CAST")
        snapshot.forEachIndexed { i, e -> result[i] = e as T }
        return result
    }

    override fun iterator(): Iterator<E> {
        val snapshot = toList()
        return snapshot.iterator()
    }

    /**
     * lock을 이미 획득한 상태에서 내부적으로 add를 수행합니다.
     */
    private fun addInternal(item: E) {
        array[write] = item
        write = (write + 1) % maxSize

        if (_size == maxSize) {
            read = (read + 1) % maxSize
        } else {
            _size++
        }
    }

    /**
     * lock을 이미 획득한 상태에서 내부적으로 clear를 수행합니다.
     */
    private fun clearInternal() {
        array.fill(null)
        read = 0
        write = 0
        _size = 0
    }
}
