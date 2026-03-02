package io.bluetape4k.collections.eclipse.multi

import io.bluetape4k.collections.eclipse.toTuplePair
import io.bluetape4k.collections.eclipse.toUnifiedSet
import org.eclipse.collections.api.multimap.Multimap
import org.eclipse.collections.api.multimap.bag.ImmutableBagMultimap
import org.eclipse.collections.api.multimap.bag.MutableBagMultimap
import org.eclipse.collections.api.multimap.list.ImmutableListMultimap
import org.eclipse.collections.api.multimap.list.MutableListMultimap
import org.eclipse.collections.api.multimap.set.ImmutableSetMultimap
import org.eclipse.collections.api.multimap.set.MutableSetMultimap
import org.eclipse.collections.api.multimap.sortedbag.ImmutableSortedBagMultimap
import org.eclipse.collections.api.multimap.sortedset.ImmutableSortedSetMultimap
import org.eclipse.collections.impl.factory.Multimaps
import org.eclipse.collections.impl.multimap.bag.HashBagMultimap
import org.eclipse.collections.impl.multimap.list.FastListMultimap
import org.eclipse.collections.impl.multimap.set.UnifiedSetMultimap

/**
 * 비어 있는 immutable list multimap을 생성합니다.
 *
 * ## 동작/계약
 * - null 입력 없이 항상 빈 immutable 인스턴스를 반환합니다.
 * - mutate 없이 새 객체를 allocate 합니다.
 * - 반환 값은 읽기 전용이며 put/add를 지원하지 않습니다.
 *
 * ```kotlin
 * val mm = emptyImmutableListMultimap<String, Int>()
 * check(mm.isEmpty)
 * check(mm.toMutable().isEmpty)
 * ```
 */
fun <K, V> emptyImmutableListMultimap(): ImmutableListMultimap<K, V> = Multimaps.immutable.list.empty<K, V>()

/**
 * 비어 있는 immutable set multimap을 생성합니다.
 *
 * ## 동작/계약
 * - null 입력 없이 항상 빈 immutable 인스턴스를 반환합니다.
 * - mutate 없이 새 객체를 allocate 합니다.
 * - set semantics를 가지므로 키별 중복 값이 허용되지 않습니다.
 *
 * ```kotlin
 * val mm = emptyImmutableSetMultimap<String, Int>()
 * check(mm.isEmpty)
 * check(mm.toMutable().isEmpty)
 * ```
 */
fun <K, V> emptyImmutableSetMultimap(): ImmutableSetMultimap<K, V> = Multimaps.immutable.set.empty<K, V>()

/**
 * 비어 있는 immutable bag multimap을 생성합니다.
 *
 * ## 동작/계약
 * - null 입력 없이 항상 빈 immutable 인스턴스를 반환합니다.
 * - mutate 없이 새 객체를 allocate 합니다.
 * - bag semantics를 가지므로 키별 중복 값의 개수를 유지합니다.
 *
 * ```kotlin
 * val mm = emptyImmutableBagMultimap<String, Int>()
 * check(mm.isEmpty)
 * check(mm.toMutable().isEmpty)
 * ```
 */
fun <K, V> emptyImmutableBagMultimap(): ImmutableBagMultimap<K, V> = Multimaps.immutable.bag.empty<K, V>()

/**
 * 자연 정렬 기준의 비어 있는 immutable sorted-set multimap을 생성합니다.
 *
 * ## 동작/계약
 * - 값 타입 [V]는 [Comparable] 구현이 필요합니다.
 * - mutate 없이 새 immutable 인스턴스를 allocate 합니다.
 * - 키별 값은 정렬 상태와 set semantics를 유지합니다.
 *
 * ```kotlin
 * val mm = emptyImmutableSortedSetMultimap<String, Int>()
 * check(mm.isEmpty)
 * check(mm.toMutable().isEmpty)
 * ```
 */
fun <K, V: Comparable<V>> emptyImmutableSortedSetMultimap(): ImmutableSortedSetMultimap<K, V> =
    Multimaps.immutable.sortedSet.of<K, V>(Comparator<V>.naturalOrder())

