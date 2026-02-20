package io.bluetape4k.coroutines.flow.extensions.subject

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.atomicfu.atomic
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

    private val referenceArray =
        AtomicReferenceArray<Any?>(nextPowerOf2(capacity))
            .apply {
                repeat(length()) {
                    lazySet(it, EMPTY)
                }
            }
    private val mask = referenceArray.length() - 1

    private val consumerIndex = atomic(0)
    private val producerIndex = atomic(0)

    val isEmpty: Boolean get() = consumerIndex.value == producerIndex.value

    fun offer(value: T): Boolean {
        val pi = producerIndex.value

        val offset = pi and mask

        if (referenceArray.get(offset) != EMPTY) {
            return false
        }
        referenceArray.lazySet(offset, value)
        producerIndex.value = pi + 1
        return true
    }

    fun poll(out: Array<Any?>): Boolean {
        val ci = consumerIndex.value
        val offset = ci and mask

        val value = referenceArray.get(offset)
        if (value == EMPTY) {
            return false
        }
        out[0] = value
        referenceArray.lazySet(offset, EMPTY)
        consumerIndex.value = ci + 1
        return true
    }

    fun clear() {
        if (consumerIndex.value == producerIndex.value) {
            return
        }
        var ci = consumerIndex.value

        while (true) {
            val offset = ci and mask
            if (referenceArray.get(offset) == EMPTY) {
                break
            }
            referenceArray.lazySet(offset, EMPTY)
            ci++
        }
        consumerIndex.value = ci
    }
}
