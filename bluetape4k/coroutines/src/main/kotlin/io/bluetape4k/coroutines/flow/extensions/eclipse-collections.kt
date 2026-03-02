package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.collections.eclipse.unifiedMapOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toCollection
import org.eclipse.collections.impl.list.mutable.FastList
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList
import org.eclipse.collections.impl.map.mutable.UnifiedMap
import org.eclipse.collections.impl.set.mutable.UnifiedSet

internal const val INITIAL_CAPACITY = 10

/**
 * Flow를 Eclipse Collections [FastList]로 수집합니다.
 *
 * ## 동작/계약
 * - Flow를 끝까지 수집해 [destination]에 순서대로 추가합니다.
 * - [destination]을 직접 변경(mutate)하고 같은 인스턴스를 반환합니다.
 * - 수신 Flow를 수집(consuming)합니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2, 2, 3).toFastList()
 * // out == [1, 2, 2, 3]
 * ```
 * @param destination 수집 결과를 담을 대상 FastList입니다.
 */
suspend fun <T> Flow<T>.toFastList(destination: FastList<T> = FastList.newList<T>()): FastList<T> =
    toCollection(destination)

/**
 * Flow를 Eclipse Collections [UnifiedSet]으로 수집합니다.
 *
 * ## 동작/계약
 * - Flow를 끝까지 수집해 [destination]에 추가합니다.
 * - Set 특성상 중복 값은 제거됩니다.
 * - [destination]을 직접 변경(mutate)하고 같은 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2, 2, 3).toUnifiedSet()
 * // out == [1, 2, 3]
 * ```
 * @param destination 수집 결과를 담을 대상 UnifiedSet입니다.
 */
suspend fun <T> Flow<T>.toUnifiedSet(destination: UnifiedSet<T> = UnifiedSet.newSet<T>()): UnifiedSet<T> =
    toCollection(destination)

/**
 * Flow를 key/value selector 기반 [UnifiedMap]으로 수집합니다.
 *
 * ## 동작/계약
 * - 각 요소마다 [keySelector], [valueSelector]를 적용해 맵 엔트리를 갱신합니다.
 * - 동일 키가 반복되면 마지막 값으로 덮어씁니다.
 * - [destination]을 직접 변경(mutate)하고 같은 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val out = flowOf("a", "bb", "ccc")
 *     .toUnifiedMap(keySelector = { it.length }, valueSelector = { it.uppercase() })
 * // out[2] == "BB"
 * ```
 * @param destination 수집 결과를 담을 대상 UnifiedMap입니다.
 * @param keySelector 요소에서 키를 계산하는 함수입니다.
 * @param valueSelector 요소에서 값을 계산하는 함수입니다.
 */
suspend fun <T, K, V> Flow<T>.toUnifiedMap(
    destination: UnifiedMap<K, V> = unifiedMapOf(),
    keySelector: (T) -> K,
    valueSelector: (T) -> V,
): UnifiedMap<K, V> {
    collect {
        destination[keySelector(it)] = valueSelector(it)
    }
    return destination
}

/**
 * `Flow<Int>`를 primitive [IntArrayList]로 수집합니다.
 *
 * ## 동작/계약
 * - Flow를 끝까지 수집해 [destination]에 순서대로 추가합니다.
 * - [destination]을 직접 변경(mutate)하고 같은 인스턴스를 반환합니다.
 * - boxing 없는 primitive 리스트에 저장합니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2, 3).toIntArrayList()
 * // out == [1, 2, 3]
 * ```
 * @param destination 수집 결과를 담을 대상 IntArrayList입니다.
 */
suspend fun Flow<Int>.toIntArrayList(
    destination: IntArrayList = IntArrayList(INITIAL_CAPACITY),
): IntArrayList {
    collect {
        destination.add(it)
    }
    return destination
}

/**
 * `Flow<Long>`를 primitive [LongArrayList]로 수집합니다.
 *
 * ## 동작/계약
 * - Flow를 끝까지 수집해 [destination]에 순서대로 추가합니다.
 * - [destination]을 직접 변경(mutate)하고 같은 인스턴스를 반환합니다.
 * - boxing 없는 primitive 리스트에 저장합니다.
 *
 * ```kotlin
 * val out = flowOf(1L, 2L, 3L).toLongArrayList()
 * // out == [1, 2, 3]
 * ```
 * @param destination 수집 결과를 담을 대상 LongArrayList입니다.
 */
suspend fun Flow<Long>.toLongArrayList(
    destination: LongArrayList = LongArrayList(INITIAL_CAPACITY),
): LongArrayList {
    collect {
        destination.add(it)
    }
    return destination
}

/**
 * `Flow<Float>`를 primitive [FloatArrayList]로 수집합니다.
 *
 * ## 동작/계약
 * - Flow를 끝까지 수집해 [destination]에 순서대로 추가합니다.
 * - [destination]을 직접 변경(mutate)하고 같은 인스턴스를 반환합니다.
 * - boxing 없는 primitive 리스트에 저장합니다.
 *
 * ```kotlin
 * val out = flowOf(1f, 2f, 3f).toFloatArrayList()
 * // out == [1.0, 2.0, 3.0]
 * ```
 * @param destination 수집 결과를 담을 대상 FloatArrayList입니다.
 */
suspend fun Flow<Float>.toFloatArrayList(
    destination: FloatArrayList = FloatArrayList(INITIAL_CAPACITY),
): FloatArrayList {
    collect {
        destination.add(it)
    }
    return destination
}

/**
 * `Flow<Double>`를 primitive [DoubleArrayList]로 수집합니다.
 *
 * ## 동작/계약
 * - Flow를 끝까지 수집해 [destination]에 순서대로 추가합니다.
 * - [destination]을 직접 변경(mutate)하고 같은 인스턴스를 반환합니다.
 * - boxing 없는 primitive 리스트에 저장합니다.
 *
 * ```kotlin
 * val out = flowOf(1.0, 2.0, 3.0).toDoubleArrayList()
 * // out == [1.0, 2.0, 3.0]
 * ```
 * @param destination 수집 결과를 담을 대상 DoubleArrayList입니다.
 */
suspend fun Flow<Double>.toDoubleArrayList(
    destination: DoubleArrayList = DoubleArrayList(INITIAL_CAPACITY),
): DoubleArrayList {
    collect {
        destination.add(it)
    }
    return destination
}
