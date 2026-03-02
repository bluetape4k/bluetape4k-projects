package io.bluetape4k.collections.eclipse

import io.bluetape4k.collections.asIterable
import org.eclipse.collections.api.factory.SortedMaps
import org.eclipse.collections.api.map.sorted.ImmutableSortedMap
import org.eclipse.collections.api.map.sorted.MutableSortedMap

/**
 * 비어 있는 immutable sorted map을 생성합니다.
 *
 * ## 동작/계약
 * - [K]는 [Comparable] 구현이 필요합니다.
 * - 항상 새 immutable 맵을 allocate 하며 mutate 하지 않습니다.
 *
 * ```kotlin
 * val map = emptyImmutableSortedMap<Int, String>()
 * check(map.isEmpty)
 * check(map.toMap().isEmpty())
 * ```
 */
fun <K: Comparable<K>, V> emptyImmutableSortedMap(): ImmutableSortedMap<K, V> = SortedMaps.immutable.empty<K, V>()

/**
 * 비어 있는 mutable sorted map을 생성합니다.
 *
 * ## 동작/계약
 * - [K]는 [Comparable] 구현이 필요합니다.
 * - 항상 새 mutable 맵을 allocate 하며 키 정렬 순서를 유지합니다.
 *
 * ```kotlin
 * val map = emptyMutableSortedMap<Int, String>()
 * map[1] = "a"
 * check(map.containsKey(1))
 * ```
 */
fun <K: Comparable<K>, V> emptyMutableSortedMap(): MutableSortedMap<K, V> = SortedMaps.mutable.empty<K, V>()

/**
 * [builder]를 호출해 mutable sorted map을 생성합니다.
 *
 * ## 동작/계약
 * - [size] 횟수만큼 builder를 호출합니다.
 * - 같은 키가 반복되면 마지막 값으로 덮어써집니다.
 * - 항상 새 mutable 맵을 allocate 합니다.
 *
 * ```kotlin
 * val map = mutableSortedMap(2) { it to "v$it" }
 * check(map.size == 2)
 * check(map[0] == "v0")
 * ```
 */
inline fun <K: Comparable<K>, V> mutableSortedMap(
    size: Int,
    @BuilderInference builder: (Int) -> Pair<K, V>,
): MutableSortedMap<K, V> =
    SortedMaps.mutable.of<K, V>().apply {
        repeat(size) { index ->
            add(builder(index).toTuplePair())
        }
    }

/**
 * 가변 인자 [Pair]로 mutable sorted map을 생성합니다.
 *
 * ## 동작/계약
 * - 같은 키가 반복되면 마지막 값으로 덮어써집니다.
 * - 항상 새 mutable 맵을 allocate 합니다.
 * - 기본 비교자는 키의 자연 순서를 사용합니다.
 *
 * ```kotlin
 * val map = mutableSortedMapOf(2 to "b", 1 to "a")
 * check(map.firstKey() == 1)
 * check(map[2] == "b")
 * ```
 */
fun <K: Comparable<K>, V> mutableSortedMapOf(
    vararg pairs: Pair<K, V>,
): MutableSortedMap<K, V> =
    SortedMaps.mutable.of<K, V>().apply {
        pairs.forEach { pair ->
            add(pair.toTuplePair())
        }
    }

/**
 * 사용자 지정 [comparator]로 mutable sorted map을 생성합니다.
 *
 * ## 동작/계약
 * - 키 정렬은 [comparator]를 사용합니다.
 * - 같은 키가 반복되면 마지막 값으로 덮어써집니다.
 * - 항상 새 mutable 맵을 allocate 합니다.
 *
 * ```kotlin
 * val map = mutableSortedMapOf(1 to "a", 2 to "b", comparator = compareByDescending { it })
 * check(map.firstKey() == 2)
 * check(map[1] == "a")
 * ```
 */
