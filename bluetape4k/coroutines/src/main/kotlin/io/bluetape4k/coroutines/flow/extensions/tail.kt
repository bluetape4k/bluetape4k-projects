package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.support.requireZeroOrPositiveNumber
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 정상 완료한 Flow의 마지막 [count]개 요소를 원래 순서대로 방출합니다.
 *
 * ## 동작/계약
 * - 수집마다 독립적인 버퍼에 최대 [count]개를 보관하며 전체 입력을 저장하지 않습니다.
 * - upstream이 정상 완료해야 방출합니다. 무한 Flow에는 결과를 방출하지 않습니다.
 * - `count == 0`이어도 upstream을 수집하여 완료와 오류를 관찰하지만 값을 보관하거나 방출하지 않습니다.
 * - null을 지원하며 upstream 실패·취소 시 보관한 값을 버리고 원래 예외를 전파합니다.
 * - dispatcher를 변경하거나 별도 coroutine을 시작하지 않습니다.
 *
 * ```kotlin
 * val recent = flowOf(1, 2, 3, 4).takeLast(2).toList()
 * // recent == [3, 4]
 * ```
 *
 * @param count 보관할 최대 요소 수입니다. 음수이면 호출 즉시 [IllegalArgumentException]이 발생합니다.
 */
fun <T> Flow<T>.takeLast(count: Int): Flow<T> {
    count.requireZeroOrPositiveNumber("count")
    return flow {
        val pending = ArrayDeque<T>()
        collect { value ->
            currentCoroutineContext().ensureActive()
            if (count > 0) {
                if (pending.size == count) pending.removeFirst()
                pending.addLast(value)
            }
        }
        while (pending.isNotEmpty()) emit(pending.removeFirst())
    }
}

/**
 * Flow의 마지막 [count]개 요소를 제외한 앞부분을 순서대로 방출합니다.
 *
 * ## 동작/계약
 * - 수집마다 최대 [count]개를 보관하며 다음 요소가 도착하면 가장 오래된 요소를 방출합니다.
 * - upstream 완료를 기다리지 않고 방출하므로 무한 Flow에도 사용할 수 있습니다.
 * - `count == 0`이면 원본 Flow를 반환합니다. null 요소도 지원합니다.
 * - 완료·실패·취소 시 보관한 마지막 요소를 방출하지 않으며 원래 예외를 전파합니다.
 * - downstream 방출이 끝날 때까지 다음 upstream 요소를 수집하지 않습니다.
 *
 * ```kotlin
 * val withoutTrailer = flowOf(1, 2, 3, 4).dropLast(2).toList()
 * // withoutTrailer == [1, 2]
 * ```
 *
 * @param count 제외할 마지막 요소 수입니다. 음수이면 호출 즉시 [IllegalArgumentException]이 발생합니다.
 */
fun <T> Flow<T>.dropLast(count: Int): Flow<T> {
    count.requireZeroOrPositiveNumber("count")
    if (count == 0) return this
    return flow {
        val pending = ArrayDeque<T>()
        collect { value ->
            currentCoroutineContext().ensureActive()
            if (pending.size == count) emit(pending.removeFirst())
            pending.addLast(value)
        }
    }
}
