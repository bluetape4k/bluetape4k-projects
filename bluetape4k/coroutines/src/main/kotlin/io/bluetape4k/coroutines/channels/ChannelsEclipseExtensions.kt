package io.bluetape4k.coroutines.channels

import io.bluetape4k.collections.eclipse.emptyFastList
import io.bluetape4k.collections.eclipse.emptyUnifiedSet
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import org.eclipse.collections.impl.list.mutable.FastList
import org.eclipse.collections.impl.set.mutable.UnifiedSet

/**
 * 채널의 모든 요소를 [FastList]에 모읍니다.
 *
 * ## 동작/계약
 * - `consumeEach`를 사용하므로 수집이 끝나면 채널을 consume(종료)합니다.
 * - 전달된 `destination`을 직접 변경(mutate)해 채우고 같은 인스턴스를 반환합니다.
 * - 채널/소비 중 예외는 호출자에게 전파됩니다.
 *
 * ```kotlin
 * val list = channel.toFastList()
 * // list에는 채널에서 받은 순서대로 값이 들어간다.
 * ```
 *
 * @param destination 결과를 누적할 대상 리스트입니다.
 */
suspend fun <E> ReceiveChannel<E>.toFastList(destination: FastList<E> = emptyFastList()): FastList<E> {
    consumeEach { destination.add(it) }
    return destination
}

/**
 * 채널의 모든 요소를 [UnifiedSet]에 모읍니다.
 *
 * ## 동작/계약
 * - `consumeEach`로 채널을 끝까지 소비합니다.
 * - 전달된 `destination` 집합을 직접 변경하고 동일 인스턴스를 반환합니다.
 * - 집합 특성상 중복 값은 자동 제거됩니다.
 *
 * ```kotlin
 * val set = channel.toUnifiedSet()
 * // set에는 채널 값의 중복 제거 결과가 들어간다.
 * ```
 *
 * @param destination 결과를 누적할 대상 집합입니다.
 */
suspend fun <E> ReceiveChannel<E>.toUnifiedSet(destination: UnifiedSet<E> = emptyUnifiedSet()): UnifiedSet<E> {
    consumeEach { destination.add(it) }
    return destination
}