/**
 * 자연 정렬 기준의 비어 있는 immutable sorted-bag multimap을 생성합니다.
 *
 * ## 동작/계약
 * - 값 타입 [V]는 [Comparable] 구현이 필요합니다.
 * - mutate 없이 새 immutable 인스턴스를 allocate 합니다.
 * - 키별 값은 정렬 상태와 bag semantics(중복 카운트)를 유지합니다.
 *
 * ```kotlin
 * val mm = emptyImmutableSortedBagMultimap<String, Int>()
 * check(mm.isEmpty)
 * check(mm.toMutable().isEmpty)
 * ```
 */
fun <K, V: Comparable<V>> emptyImmutableSortedBagMultimap(): ImmutableSortedBagMultimap<K, V> =
    Multimaps.immutable.sortedBag.of<K, V>(Comparator<V>.naturalOrder())

/**
 * 비어 있는 mutable list multimap을 생성합니다.
 *
 * ## 동작/계약
 * - null 입력 없이 항상 빈 mutable 인스턴스를 반환합니다.
 * - mutate 없이 새 객체를 allocate 합니다.
 * - 같은 키에 값 추가 시 삽입 순서를 유지합니다.
 *
 * ```kotlin
 * val mm = emptyMutableListMultimap<String, Int>()
 * mm.put("a", 1)
 * check(mm["a"].contains(1))
 * ```
 */
fun <K, V> emptyMutableListMultimap(): MutableListMultimap<K, V> = Multimaps.mutable.list.empty<K, V>()

/**
 * 비어 있는 mutable set multimap을 생성합니다.
 *
 * ## 동작/계약
 * - null 입력 없이 항상 빈 mutable 인스턴스를 반환합니다.
 * - mutate 없이 새 객체를 allocate 합니다.
 * - 같은 키에서 중복 값은 한 번만 유지됩니다.
 *
 * ```kotlin
 * val mm = emptyMutableSetMultimap<String, Int>()
 * mm.put("a", 1)
 * check(mm["a"].contains(1))
 * ```
 */
fun <K, V> emptyMutableSetMultimap(): MutableSetMultimap<K, V> = Multimaps.mutable.set.empty<K, V>()

/**
 * 비어 있는 mutable bag multimap을 생성합니다.
 *
 * ## 동작/계약
 * - null 입력 없이 항상 빈 mutable 인스턴스를 반환합니다.
 * - mutate 없이 새 객체를 allocate 합니다.
 * - 같은 키에서 동일 값의 중복 카운트를 유지합니다.
 *
 * ```kotlin
 * val mm = emptyMutableBagMultimap<String, Int>()
 * mm.put("a", 1)
 * check(mm["a"].contains(1))
 * ```
 */
fun <K, V> emptyMutableBagMultimap(): MutableBagMultimap<K, V> = Multimaps.mutable.bag.empty<K, V>()


/**
 * [Pair] 배열로 mutable list multimap을 생성합니다.
 *
 * ## 동작/계약
 * - 입력 pair를 순회하며 결과 multimap을 mutate 합니다.
 * - 항상 새 multimap을 allocate 합니다.
 * - 같은 키에 여러 값이 순서대로 누적됩니다.
 *
 * ```kotlin
 * val mm = listMultimapOf("a" to 1, "a" to 2)
 * check(mm["a"].size == 2)
 * check(mm["a"].contains(1))
 * ```
 */
fun <K, V> listMultimapOf(vararg pairs: Pair<K, V>): MutableListMultimap<K, V> =
    FastListMultimap.newMultimap<K, V>().also { multimap ->
        pairs.forEach { (key, value) -> multimap.put(key, value) }
    }

/**
 * [Pair] 배열로 mutable set multimap을 생성합니다.
 *
 * ## 동작/계약
 * - 입력 pair를 순회하며 결과 multimap을 mutate 합니다.
 * - 항상 새 multimap을 allocate 합니다.
 * - 같은 키에서 중복 값은 제거됩니다.
 *
 * ```kotlin
 * val mm = setMultimapOf("a" to 1, "a" to 1)
 * check(mm["a"].size == 1)
 * check(mm["a"].contains(1))
 * ```
 */
