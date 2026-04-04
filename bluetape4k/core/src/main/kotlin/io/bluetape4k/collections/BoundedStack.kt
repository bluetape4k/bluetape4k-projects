package io.bluetape4k.collections

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 크기에 제한이 있는 스택 (Stack).
 *
 * 최대 [maxSize]개의 요소를 저장할 수 있으며, 초과 시 가장 오래된 요소가 제거됩니다.
 * 모든 변경/읽기 연산은 [ReentrantLock]으로 보호되어 thread-safe 합니다.
 *
 * 사용 예:
 * ```kotlin
 * val stack = BoundedStack<String>(maxSize = 3)
 * stack.push("a")
 * stack.push("b")
 * stack.push("c")
 * stack.push("d")  // maxSize 초과: 가장 오래된 "a" 제거
 * stack.peek()     // "d" (top)
 * stack.pop()      // "d" 반환 및 제거
 * stack.toList()   // ["c", "b"] (top → bottom 순서)
 * ```
 *
 * @param E 요소 타입
 * @param maxSize 스택의 최대 크기 (1 이상)
 */
class BoundedStack<E>(val maxSize: Int): Iterable<E> {

    init {
        require(maxSize > 0) { "maxSize must be positive" }
    }

    @Suppress("UNCHECKED_CAST")
    private val array: Array<Any?> = arrayOfNulls(maxSize)
    private val lock = ReentrantLock()

    private var top = 0
    private var count = 0

    /**
     * 스택에 저장된 요소의 수를 반환합니다.
     *
     * ```kotlin
     * val stack = BoundedStack<String>(4)
     * stack.pushAll("a", "b", "c")
     * stack.size  // 3
     * ```
     */
    val size: Int get() = lock.withLock { count }

    /**
     * 스택이 비어 있는지 여부를 반환합니다.
     *
     * ```kotlin
     * val stack = BoundedStack<String>(4)
     * stack.isEmpty  // true
     * stack.push("a")
     * stack.isEmpty  // false
     * ```
     */
    val isEmpty: Boolean get() = lock.withLock { count == 0 }

    /**
     * 지정한 [index] 위치의 요소를 반환합니다.
     * index 0이 스택의 top (가장 최근 push된 요소) 입니다.
     *
     * ```kotlin
     * val stack = BoundedStack<String>(4)
     * stack.pushAll("a", "b", "c")
     * stack[0]  // "c" (top)
     * stack[1]  // "b"
     * stack[2]  // "a" (bottom)
     * ```
     *
     * @param index 0부터 시작하는 인덱스
     * @throws IndexOutOfBoundsException [index]가 범위를 벗어나는 경우
     */
    operator fun get(index: Int): E = lock.withLock {
        if (index < 0 || index >= count) {
            throw IndexOutOfBoundsException(index.toString())
        }
        @Suppress("UNCHECKED_CAST")
        array[(top + index) % maxSize] as E
    }

    /**
     * 스택의 top에 [item]을 추가합니다.
     * 스택이 가득 찬 경우, 가장 오래된 요소가 제거됩니다.
     *
     * ```kotlin
     * val stack = BoundedStack<String>(3)
     * stack.push("a")  // "a"
     * stack.push("b")  // "b"
     * stack.push("c")  // "c"
     * stack.push("d")  // "d" — maxSize 초과로 "a" 제거됨
     * stack.toList()   // ["d", "c", "b"]
     * ```
     *
     * @param item 추가할 요소
     * @return 추가된 요소
     */
    fun push(item: E): E = lock.withLock {
        top = if (top == 0) maxSize - 1 else top - 1
        array[top] = item
        if (count < maxSize) count++
        item
    }

    /**
     * 여러 요소를 순서대로 push 합니다.
     *
     * ```kotlin
     * val stack = BoundedStack<Int>(5)
     * stack.pushAll(1, 2, 3)
     * stack.toList()  // [3, 2, 1]
     * ```
     *
     * @param items push할 요소들
     */
    fun pushAll(vararg items: E): Unit = lock.withLock {
        items.forEach { pushInternal(it) }
    }

    /**
     * 컬렉션의 모든 요소를 순서대로 push 합니다.
     *
     * ```kotlin
     * val stack = BoundedStack<String>(4)
     * stack.pushAll(listOf("a", "b", "c"))
     * stack.toList()  // ["c", "b", "a"]
     * ```
     *
     * @param elements push할 요소 컬렉션
     */
    fun pushAll(elements: Collection<E>): Unit = lock.withLock {
        elements.forEach { pushInternal(it) }
    }

