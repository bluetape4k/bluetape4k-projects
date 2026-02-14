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

/** primitive array list 생성 시 기본 초기 용량 */
internal const val INITIAL_CAPACITY = 10

/**
 * Flow 요소를 [FastList]로 수집합니다.
 */
suspend fun <T> Flow<T>.toFastList(destination: FastList<T> = FastList.newList<T>()): FastList<T> =
    toCollection(destination)

/**
 * Flow 요소를 [UnifiedSet]으로 수집합니다.
 */
suspend fun <T> Flow<T>.toUnifiedSet(destination: UnifiedSet<T> = UnifiedSet.newSet<T>()): UnifiedSet<T> =
    toCollection(destination)

/**
 * Flow 요소를 key/value selector로 [UnifiedMap]에 수집합니다.
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

/** Flow<Int> 요소를 [IntArrayList]로 수집합니다. */
suspend fun Flow<Int>.toIntArrayList(
    destination: IntArrayList = IntArrayList(INITIAL_CAPACITY),
): IntArrayList {
    collect {
        destination.add(it)
    }
    return destination
}

/** Flow<Long> 요소를 [LongArrayList]로 수집합니다. */
suspend fun Flow<Long>.toLongArrayList(
    destination: LongArrayList = LongArrayList(INITIAL_CAPACITY),
): LongArrayList {
    collect {
        destination.add(it)
    }
    return destination
}

/** Flow<Float> 요소를 [FloatArrayList]로 수집합니다. */
suspend fun Flow<Float>.toFloatArrayList(
    destination: FloatArrayList = FloatArrayList(INITIAL_CAPACITY),
): FloatArrayList {
    collect {
        destination.add(it)
    }
    return destination
}

/** Flow<Double> 요소를 [DoubleArrayList]로 수집합니다. */
suspend fun Flow<Double>.toDoubleArrayList(
    destination: DoubleArrayList = DoubleArrayList(INITIAL_CAPACITY),
): DoubleArrayList {
    collect {
        destination.add(it)
    }
    return destination
}