fun <K, V> setMultimapOf(vararg pairs: Pair<K, V>): MutableSetMultimap<K, V> =
    UnifiedSetMultimap.newMultimap<K, V>().also { multimap ->
        pairs.forEach { (key, value) -> multimap.put(key, value) }
    }

/**
 * [Pair] 배열로 mutable bag multimap을 생성합니다.
 *
 * ## 동작/계약
 * - 입력 pair를 순회하며 결과 multimap을 mutate 합니다.
 * - 항상 새 multimap을 allocate 합니다.
 * - 같은 키에서 동일 값의 중복 횟수가 유지됩니다.
 *
 * ```kotlin
 * val mm = bagMultimapOf("a" to 1, "a" to 1)
 * check(mm["a"].contains(1))
 * check(mm.sizeDistinct == 1)
 * ```
 */
fun <K, V> bagMultimapOf(vararg pairs: Pair<K, V>): HashBagMultimap<K, V> =
    HashBagMultimap.newMultimap<K, V>().also { multimap ->
        pairs.forEach { (key, value) -> multimap.put(key, value) }
    }

/**
 * [Map]을 immutable list multimap으로 변환합니다.
 *
 * ## 동작/계약
 * - 입력 map은 mutate 하지 않습니다.
 * - 결과는 새 immutable 인스턴스로 allocate 됩니다.
 * - Map 특성상 키당 값은 최대 1개만 반영됩니다.
 *
 * ```kotlin
 * val mm = mapOf("a" to 1).toImmutableListMultimap()
 * check(mm["a"].contains(1))
 * check(mm.isEmpty.not())
 * ```
 */
fun <K, V> Map<K, V>.toImmutableListMultimap(): ImmutableListMultimap<K, V> =
    toListMultimap().toImmutable()

/**
 * [Map]을 mutable list multimap으로 복사합니다.
 *
 * ## 동작/계약
 * - [destination]을 직접 mutate 하고 같은 인스턴스를 반환합니다.
 * - 입력 map은 mutate 하지 않습니다.
 * - Map 특성상 키당 값은 최대 1개만 추가됩니다.
 *
 * ```kotlin
 * val out = mapOf("a" to 1).toListMultimap()
 * check(out["a"].contains(1))
 * check(out.sizeDistinct == 1)
 * ```
 */
fun <K, V> Map<K, V>.toListMultimap(
    destination: MutableListMultimap<K, V> = Multimaps.mutable.list.empty<K, V>(),
): MutableListMultimap<K, V> {
    forEach { (key, value) ->
        destination.put(key, value)
    }
    return destination
}

/**
 * [Map]을 immutable set multimap으로 변환합니다.
 *
 * ## 동작/계약
 * - 입력 map은 mutate 하지 않습니다.
 * - 결과는 새 immutable 인스턴스로 allocate 됩니다.
 * - Map 특성상 키당 값은 최대 1개만 반영됩니다.
 *
 * ```kotlin
 * val mm = mapOf("a" to 1).toImmutableSetMultimap()
 * check(mm["a"].contains(1))
 * check(mm.isEmpty.not())
 * ```
 */
fun <K, V> Map<K, V>.toImmutableSetMultimap(): ImmutableSetMultimap<K, V> =
    toSetMultimap().toImmutable()

/**
 * [Map]을 mutable set multimap으로 복사합니다.
 *
 * ## 동작/계약
 * - [destination]을 직접 mutate 하고 같은 인스턴스를 반환합니다.
 * - 입력 map은 mutate 하지 않습니다.
 * - set semantics를 사용하므로 중복 값은 제거됩니다.
 *
 * ```kotlin
 * val out = mapOf("a" to 1).toSetMultimap()
 * check(out["a"].contains(1))
 * check(out.sizeDistinct == 1)
 * ```
 */
fun <K, V> Map<K, V>.toSetMultimap(
    destination: MutableSetMultimap<K, V> = Multimaps.mutable.set.empty<K, V>(),
): MutableSetMultimap<K, V> {
    forEach { (key, value) ->
        destination.put(key, value)
    }
    return destination
}

