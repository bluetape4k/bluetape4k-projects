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
 * 같은 키로 분류된 하위 Flow를 나타내는 타입입니다.
 *
 * ## 동작/계약
 * - 각 인스턴스는 하나의 [key]와 해당 키에 속한 값 스트림을 나타냅니다.
 * - 구현체(`FlowGroup`) 기준으로 그룹 스트림은 최대 한 번만 수집할 수 있습니다.
 * - 그룹 내부 값은 원본 Flow에서 관측된 순서대로 전달됩니다.
 * - 그룹 생성/해제 시점은 업스트림 데이터와 다운스트림 취소 상태에 따라 달라집니다.
 *
 * ```kotlin
 * val groups = flowRangeOf(1, 4).groupBy { it % 2 }
 * val keys = groups.map { it.key }.toList()
 * // keys contains 1 and 0
 * ```
 */
interface GroupedFlow<K: Any, V>: Flow<V> {

    /** 그룹 키입니다. */
    val key: K
}

/**
 * 그룹의 값을 `List` 하나로 모아 방출하는 Flow로 변환합니다.
 *
 * ## 동작/계약
 * - 그룹 값을 끝까지 수집한 뒤 `List<V>` 1개를 방출합니다.
 * - 수신 그룹 스트림을 소비하며, 수집 후에는 재사용할 수 없습니다.
 * - 값 수에 비례해 리스트 할당이 발생합니다.
 *
 * ```kotlin
 * val values = flowRangeOf(1, 6)
 *     .groupBy { it % 2 }
 *     .flatMapMerge { it.toValues() }
 *     .toList()
 * // values contains [1, 3, 5] and [2, 4, 6]
 * ```
 */
fun <K: Any, V> GroupedFlow<K, V>.toValues(): Flow<List<V>> = flowFromSuspend { toFastList() }

/**
 * 그룹 키와 그룹 값 목록을 함께 담는 직렬화 가능 DTO입니다.
 *
 * ## 동작/계약
 * - [key]는 그룹 식별자, [values]는 해당 키에 매핑된 값 목록입니다.
 * - 불변 데이터 구조이며 생성 후 내부 상태를 변경하지 않습니다.
 * - `equals`/`hashCode`/`toString`은 data class 기본 규칙을 따릅니다.
 *
 * ```kotlin
 * val item = GroupItem(1, listOf(1, 3, 5))
 * // item.values == [1, 3, 5]
 * ```
 */
data class GroupItem<K, V>(
    val key: K,
    val values: List<V>,
): Serializable

/**
 * 그룹을 [GroupItem] 1건으로 변환합니다.
 *
 * ## 동작/계약
 * - 그룹 값을 끝까지 수집한 뒤 [GroupItem]을 1건 방출합니다.
 * - 수신 그룹 스트림을 소비하며, 값 수에 비례해 리스트 할당이 발생합니다.
 * - 수집 중 예외는 그대로 전파됩니다.
 *
 * ```kotlin
 * val items = flowRangeOf(1, 6)
 *     .groupBy { it % 2 }
 *     .flatMapMerge { it.toGroupItems() }
 *     .toList()
 * // items contains GroupItem(1, [1,3,5]) and GroupItem(0, [2,4,6])
 * ```
 */
fun <K: Any, V> GroupedFlow<K, V>.toGroupItems(): Flow<GroupItem<K, V>> = flowFromSuspend {
    val values = toFastList()
    GroupItem(key, values)
}