fun <K: Comparable<K>, V> mutableSortedMapOf(
    vararg pairs: Pair<K, V>,
    comparator: Comparator<K>,
): MutableSortedMap<K, V> =
    SortedMaps.mutable.of<K, V>(comparator).apply {
        pairs.forEach { pair ->
            add(pair.toTuplePair())
        }
    }

/**
 * 일반 [Map]을 mutable sorted map으로 변환합니다.
 *
 * ## 동작/계약
 * - 수신이 이미 [MutableSortedMap]이면 allocate 없이 그대로 반환합니다.
 * - 그 외에는 새 sorted map을 allocate 해 복사합니다.
 * - 수신 맵은 mutate 하지 않습니다.
 *
 * ```kotlin
 * val map = mapOf(2 to "b", 1 to "a").toMutableSortedMap()
 * check(map.firstKey() == 1)
 * check(map.size == 2)
 * ```
 */
fun <K: Comparable<K>, V> Map<K, V>.toMutableSortedMap(): MutableSortedMap<K, V> = when (this) {
    is MutableSortedMap<K, V> -> this
    else -> SortedMaps.mutable.ofSortedMap<K, V>(this)
}

/**
 * [Pair] iterable을 [destination] sorted map에 누적합니다.
 *
 * ## 동작/계약
 * - [destination]을 직접 mutate 하고 같은 인스턴스를 반환합니다.
 * - 같은 키가 반복되면 마지막 값으로 덮어써집니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * val out = listOf(2 to "b", 1 to "a").toMutableSortedMap()
 * check(out.firstKey() == 1)
 * check(out[2] == "b")
 * ```
 */
@JvmName("toMutableSortedMapFromIterablePairToDest")
fun <K: Comparable<K>, V> Iterable<Pair<K, V>>.toMutableSortedMap(
    destination: MutableSortedMap<K, V> = SortedMaps.mutable.of(),
): MutableSortedMap<K, V> {
    forEach {
        destination[it.first] = it.second
    }
    return destination
}

/**
 * [Pair] iterable을 [comparator] 기반 sorted map으로 변환합니다.
 *
 * ## 동작/계약
 * - 새 destination 맵을 allocate 한 뒤 값을 누적합니다.
 * - 같은 키가 반복되면 마지막 값으로 덮어써집니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * val out = listOf(1 to "a", 2 to "b").toMutableSortedMap(compareByDescending { it })
 * check(out.firstKey() == 2)
 * check(out[1] == "a")
 * ```
 */
@JvmName("toMutableSortedMapFromIterablePairWithComparator")
fun <K: Comparable<K>, V> Iterable<Pair<K, V>>.toMutableSortedMap(
    comparator: Comparator<K>,
): MutableSortedMap<K, V> {
    val destination: MutableSortedMap<K, V> = SortedMaps.mutable.of(comparator)
    forEach {
        destination[it.first] = it.second
    }
    return destination
}

/**
 * [Pair] sequence를 [destination] sorted map에 누적합니다.
 *
 * ## 동작/계약
 * - sequence를 한 번 소비하며 [destination]을 mutate 합니다.
 * - 같은 키가 반복되면 마지막 값으로 덮어써집니다.
 * - 결과로 [destination] 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val out = sequenceOf(2 to "b", 1 to "a").toMutableSortedMap()
 * check(out.firstKey() == 1)
 * check(out.size == 2)
 * ```
 */
@JvmName("toMutableSortedMapFromSequencePairToDest")
fun <K: Comparable<K>, V> Sequence<Pair<K, V>>.toMutableSortedMap(
    destination: MutableSortedMap<K, V> = SortedMaps.mutable.of(),
): MutableSortedMap<K, V> =
    asIterable().toMutableSortedMap(destination)

/**
 * [Pair] sequence를 [comparator] 기반 sorted map으로 변환합니다.
 *
 * ## 동작/계약
 * - sequence를 한 번 소비해 새 destination 맵에 누적합니다.
 * - 같은 키가 반복되면 마지막 값으로 덮어써집니다.
 * - 결과 맵은 [comparator] 정렬을 따릅니다.
 *
 * ```kotlin
 * val out = sequenceOf(1 to "a", 2 to "b").toMutableSortedMap(compareByDescending { it })
 * check(out.firstKey() == 2)
 * check(out.size == 2)
 * ```
 */
