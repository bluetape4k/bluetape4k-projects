package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.collections.tryForEach
import io.bluetape4k.coroutines.flow.exceptions.FlowOperationException
import io.bluetape4k.support.uninitialized
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import org.eclipse.collections.api.multimap.list.ListMultimap
import org.eclipse.collections.api.multimap.list.MutableListMultimap
import org.eclipse.collections.impl.factory.Multimaps
import org.eclipse.collections.impl.map.mutable.UnifiedMap
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.coroutines.cancellation.CancellationException

/**
 * Key 로 Grouping 된 [Flow] 를 표현하는 interface 입니다.
 */
interface GroupedFlow<K: Any, V>: Flow<V> {

    /**
     * The grouped key of the flow
     */
    val key: K
}

/**
 * [GroupedFlow]의 Value들만 묶어, `List<V>` 형태의 요소를 제공하는 [Flow]로 변환합니다.
 */
fun <K: Any, V> GroupedFlow<K, V>.toValues(): Flow<List<V>> = flowFromSuspend { toFastList() }

data class GroupItem<K, V>(
    val key: K,
    val values: List<V>,
): Serializable

/**
 * [GroupedFlow]의 key 와 value들을 묶어, `Pair<K, List<V>>` 형태의 요소를 제공하는 [Flow]로 변환합니다.
 */
fun <K: Any, V> GroupedFlow<K, V>.toGroupItems(): Flow<GroupItem<K, V>> = flowFromSuspend {
    val values = toFastList()
    GroupItem(key, values)
}

/**
 * [GroupedFlow]의 Flow 를 [destination] Map에 `Map<K, List<V>>` 형태로 변환합니다.
 */
suspend fun <K: Any, V> Flow<GroupedFlow<K, V>>.toMap(
    destination: MutableMap<K, List<V>> = UnifiedMap<K, List<V>>(),
): MutableMap<K, List<V>> {
    this
        .flatMapMerge { it.toGroupItems() }
        .collect { groupItem ->
            destination[groupItem.key] = groupItem.values
        }
    return destination
}

/**
 * [GroupedFlow]의 Flow 를 [destination] Map에 `Map<K, List<V>>` 형태로 변환합니다.
 */
suspend fun <K: Any, V> Flow<GroupedFlow<K, V>>.toUnifiedMap(
    destination: MutableMap<K, List<V>> = UnifiedMap<K, List<V>>(),
): MutableMap<K, List<V>> {
    this
        .flatMapMerge { it.toGroupItems() }
        .collect { groupItem ->
            destination[groupItem.key] = groupItem.values
        }
    return destination
}

/**
 * [GroupedFlow]의 Flow 를 [destination] Map에 `Map<K, List<V>>` 형태로 변환합니다.
 */
suspend fun <K: Any, V> Flow<GroupedFlow<K, V>>.toListMultiMap(
    destination: MutableListMultimap<K, V> = Multimaps.mutable.list.with<K, V>(),
): ListMultimap<K, V> {
    this
        .flatMapMerge { it.toGroupItems() }
        .collect { groupItem ->
            destination.putAll(groupItem.key, groupItem.values)
        }
    return destination
}

/**
 * [Flow]의 항목을 [keySelector]로 그룹화하여 `Flow<GroupedFlow<K, T>>`로 변환합니다.
 *
 * ```
 * flowRangeOf(1, 10)
 *     .groupBy { it % 3 }
 *     .flatMapMerge { it.toValues() }
 *     .assertResultSet(listOf(1, 4, 7, 10), listOf(2, 5, 8), listOf(3, 6, 9))
 * ```
 *
 * ### Note [groupBy] 함수 뒤에 [log] 함수를 사용하면 작동이 안됩니다. (버그)
 */
fun <T, K: Any> Flow<T>.groupBy(keySelector: (T) -> K): Flow<GroupedFlow<K, T>> =
    groupByInternal(this, keySelector) { it }