/**
 * 그룹 Flow를 `Map<K, List<V>>`로 수집합니다.
 *
 * ## 동작/계약
 * - 각 그룹을 `key -> values` 엔트리로 [destination]에 저장합니다.
 * - 같은 키 엔트리는 마지막으로 수집된 그룹 결과로 덮어씁니다.
 * - [destination]을 직접 변경(mutate)한 뒤 같은 인스턴스를 반환합니다.
 * - 그룹 값 크기에 비례해 리스트/맵 할당이 발생합니다.
 *
 * ```kotlin
 * val map = flowRangeOf(1, 10).groupBy { it % 2 }.toMap()
 * // map[0] == [2, 4, 6, 8, 10]
 * ```
 * @param destination 결과를 누적할 대상 맵입니다.
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
 * 그룹 Flow를 UnifiedMap 기반 `Map<K, List<V>>`로 수집합니다.
 *
 * ## 동작/계약
 * - 동작은 [toMap]과 동일하며 기본 destination만 다릅니다.
 * - [destination]을 직접 변경(mutate)한 뒤 같은 인스턴스를 반환합니다.
 * - 같은 키는 마지막 결과로 덮어씁니다.
 *
 * ```kotlin
 * val map = flowRangeOf(1, 10).groupBy { it % 2 }.toUnifiedMap()
 * // map[1] == [1, 3, 5, 7, 9]
 * ```
 * @param destination 결과를 누적할 대상 맵입니다.
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
 * 그룹 Flow를 Eclipse Collections [ListMultimap]으로 수집합니다.
 *
 * ## 동작/계약
 * - 각 그룹 값을 [destination] 멀티맵에 `putAll`로 추가합니다.
 * - [destination]을 직접 변경(mutate)한 뒤 반환합니다.
 * - 같은 키에 여러 값이 누적되며, 그룹 내부 순서를 유지합니다.
 * - 값 수에 비례한 추가 저장 할당이 발생합니다.
 *
 * ```kotlin
 * val mmap = flowRangeOf(1, 10).groupBy { it % 2 }.toListMultiMap()
 * // mmap[0] == [2, 4, 6, 8, 10]
 * ```
 * @param destination 결과를 누적할 대상 멀티맵입니다.
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
 * 원본 Flow를 키 기준으로 동적 그룹화합니다.
 *
 * ## 동작/계약
 * - 새 키가 등장하면 새로운 [GroupedFlow]를 방출하고, 이후 같은 키 값은 해당 그룹으로 전달합니다.
 * - 그룹 Flow는 최대 한 번만 수집할 수 있습니다.
 * - 업스트림 오류는 [FlowOperationException]으로 감싸 전파됩니다.
 * - 다운스트림 취소 시 남은 그룹이 없으면 수집을 조기 종료합니다.
 *
 * ```kotlin
 * val values = flowRangeOf(1, 10)
 *     .groupBy { it % 2 }
 *     .flatMapMerge { it.toValues() }
 *     .toList()
 * // values contains [1, 3, 5, 7, 9] and [2, 4, 6, 8, 10]
 * ```
 * @param keySelector 요소에서 그룹 키를 계산합니다.
 */
fun <T, K: Any> Flow<T>.groupBy(keySelector: (T) -> K): Flow<GroupedFlow<K, T>> =
    groupByInternal(this, keySelector) { it }

/**
 * 원본 Flow를 키/값 변환 규칙으로 동적 그룹화합니다.
 *
 * ## 동작/계약
 * - [keySelector]로 그룹 키를 계산하고 [valueSelector]로 그룹 값을 변환합니다.
 * - 그룹 Flow 생성/수집/오류 전파 규칙은 [groupBy]와 동일합니다.
 * - 업스트림 오류는 [FlowOperationException]으로 감싸 전파됩니다.
 *
 * ```kotlin
 * val values = flowRangeOf(1, 10)
 *     .groupBy({ it % 2 }) { it + 1 }
 *     .flatMapMerge { it.toValues() }
 *     .toList()
 * // values contains [2, 4, 6, 8, 10] and [3, 5, 7, 9, 11]
 * ```
 * @param keySelector 요소에서 그룹 키를 계산합니다.
 * @param valueSelector 요소를 그룹 값으로 변환합니다.
 */
fun <T, K: Any, V> Flow<T>.groupBy(
    keySelector: (T) -> K,
    valueSelector: (T) -> V,
): Flow<GroupedFlow<K, V>> =
    groupByInternal(this, keySelector, valueSelector)

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