/**
 * [Map]을 immutable bag multimap으로 변환합니다.
 *
 * ## 동작/계약
 * - 입력 map은 mutate 하지 않습니다.
 * - 결과는 새 immutable 인스턴스로 allocate 됩니다.
 * - Map 특성상 키당 값은 최대 1개만 반영됩니다.
 *
 * ```kotlin
 * val mm = mapOf("a" to 1).toImmutableBagMultimap()
 * check(mm["a"].contains(1))
 * check(mm.isEmpty.not())
 * ```
 */
fun <K, V> Map<K, V>.toImmutableBagMultimap(): ImmutableBagMultimap<K, V> =
    toBagMultimap().toImmutable()

/**
 * [Map]을 mutable bag multimap으로 복사합니다.
 *
 * ## 동작/계약
 * - [destination]을 직접 mutate 하고 같은 인스턴스를 반환합니다.
 * - 입력 map은 mutate 하지 않습니다.
 * - bag semantics를 사용하므로 값의 중복 카운트를 표현할 수 있습니다.
 *
 * ```kotlin
 * val out = mapOf("a" to 1).toBagMultimap()
 * check(out["a"].contains(1))
 * check(out.sizeDistinct == 1)
 * ```
 */
fun <K, V> Map<K, V>.toBagMultimap(
    destination: MutableBagMultimap<K, V> = Multimaps.mutable.bag.empty<K, V>(),
): MutableBagMultimap<K, V> {
    forEach { (key, value) ->
        destination.put(key, value)
    }
    return destination
}

/**
 * 키와 값 컬렉션 기준으로 multimap 엔트리를 필터링합니다.
 *
 * ## 동작/계약
 * - [predicate]가 true인 키-값 그룹만 유지합니다.
 * - 수신 multimap은 mutate 하지 않고 필터링 결과를 allocate 합니다.
 * - 키별 값 iterable은 뷰(view)일 수 있으므로 외부 mutate에 영향 받을 수 있습니다.
 *
 * ```kotlin
 * val src = listMultimapOf("a" to 1, "b" to 2)
 * val out = src.filter { key, _ -> key == "a" }
 * check(out.sizeDistinct == 1)
 * ```
 */
fun <K, V> Multimap<K, V>.filter(predicate: (K, Iterable<V>) -> Boolean): Multimap<K, V> =
    selectKeysMultiValues { key, values -> predicate(key, values) }

/**
 * multimap의 각 키-값 컬렉션을 [mapper]로 변환해 리스트로 수집합니다.
 *
 * ## 동작/계약
 * - 수신 multimap은 mutate 하지 않고 결과 리스트를 allocate 합니다.
 * - 키 순회 순서는 구현체의 iteration order를 따릅니다.
 * - mapper에서 발생한 예외는 그대로 전파됩니다.
 *
 * ```kotlin
 * val mm = listMultimapOf("a" to 1, "a" to 2)
 * val out = mm.toList { k, v -> "$k:${v.size}" }
 * check(out.contains("a:2"))
 * ```
 *
 * @param mapper 키와 해당 값 컬렉션을 결과 타입으로 변환하는 함수
 */
inline fun <K, V, R> Multimap<K, V>.toList(mapper: (K, Iterable<V>) -> R): List<R> =
    keyMultiValuePairsView().map { mapper(it.one, it.two) }

/**
 * multimap의 각 키-값 컬렉션을 [mapper]로 변환해 집합으로 수집합니다.
 *
 * ## 동작/계약
 * - 수신 multimap은 mutate 하지 않고 결과 set을 allocate 합니다.
 * - mapper 결과의 중복은 set semantics로 제거됩니다.
 * - mapper에서 발생한 예외는 그대로 전파됩니다.
 *
 * ```kotlin
 * val mm = listMultimapOf("a" to 1, "a" to 2, "b" to 3)
 * val out = mm.toSet { _, values -> values.size }
 * check(out.contains(2) && out.contains(1))
 * ```
 *
 * @param mapper 키와 해당 값 컬렉션을 결과 타입으로 변환하는 함수
 */
