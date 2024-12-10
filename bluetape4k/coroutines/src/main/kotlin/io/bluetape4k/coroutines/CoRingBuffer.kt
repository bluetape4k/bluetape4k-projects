package io.bluetape4k.coroutines


import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Coroutines 환경에서 사용하는 RingBuffer 입니다.
 */
@Suppress("UNCHECKED_CAST")
class CoRingBuffer<T: Any>(
    private val buffer: CopyOnWriteArrayList<T?>,
    private var startIndex: Int = 0,
    size: Int = 0,
): Iterable<T?> by buffer {

    companion object: KLogging() {
        @JvmStatic
        operator fun <T: Any> invoke(size: Int, empty: T): CoRingBuffer<T> {
            val list = MutableList(size) { empty } as MutableList<T?>
            val buffer = CopyOnWriteArrayList(list)
            return CoRingBuffer(buffer)
        }

        @JvmStatic
        fun <T: Any> boxing(size: Int): CoRingBuffer<T> {
            val list: MutableList<T?> = MutableList(size) { null }
            val buffer = CopyOnWriteArrayList(list)
            return CoRingBuffer(buffer)
        }
    }

    private val mutex: Mutex = Mutex()
    private val lock = Mutex()

    var size: Int = size
        private set

    val isFull: Boolean get() = size == buffer.size

    suspend fun get(index: Int): T = mutex.withLock {
        require(index >= 0) { "Index must be positive" }
        require(index < size) { "Index $index is out of circular buffer size $size" }
        buffer[startIndex.forward(index)] as T
    }

    override fun iterator(): Iterator<T> {
        return runBlocking { snapshot().iterator() }
    }

    suspend fun snapshot(): List<T> = mutex.withLock {
        val copy = buffer.toList()
        List(size) { copy[startIndex.forward(it)] as T }
    }

    suspend fun push(element: T) {
        mutex.withLock {
            buffer[startIndex.forward(size)] = element
            if (isFull) startIndex++ else size++
        }
    }

    private fun Int.forward(n: Int): Int = (this + n) % buffer.size

    override fun toString(): String {
        return buffer.joinToString(prefix = "[", separator = ", ", postfix = "]")
    }
}
