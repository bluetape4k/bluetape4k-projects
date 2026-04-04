package io.bluetape4k.rule.core

import java.lang.reflect.Method
import java.util.*

/**
 * Action 메서드와 실행 순서 정보를 가지는 클래스입니다. ([RuleProxy]에서 사용)
 *
 * ```kotlin
 * // @Action(order = 1), @Action(order = 2) 어노테이션된 메서드들을
 * // RuleProxy가 ActionMethodOrderBean으로 수집하여 순서대로 실행합니다.
 * val bean1 = ActionMethodOrderBean(method1, order = 1)
 * val bean2 = ActionMethodOrderBean(method2, order = 2)
 * val sorted = sortedSetOf(bean1, bean2)
 * ```
 *
 * @property method Action 메서드
 * @property order 실행 순서
 */
class ActionMethodOrderBean(
    val method: Method,
    val order: Int,
): Comparable<ActionMethodOrderBean> {

    override fun compareTo(other: ActionMethodOrderBean): Int = when {
        order < other.order -> -1
        order > other.order -> 1
        else -> when (method) {
            other.method -> 0
            else -> 1
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActionMethodOrderBean) return false
        return method == other.method && order == other.order
    }

    override fun hashCode(): Int = Objects.hash(method, order)
}