inline fun <K, V, R> Multimap<K, V>.toSet(mapper: (K, Iterable<V>) -> R): Set<R> =
    keyMultiValuePairsView().map { mapper(it.one, it.two) }.toUnifiedSet()

/**
 * 일반 iterable을 key-value pair로 매핑해 list multimap으로 수집합니다.
 *
 * ## 동작/계약
 * - [destination]을 직접 mutate 하고 같은 인스턴스를 반환합니다.
 * - mapper가 생성한 key가 같으면 값이 순서대로 누적됩니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * val out = listOf("a", "bb").toListMultimap { it.length to it }
 * check(out[1].contains("a"))
 * check(out[2].contains("bb"))
 * ```
 *
 * @param destination 결과를 누적할 대상 multimap
 * @param mapper 입력 요소를 pair로 변환하는 함수
 */
inline fun <T, K, V> Iterable<T>.toListMultimap(
    destination: MutableListMultimap<K, V> = Multimaps.mutable.list.empty(),
    mapper: (T) -> Pair<K, V>,
): MutableListMultimap<K, V> {
    forEach {
        destination.add(mapper(it).toTuplePair())
    }
    return destination
}

/**
 * 일반 iterable을 key-value pair로 매핑해 set multimap으로 수집합니다.
 *
 * ## 동작/계약
 * - [destination]을 직접 mutate 하고 같은 인스턴스를 반환합니다.
 * - 같은 key에서 중복 value는 set semantics로 제거됩니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * val out = listOf("a", "a").toSetMultimap { 1 to it }
 * check(out[1].size == 1)
 * check(out[1].contains("a"))
 * ```
 *
 * @param destination 결과를 누적할 대상 multimap
 * @param mapper 입력 요소를 pair로 변환하는 함수
 */
inline fun <T, K, V> Iterable<T>.toSetMultimap(
    destination: MutableSetMultimap<K, V> = Multimaps.mutable.set.empty(),
    mapper: (T) -> Pair<K, V>,
): MutableSetMultimap<K, V> {
    forEach {
        destination.add(mapper(it).toTuplePair())
    }
    return destination
}

/**
 * 일반 iterable을 key-value pair로 매핑해 bag multimap으로 수집합니다.
 *
 * ## 동작/계약
 * - [destination]을 직접 mutate 하고 같은 인스턴스를 반환합니다.
 * - 같은 key/value가 반복되면 bag semantics로 중복 카운트가 유지됩니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * val out = listOf("a", "a").toBagMultimap { 1 to it }
 * check(out[1].contains("a"))
 * check(out.sizeDistinct == 1)
 * ```
 *
 * @param destination 결과를 누적할 대상 multimap
 * @param mapper 입력 요소를 pair로 변환하는 함수
 */
inline fun <T, K, V> Iterable<T>.toBagMultimap(
    destination: MutableBagMultimap<K, V> = Multimaps.mutable.bag.empty(),
    mapper: (T) -> Pair<K, V>,
): MutableBagMultimap<K, V> {
    forEach {
        destination.add(mapper(it).toTuplePair())
    }
    return destination
}

/**
 * iterable을 [keySelector] 기준으로 grouping 하여 list multimap을 생성합니다.
 *
 * ## 동작/계약
 * - [destination]을 직접 mutate 하고 같은 인스턴스를 반환합니다.
 * - 같은 key 아래에 원본 요소를 순서대로 누적합니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * val out = listOf("a", "bb").groupByListMultimap { it.length }
 * check(out[1].contains("a"))
 * check(out[2].contains("bb"))
 * ```
 *
 * @param destination 결과를 누적할 대상 multimap
 * @param keySelector 각 요소의 그룹 키를 계산하는 함수
 */
inline fun <T, K> Iterable<T>.groupByListMultimap(
    destination: MutableListMultimap<K, T> = Multimaps.mutable.list.empty(),
    keySelector: (T) -> K,
): MutableListMultimap<K, T> {
    return toListMultimap(destination) { keySelector(it) to it }
}

