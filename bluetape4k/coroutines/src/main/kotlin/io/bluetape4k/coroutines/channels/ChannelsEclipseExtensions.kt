package io.bluetape4k.coroutines.channels

import io.bluetape4k.collections.eclipse.emptyFastList
import io.bluetape4k.collections.eclipse.emptyUnifiedSet
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import org.eclipse.collections.impl.list.mutable.FastList
import org.eclipse.collections.impl.set.mutable.UnifiedSet

/**
 * 채널의 모든 요소를 소비해 [destination]에 순서대로 추가하고 반환합니다.
 *
 * [destination]을 지정하지 않으면 빈 [FastList]를 생성해 사용합니다.
 * 이 함수는 채널을 끝까지 소비하며, [consumeEach] 특성상 소비 후 채널을 취소합니다.
 */
suspend fun <E> ReceiveChannel<E>.toFastList(destination: FastList<E> = emptyFastList()): FastList<E> {
    consumeEach { destination.add(it) }
    return destination
}

/**
 * 채널의 모든 요소를 소비해 [destination]에 추가하고 반환합니다.
 *
 * [destination]을 지정하지 않으면 빈 [UnifiedSet]을 생성해 사용합니다.
 * 이 함수는 채널을 끝까지 소비하며, [consumeEach] 특성상 소비 후 채널을 취소합니다.
 */
suspend fun <E> ReceiveChannel<E>.toUnifiedSet(destination: UnifiedSet<E> = emptyUnifiedSet()): UnifiedSet<E> {
    consumeEach { destination.add(it) }
    return destination
}
