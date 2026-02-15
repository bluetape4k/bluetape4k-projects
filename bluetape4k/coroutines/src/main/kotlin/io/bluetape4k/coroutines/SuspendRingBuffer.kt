package io.bluetape4k.coroutines

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireInOpenRange
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coroutines 환경에서 사용하는 RingBuffer 입니다.
 */
@Suppress("UNCHECKED_CAST")
class SuspendRingBuffer<T: Any>(
    private val buffer: MutableList<T?>,
    private var startIndex: Int = 0,
    _size: Int = 0,
): Iterable<T?> by buffer {

    companion object: KLogging() {
        @JvmStatic
        operator fun <T: Any> invoke(size: Int, empty: T): SuspendRingBuffer<T> {
            size.requirePositiveNumber("size")
            val list = MutableList(size) { empty } as MutableList<T?>
            return SuspendRingBuffer(list)
        }

        /**
         * nullable 값을 저장할 수 있는 boxing ring buffer를 생성합니다.
         */
        fun <T: Any> boxing(size: Int): SuspendRingBuffer<T> {
            size.requirePositiveNumber("size")
            val list: MutableList<T?> = MutableList(size) { null }
            return SuspendRingBuffer(list)
        }
    }

    private val mutex: Mutex = Mutex()

    /**
     * RingBuffer 에 담은 갯수 (덮어쓴 것은 무시된다)
     */
    var size: Int = _size
        private set

    /**
     * 버퍼에 모든 요소가 채워졌는지 여부
     */
    val isFull: Boolean get() = size == buffer.size

    suspend fun get(index: Int): T = mutex.withLock {
        index.requireInOpenRange(0, size, "index")
        buffer[startIndex.forward(index)] as T
    }

    override fun iterator(): Iterator<T> {
        return runBlocking { snapshot().iterator() }
    }

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