/**
 * iterable을 [keySelector] 기준으로 grouping 하여 set multimap을 생성합니다.
 *
 * ## 동작/계약
 * - [destination]을 직접 mutate 하고 같은 인스턴스를 반환합니다.
 * - 같은 key의 중복 요소는 set semantics로 제거됩니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * val out = listOf("a", "a").groupBySetMultimap { it.length }
 * check(out[1].size == 1)
 * check(out[1].contains("a"))
 * ```
 *
 * @param destination 결과를 누적할 대상 multimap
 * @param keySelector 각 요소의 그룹 키를 계산하는 함수
 */
inline fun <T, K> Iterable<T>.groupBySetMultimap(
    destination: MutableSetMultimap<K, T> = Multimaps.mutable.set.empty(),
    keySelector: (T) -> K,
): MutableSetMultimap<K, T> {
    return toSetMultimap(destination) { keySelector(it) to it }
}

/**
 * iterable을 [keySelector] 기준으로 grouping 하여 bag multimap을 생성합니다.
 *
 * ## 동작/계약
 * - [destination]을 직접 mutate 하고 같은 인스턴스를 반환합니다.
 * - 같은 key/value가 반복되면 bag semantics로 중복 카운트가 유지됩니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * val out = listOf("a", "a").groupByBagMultimap { it.length }
 * check(out[1].contains("a"))
 * check(out.sizeDistinct == 1)
 * ```
 *
 * @param destination 결과를 누적할 대상 multimap
 * @param keySelector 각 요소의 그룹 키를 계산하는 함수
 */
inline fun <T, K> Iterable<T>.groupByBagMultimap(
    destination: MutableBagMultimap<K, T> = Multimaps.mutable.bag.empty(),
    keySelector: (T) -> K,
): MutableBagMultimap<K, T> {
    return toBagMultimap(destination) { keySelector(it) to it }
}

/**
 * [Pair] iterable을 list multimap으로 복사합니다.
 *
 * ## 동작/계약
 * - [destination]을 직접 mutate 하고 같은 인스턴스를 반환합니다.
 * - 같은 key의 값은 삽입 순서대로 누적됩니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * val out = listOf("a" to 1, "a" to 2).toListMultimap()
 * check(out["a"].size == 2)
 * check(out["a"].contains(1))
 * ```
 *
 * @param destination 결과를 누적할 대상 multimap
 */
fun <K, V> Iterable<Pair<K, V>>.toListMultimap(
    destination: MutableListMultimap<K, V> = Multimaps.mutable.list.empty(),
): MutableListMultimap<K, V> {
    return toListMultimap(destination) { it }
}

/**
 * [Pair] iterable을 set multimap으로 복사합니다.
 *
 * ## 동작/계약
 * - [destination]을 직접 mutate 하고 같은 인스턴스를 반환합니다.
 * - 같은 key의 중복 value는 set semantics로 제거됩니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * val out = listOf("a" to 1, "a" to 1).toSetMultimap()
 * check(out["a"].size == 1)
 * check(out["a"].contains(1))
 * ```
 *
 * @param destination 결과를 누적할 대상 multimap
 */
fun <K, V> Iterable<Pair<K, V>>.toSetMultimap(
    destination: MutableSetMultimap<K, V> = Multimaps.mutable.set.empty(),
): MutableSetMultimap<K, V> {
    return toSetMultimap(destination) { it }
}

/**
 * [Pair] iterable을 bag multimap으로 복사합니다.
 *
 * ## 동작/계약
 * - [destination]을 직접 mutate 하고 같은 인스턴스를 반환합니다.
 * - 같은 key/value 중복은 bag semantics로 유지됩니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * val out = listOf("a" to 1, "a" to 1).toBagMultimap()
 * check(out["a"].contains(1))
 * check(out.sizeDistinct == 1)
 * ```
 *
 * @param destination 결과를 누적할 대상 multimap
 */
fun <K, V> Iterable<Pair<K, V>>.toBagMultimap(
    destination: MutableBagMultimap<K, V> = Multimaps.mutable.bag.empty(),
): MutableBagMultimap<K, V> {
    return toBagMultimap(destination) { it }
}