@JvmName("toMutableSortedMapFromSequencePairWithComparator")
fun <K: Comparable<K>, V> Sequence<Pair<K, V>>.toMutableSortedMap(
    comparator: Comparator<K>,
): MutableSortedMap<K, V> =
    asIterable().toMutableSortedMap(comparator)

/**
 * [Pair] iterator를 [destination] sorted map에 누적합니다.
 *
 * ## 동작/계약
 * - iterator를 끝까지 소비하며 [destination]을 mutate 합니다.
 * - 같은 키가 반복되면 마지막 값으로 덮어써집니다.
 * - 결과로 [destination] 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val out = listOf(2 to "b", 1 to "a").iterator().toMutableSortedMap()
 * check(out.firstKey() == 1)
 * check(out.size == 2)
 * ```
 */
@JvmName("toMutableSortedMapFromSequencePairToDest")
fun <K: Comparable<K>, V> Iterator<Pair<K, V>>.toMutableSortedMap(
    destination: MutableSortedMap<K, V> = SortedMaps.mutable.of(),
): MutableSortedMap<K, V> =
    asIterable().toMutableSortedMap(destination)

/**
 * [Pair] iterator를 [comparator] 기반 sorted map으로 변환합니다.
 *
 * ## 동작/계약
 * - iterator를 끝까지 소비해 새 destination 맵에 누적합니다.
 * - 같은 키가 반복되면 마지막 값으로 덮어써집니다.
 * - 결과 맵은 [comparator] 정렬을 따릅니다.
 *
 * ```kotlin
 * val out = listOf(1 to "a", 2 to "b").iterator().toMutableSortedMap(compareByDescending { it })
 * check(out.firstKey() == 2)
 * check(out.size == 2)
 * ```
 */
@JvmName("toMutableSortedMapFromIteratorPairWithComparator")
fun <K: Comparable<K>, V> Iterator<Pair<K, V>>.toMutableSortedMap(
    comparator: Comparator<K>,
): MutableSortedMap<K, V> =
    asIterable().toMutableSortedMap(comparator)

/**
 * [Pair] 배열을 [destination] sorted map에 누적합니다.
 *
 * ## 동작/계약
 * - 수신 배열은 mutate 하지 않고 [destination]만 mutate 합니다.
 * - 같은 키가 반복되면 마지막 값으로 덮어써집니다.
 * - 결과로 [destination] 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val out = arrayOf(2 to "b", 1 to "a").toMutableSortedMap()
 * check(out.firstKey() == 1)
 * check(out.size == 2)
 * ```
 */
@JvmName("toMutableSortedMapFromArrayPairToDest")
fun <K: Comparable<K>, V> Array<Pair<K, V>>.toMutableSortedMap(
    destination: MutableSortedMap<K, V> = SortedMaps.mutable.of(),
): MutableSortedMap<K, V> =
    asIterable().toMutableSortedMap(destination)

/**
 * [Pair] 배열을 [comparator] 기반 sorted map으로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 배열은 mutate 하지 않고 새 destination 맵을 allocate 합니다.
 * - 같은 키가 반복되면 마지막 값으로 덮어써집니다.
 * - 결과 맵은 [comparator] 정렬을 따릅니다.
 *
 * ```kotlin
 * val out = arrayOf(1 to "a", 2 to "b").toMutableSortedMap(compareByDescending { it })
 * check(out.firstKey() == 2)
 * check(out.size == 2)
 * ```
 */
@JvmName("toMutableSortedMapFromArrayPairWithComparator")
fun <K: Comparable<K>, V> Array<Pair<K, V>>.toMutableSortedMap(
    comparator: Comparator<K>,
): MutableSortedMap<K, V> =
    asIterable().toMutableSortedMap(comparator)
