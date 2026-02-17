package io.bluetape4k.javatimes

import java.time.temporal.Temporal

/**
 * [Temporal] 타입의 값들에 대한 반복자(iterator)
 */
abstract class TemporalIterator<out T: Temporal>: Iterator<T> {

    /**
     * 박싱 없이 시퀀스의 다음 값을 반환합니다.
     */
    abstract fun nextTemporal(): T

    /**
     * 반복에서 다음 요소를 반환합니다.
     */
    final override fun next(): T = nextTemporal()
}
