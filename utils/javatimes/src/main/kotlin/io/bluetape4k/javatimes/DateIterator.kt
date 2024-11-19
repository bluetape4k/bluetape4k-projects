package io.bluetape4k.javatimes

import java.util.*

/**
 * [Date]를 열거하는 Iterator 입니다.
 *
 * @param T [Date]의 하위 클래스
 */
abstract class DateIterator<out T: Date>: Iterator<T> {

    /**
     * 다음 [Date]를 반환합니다.
     */
    abstract fun nextDate(): T

    /**
     * 다음 [T] 반환합니다. (Date를 상속받는 클래스)
     */
    final override fun next(): T = nextDate()
}