    /**
     * 스택의 top 요소를 제거하고 반환합니다.
     *
     * ```kotlin
     * val stack = BoundedStack<String>(4)
     * stack.pushAll("a", "b", "c")
     * stack.pop()    // "c" 반환 및 제거
     * stack.pop()    // "b" 반환 및 제거
     * stack.size     // 1
     * ```
     *
     * @return 제거된 top 요소
     * @throws NoSuchElementException 스택이 비어 있는 경우
     */
    fun pop(): E = lock.withLock {
        if (count == 0) {
            throw NoSuchElementException("Stack is empty")
        }
        val oldTop = top

        @Suppress("UNCHECKED_CAST")
        val item = array[oldTop] as E
        array[oldTop] = null
        top = (top + 1) % maxSize
        count--
        item
    }

    /**
     * 스택의 top 요소를 제거하지 않고 반환합니다.
     *
     * ```kotlin
     * val stack = BoundedStack<String>(4)
     * stack.pushAll("a", "b", "c")
     * stack.peek()   // "c" (제거되지 않음)
     * stack.size     // 3 (변화 없음)
     * ```
     *
     * @return top 요소
     * @throws NoSuchElementException 스택이 비어 있는 경우
     */
    fun peek(): E = lock.withLock {
        if (count == 0) {
            throw NoSuchElementException("Stack is empty")
        }
        @Suppress("UNCHECKED_CAST")
        array[top] as E
    }

    /**
     * 지정한 [index] 위치에 [elem]을 삽입합니다.
     * 기존 요소들은 한 칸씩 밀려납니다. index 0에 삽입하면 push와 동일합니다.
     *
     * ```kotlin
     * val stack = BoundedStack<String>(3)
     * stack.pushAll("a", "b")  // top→bottom: ["b", "a"]
     * stack.insert(1, "x")     // 인덱스 1에 삽입 → ["b", "x", "a"]
     * stack.toList()           // ["b", "x", "a"]
     * ```
     *
     * @param index 삽입할 위치 (0 = top)
     * @param elem 삽입할 요소
     * @return 삽입된 요소
     * @throws IndexOutOfBoundsException [index]가 count보다 큰 경우
     */
    fun insert(index: Int, elem: E): E = lock.withLock {
        if (index == 0) {
            return@withLock pushInternal(elem)
        }
        if (index < 0 || index > count) {
            throw IndexOutOfBoundsException(index.toString())
        }
        // 새 top 위치를 확보
        val newTop = if (top == 0) maxSize - 1 else top - 1
        // 기존 요소들을 한 칸씩 앞(top 방향)으로 이동
        for (i in 0 until index) {
            val fromPos = (top + i) % maxSize
            val toPos = (newTop + i) % maxSize
            array[toPos] = array[fromPos]
        }
        top = newTop
        // target 위치에 새 요소 삽입
        array[(top + index) % maxSize] = elem
        if (count < maxSize) count++
        elem
    }

    /**
     * 지정한 [index] 위치의 요소를 [elem]으로 교체합니다.
     *
     * ```kotlin
     * val stack = BoundedStack<String>(4)
     * stack.pushAll("a", "b", "c")  // top→bottom: ["c", "b", "a"]
     * stack.update(1, "z")          // 인덱스 1("b")을 "z"로 교체
     * stack.toList()                // ["c", "z", "a"]
     * ```
     *
     * @param index 교체할 위치
     * @param elem 새 요소
     * @throws IndexOutOfBoundsException [index]가 count보다 큰 경우
     */
    fun update(index: Int, elem: E): Unit = lock.withLock {
        if (index < 0 || index >= count) {
            throw IndexOutOfBoundsException(index.toString())
        }
        array[(top + index) % maxSize] = elem
    }

    /**
     * 스택의 모든 요소를 제거합니다.
     *
     * ```kotlin
     * val stack = BoundedStack<String>(4)
     * stack.pushAll("a", "b", "c")
     * stack.clear()
     * stack.size     // 0
     * stack.isEmpty  // true
     * ```
     */
    fun clear(): Unit = lock.withLock {
        array.fill(null)
        top = 0
        count = 0
    }

    /**
     * 스택의 요소를 top부터 bottom 순서로 [List]로 반환합니다.
     *
     * ```kotlin
     * val stack = BoundedStack<String>(4)
     * stack.pushAll("a", "b", "c")
     * stack.toList()  // ["c", "b", "a"] (top → bottom 순서)
     * ```
     *
     * @return 요소 리스트
     */
    fun toList(): List<E> = lock.withLock {
        val list = ArrayList<E>(count)
        repeat(count) {
            @Suppress("UNCHECKED_CAST")
            list.add(array[(top + it) % maxSize] as E)
        }
        list
    }

    override fun iterator(): Iterator<E> {
        val snapshot = toList()
        return snapshot.iterator()
    }

    /**
     * lock을 이미 획득한 상태에서 내부적으로 push를 수행합니다.
     */
    private fun pushInternal(item: E): E {
        top = if (top == 0) maxSize - 1 else top - 1
        array[top] = item
        if (count < maxSize) count++
        return item
    }
}
