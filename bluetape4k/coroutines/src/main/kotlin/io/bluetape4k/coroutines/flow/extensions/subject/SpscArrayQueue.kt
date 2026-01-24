package io.bluetape4k.coroutines.flow.extensions.subject

import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicReferenceArray

/**
 * 하나의 Producer와 하나의 Consumer만 사용할 수 있는 큐입니다.
 *
 * ```
 * val q = SpscArrayQueue<Int>(10)
 * val a = Array<Any?>(1) { 0 }
 * repeat(10) {
 *     q.offer(it).shouldBeTrue()
 *     q.isEmpty.shouldBeFalse()
 *     q.poll(a).shouldBeTrue()
 *     q.isEmpty.shouldBeTrue().shouldBeTrue()
 *     a[0] shouldBeEqualTo it
 * }
 * ```
 */
internal class SpscArrayQueue<T> private constructor(capacity: Int) {

    companion object: KLoggingChannel() {

        @JvmStatic
        operator fun <T> invoke(capacity: Int = 1): SpscArrayQueue<T> {
            return SpscArrayQueue(capacity.coerceAtLeast(1))
        }

        private val EMPTY = Any()

        private fun nextPowerOf2(x: Int): Int {
            val h = Integer.highestOneBit(x)
            if (h == x)
                return x
            return h * 2
        }
    }

//    private val referenceArray = atomicArrayOfNulls<Any>(nextPowerOf2(capacity))
//        .apply {
//            repeat(size) {
//                get(it).lazySet(EMPTY)
//            }
//        }

    private val referenceArray =
        AtomicReferenceArray<AtomicReference<Any>>(nextPowerOf2(capacity))
            .apply {
                repeat(length()) {
                    lazySet(it, AtomicReference<Any>(EMPTY))
                }
            }

    private val consumerIndex = AtomicInteger(0)
    private val producerIndex = AtomicInteger(0)

    val isEmpty: Boolean get() = consumerIndex.get() == producerIndex.get()

    fun offer(value: T): Boolean {
        val mask = referenceArray.length() - 1
        val pi = producerIndex.get()

        val offset = pi and mask

        if (referenceArray[offset].get() == EMPTY) {
            referenceArray[offset].lazySet(value)
            producerIndex.set(pi + 1)
            return true
        }
        return false
    }

    fun poll(out: Array<Any?>): Boolean {
        val mask = referenceArray.length() - 1
        val ci = consumerIndex.get()
        val offset = ci and mask

        if (referenceArray[offset].get() == EMPTY) {
            return false
        }
        out[0] = referenceArray[offset].get()
        referenceArray[offset].lazySet(EMPTY)
        consumerIndex.set(ci + 1)
        return true
    }

    fun clear() {
        val mask = referenceArray.length() - 1
        var ci = consumerIndex.get()

        while (true) {
            val offset = ci and mask
            if (referenceArray[offset].get() == EMPTY) {
                break
            }
            referenceArray[offset].lazySet(EMPTY)
            ci++
        }
        consumerIndex.set(ci)
    }
}
