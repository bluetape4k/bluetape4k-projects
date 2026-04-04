package io.bluetape4k.concurrent

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireInRange
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.updateAndGet

/**
 * [AtomicIntRoundrobin] 은 [maximum] 값까지의 순환 카운터를 제공합니다.
 *
 * ```kotlin
 * val atomic = AtomicIntRoundrobin(4)
 * atomic.get() shouldBeEqualTo 0
 * val nums = List(8) { atomic.next() }  // (1, 2, 3, 0, 1, 2, 3, 0)
 * ```
 *
 * @param maximum 최대 값
 */
class AtomicIntRoundrobin private constructor(val maximum: Int) {

    companion object: KLogging() {

        /** 기본 최대값 */
        private const val DEFAULT_MAXIMUM = 16

        @JvmStatic
        operator fun invoke(maximum: Int = DEFAULT_MAXIMUM): AtomicIntRoundrobin =
            maximum
                .also { it.requirePositiveNumber("maximum") }
                .let(::AtomicIntRoundrobin)
    }

    private val counter = atomic(0)

    /**
     * 현재 순환 카운터 값을 반환합니다.
     *
     * ```kotlin
     * val rr = AtomicIntRoundrobin(4)
     * rr.get() // 0
     * rr.next()
     * rr.get() // 1
     * ```
     */
    fun get(): Int = counter.value % maximum

    /**
     * 현재 값을 설정합니다.
     *
     * ```kotlin
     * val rr = AtomicIntRoundrobin(4)
     * rr.set(2)
     * rr.get() // 2
     * ```
     *
     * @param value 설정 할 값 (0 until maximum)
     */
    fun set(value: Int) {
        value.requireInRange(0, maximum - 1, "value")
        counter.value = value
    }

    /**
     * 카운터를 1 증가시키고 새 값을 반환합니다. [maximum] 이상이라면 0으로 초기화합니다.
     *
     * ```kotlin
     * val rr = AtomicIntRoundrobin(3)
     * val values = List(6) { rr.next() }
     * // values == [1, 2, 0, 1, 2, 0]
     * ```
     */
    fun next(): Int = counter.updateAndGet { (it + 1) % maximum }
}