/**
 * [Flow]의 항목을 [keySelector]로 그룹화하고, [valueSelector]로 value를 변환해서 `Flow<GroupedFlow<K, V>>`로 변환합니다.
 *
 * ```
 * flowRangeOf(1, 10)
 *     .groupBy({ it % 2 }) { it + 1 }
 *     .flatMapMerge { it.toValues() }
 *     .assertResultSet(listOf(2, 4, 6, 8, 10), listOf(3, 5, 7, 9, 11))
 * ```
 * ### Note [groupBy] 함수 뒤에 [log] 함수를 사용하면 작동이 안됩니다. (버그)
 */
fun <T, K: Any, V> Flow<T>.groupBy(
    keySelector: (T) -> K,
    valueSelector: (T) -> V,
): Flow<GroupedFlow<K, V>> =
    groupByInternal(this, keySelector, valueSelector)

/**
 * key selector 함수를 기반으로 source flow의 변환된 값들을 그룹화합니다.
 */
@Suppress("SYNTHETIC_PROPERTY_WITHOUT_JAVA_ORIGIN")
@PublishedApi
internal fun <T, K: Any, V> groupByInternal(
    source: Flow<T>,
    keySelector: (T) -> K,
    valueSelector: (T) -> V,
): Flow<GroupedFlow<K, V>> = flow {
    val map = ConcurrentHashMap<K, FlowGroup<K, V>>()
    val state = GroupByState()

    try {
        source.collect {
            val k = keySelector(it)
            var group = map[k]
            if (group != null) {
                group.next(valueSelector(it))
            } else {
                if (!state.mainStopped.value) {
                    group = FlowGroup(k, map)
                    map[k] = group

                    try {
                        emit(group)
                    } catch (e: CancellationException) {
                        state.mainStopped.value = true
                        if (map.isEmpty()) {
                            throw CancellationException()
                        }
                    }
                    group.next(valueSelector(it))
                } else {
                    if (map.isEmpty()) {
                        throw CancellationException()
                    }
                }
            }
        }
        map.values.tryForEach { it.complete() }
    } catch (e: Throwable) {
        map.values.tryForEach { it.error(e) }
        if (e is CancellationException) {
            throw e
        } else {
            throw FlowOperationException("Fail to grouping", e)
        }
    }
}

private class GroupByState {
    val mainStopped = atomic(false)
}

private class FlowGroup<K: Any, V>(
    override val key: K,
    private val map: ConcurrentMap<K, FlowGroup<K, V>>,
): AbstractFlow<V>(), GroupedFlow<K, V> {

    @Volatile
    private var value: V = uninitialized()

    @Volatile
    private var error: Throwable? = null

    private val hasValue = atomic(false)
    private val done = atomic(false)
    private val cancelled = atomic(false)

    private val consumerReady = Resumable()
    private val valueReady = Resumable()

    private val once = atomic(false)

    override suspend fun collectSafely(collector: FlowCollector<V>) {
        if (!once.compareAndSet(expect = false, update = true)) {
            error("A GroupedFlow can only be collected at most once.")
        }

        consumerReady.resume()

        while (true) {

            if (done.value && !hasValue.value) {
                error?.let { throw it }
                break
            }

            if (hasValue.value) {
                val v = value
                value = uninitialized()
                hasValue.value = false

                try {
                    collector.emit(v)
                } catch (e: Throwable) {
                    map.remove(this.key)
                    cancelled.value = true
                    consumerReady.resume()
                    throw e
                }

                consumerReady.resume()
                continue
            }

            valueReady.await()
        }
    }

    suspend fun next(value: V) {
        if (cancelled.value) return

        consumerReady.await()
        this.value = value
        this.hasValue.value = true
        valueReady.resume()
    }

    fun error(ex: Throwable) {
        error = ex
        done.value = true
        valueReady.resume()
    }

    fun complete() {
        done.value = true
        valueReady.resume()
    }
}
