package io.bluetape4k.collections.eclipse

import io.bluetape4k.collections.asIterable
import org.eclipse.collections.api.factory.Maps
import org.eclipse.collections.impl.map.mutable.UnifiedMap
import java.util.stream.Stream

/**
 * 비어 있는 mutable 맵을 생성합니다.
 *
 * ## 동작/계약
 * - null 입력은 없으며 항상 non-null 맵을 반환합니다.
 * - 항상 새 맵을 allocate 합니다.
 * - 반환 타입은 [MutableMap] 인터페이스입니다.
 *
 * ```kotlin
 * val map = emptyUnifiedMap<String, Int>()
 * check(map.isEmpty())
 * check(map is MutableMap<String, Int>)
 * ```
 */
fun <K, V> emptyUnifiedMap(): MutableMap<K, V> = Maps.mutable.empty()

/**
 * [size] 만큼 초기화 람다를 호출해 [UnifiedMap]을 생성합니다.
 *
 * ## 동작/계약
 * - [initializer]는 `0 until size` 인덱스로 순서대로 호출됩니다.
 * - 같은 키가 반복되면 마지막 값으로 덮어써집니다.
 * - 항상 새 [UnifiedMap]을 allocate 하며 기존 맵을 mutate 하지 않습니다.
 *
 * ```kotlin
 * val map = unifiedMap(2) { i -> "k$i" to i }
 * check(map["k0"] == 0)
 * check(map["k1"] == 1)
 * ```
 *
 * @param size 초기 생성할 pair 개수
 * @param initializer 인덱스 기반 key/value 생성 함수
 */
inline fun <K, V> unifiedMap(
    size: Int,
    @BuilderInference initializer: (Int) -> Pair<K, V>,
): UnifiedMap<K, V> =
    UnifiedMap.newMap<K, V>(size)
        .apply {
            repeat(size) { index ->
                val pair = initializer(index)
                this[pair.first] = pair.second
            }
        }

/**
 * 가변 인자 [pairs]로 [UnifiedMap]을 생성합니다.
 *
 * ## 동작/계약
 * - 입력이 empty이면 빈 [UnifiedMap]을 반환합니다.
 * - 같은 키가 중복되면 마지막 pair 값이 유지됩니다.
 * - 항상 새 맵을 allocate 하며 입력 배열은 mutate 하지 않습니다.
 *
 * ```kotlin
 * val map = unifiedMapOf("a" to 1, "a" to 2)
 * check(map["a"] == 2)
 * check(map.size == 1)
 * ```
 *
 * @param pairs 맵에 넣을 key/value 쌍
 */
fun <K, V> unifiedMapOf(vararg pairs: Pair<K, V>): UnifiedMap<K, V> {
    if (pairs.isEmpty()) {
        return UnifiedMap.newMap()
    }

    val map = UnifiedMap.newMap<K, V>(pairs.size)
    pairs.forEach { map[it.first] = it.second }
    return map
}

/**
 * 초기 용량 힌트를 사용해 빈 [UnifiedMap]을 생성합니다.
 *
 * ## 동작/계약
 * - [size]는 내부 용량 힌트로 사용되며 실제 크기와 동일하지 않을 수 있습니다.
 * - 항상 새 맵을 allocate 합니다.
 * - 수신 객체를 mutate 하지 않습니다.
 *
 * ```kotlin
 * val map = unifiedMapOf<String, Int>(16)
 * check(map.isEmpty())
 * check(map is UnifiedMap<String, Int>)
 * ```
 *
 * @param size 초기 용량 힌트
 */
fun <K, V> unifiedMapOf(size: Int): UnifiedMap<K, V> = UnifiedMap.newMap(size)

/**
 * 일반 [Map]을 [UnifiedMap]으로 변환합니다.
 *
 * ## 동작/계약
 * - 수신이 이미 [UnifiedMap]이면 allocate 없이 그대로 반환합니다.
 * - 그 외 타입이면 새 [UnifiedMap]을 allocate 하여 내용을 복사합니다.
 * - 수신 맵은 mutate 하지 않습니다.
 *
 * ```kotlin
 * val src = mapOf("a" to 1)
 * val dst = src.toUnifiedMap()
 * check(dst["a"] == 1)
 * ```
 */
fun <K, V> Map<K, V>.toUnifiedMap(): UnifiedMap<K, V> = when (this) {
    is UnifiedMap<K, V> -> this
    else -> UnifiedMap.newMap(this)
}

/**
 * [Pair] iterable을 [UnifiedMap]에 누적합니다.
 *
 * ## 동작/계약
 * - [destination]을 직접 mutate 하며 같은 인스턴스를 반환합니다.
 * - 같은 키가 반복되면 마지막 값으로 덮어써집니다.
 * - 수신 iterable 자체는 mutate 하지 않습니다.
 *
 * ```kotlin
 * val out = listOf("a" to 1, "b" to 2).toUnifiedMap()
 * check(out["a"] == 1)
 * check(out["b"] == 2)
 * ```
 *
 * @param destination 결과를 누적할 대상 맵
 */
@JvmName("toUnifiedMapFromIterablePair")
fun <K, V, T: Pair<K, V>> Iterable<T>.toUnifiedMap(destination: UnifiedMap<K, V> = UnifiedMap.newMap()): UnifiedMap<K, V> {
    forEach {
        destination[it.first] = it.second
    }
    return destination
}

/**
 * [Pair] sequence를 [UnifiedMap]에 누적합니다.
 *
 * ## 동작/계약
 * - sequence를 한 번 소비하며 [destination]을 mutate 합니다.
 * - 같은 키가 반복되면 마지막 값으로 덮어써집니다.
 * - 내부 구현은 iterable 변환 후 위임합니다.
 *
 * ```kotlin
 * val out = sequenceOf("a" to 1, "b" to 2).toUnifiedMap()
 * check(out["a"] == 1)
 * check(out["b"] == 2)
 * ```
 */
@JvmName("toUnifiedMapFromSequencePair")
fun <K, V, T: Pair<K, V>> Sequence<T>.toUnifiedMap(destination: UnifiedMap<K, V> = UnifiedMap.newMap()): UnifiedMap<K, V> =
    asIterable().toUnifiedMap(destination)

/**
 * [Pair] iterator를 [UnifiedMap]에 누적합니다.
 *
 * ## 동작/계약
 * - iterator를 끝까지 소비하며 [destination]을 mutate 합니다.
 * - 같은 키가 반복되면 마지막 값으로 덮어써집니다.
 * - 내부 구현은 iterable 변환 후 위임합니다.
 *
 * ```kotlin
 * val out = listOf("a" to 1).iterator().toUnifiedMap()
 * check(out["a"] == 1)
 * check(out.size == 1)
 * ```
 */
@JvmName("toUnifiedMapFromIteratorPair")
fun <K, V, T: Pair<K, V>> Iterator<T>.toUnifiedMap(destination: UnifiedMap<K, V> = UnifiedMap.newMap()): UnifiedMap<K, V> =
    asIterable().toUnifiedMap(destination)

/**
 * [Pair] 배열을 [UnifiedMap]에 누적합니다.
 *
 * ## 동작/계약
 * - 배열은 mutate 하지 않고 [destination]만 mutate 합니다.
 * - 같은 키가 반복되면 마지막 값으로 덮어써집니다.
 * - 내부 구현은 iterable 변환 후 위임합니다.
 *
 * ```kotlin
 * val out = arrayOf("a" to 1, "a" to 2).toUnifiedMap()
 * check(out["a"] == 2)
 * check(out.size == 1)
 * ```
 */
@JvmName("toUnifiedMapFromArrayPair")
fun <K, V, T: Pair<K, V>> Array<T>.toUnifiedMap(destination: UnifiedMap<K, V> = UnifiedMap.newMap()): UnifiedMap<K, V> =
    asIterable().toUnifiedMap(destination)

/**
 * [Pair] stream을 [UnifiedMap]에 누적합니다.
 *
 * ## 동작/계약
 * - stream을 한 번 소비하며 [destination]을 mutate 합니다.
 * - 같은 키가 반복되면 마지막 값으로 덮어써집니다.
 * - 내부 구현은 iterable 변환 후 위임합니다.
 *
 * ```kotlin
 * val out = Stream.of("a" to 1, "b" to 2).toUnifiedMap()
 * check(out["a"] == 1)
 * check(out["b"] == 2)
 * ```
 */
@JvmName("toUnifiedMapFromStreamPair")
fun <K, V, T: Pair<K, V>> Stream<T>.toUnifiedMap(destination: UnifiedMap<K, V> = UnifiedMap.newMap()): UnifiedMap<K, V> =
    asIterable().toUnifiedMap(destination)

/**
 * Eclipse [EcPair] iterable을 [UnifiedMap]에 누적합니다.
 *
 * ## 동작/계약
 * - [destination]을 직접 mutate 하며 같은 인스턴스를 반환합니다.
 * - 같은 키가 반복되면 마지막 값으로 덮어써질 수 있습니다.
 * - 수신 iterable 자체는 mutate 하지 않습니다.
 *
 * ```kotlin
 * val out = listOf(("a" to 1).toTuplePair()).toUnifiedMap()
 * check(out["a"] == 1)
 * check(out.size == 1)
 * ```
 *
 * @param destination 결과를 누적할 대상 맵
 */
@JvmName("toUnifiedMapFromIterableEcPair")
fun <K, V, T: EcPair<K, V>> Iterable<T>.toUnifiedMap(destination: UnifiedMap<K, V> = UnifiedMap.newMap()): UnifiedMap<K, V> {
    forEach {
        destination.add(it)
    }
    return destination
}

/**
 * Eclipse [EcPair] sequence를 [UnifiedMap]에 누적합니다.
 *
 * ## 동작/계약
 * - sequence를 한 번 소비하며 [destination]을 mutate 합니다.
 * - 같은 키가 반복되면 마지막 값으로 덮어써질 수 있습니다.
 * - 내부 구현은 iterable 변환 후 위임합니다.
 *
 * ```kotlin
 * val out = sequenceOf(("a" to 1).toTuplePair()).toUnifiedMap()
 * check(out["a"] == 1)
 * check(out.size == 1)
 * ```
 */
@JvmName("toUnifiedMapFromSequenceEcPair")
fun <K, V, T: EcPair<K, V>> Sequence<T>.toUnifiedMap(destination: UnifiedMap<K, V> = UnifiedMap.newMap()): UnifiedMap<K, V> =
    asIterable().toUnifiedMap(destination)

/**
 * Eclipse [EcPair] iterator를 [UnifiedMap]에 누적합니다.
 *
 * ## 동작/계약
 * - iterator를 끝까지 소비하며 [destination]을 mutate 합니다.
 * - 같은 키가 반복되면 마지막 값으로 덮어써질 수 있습니다.
 * - 내부 구현은 iterable 변환 후 위임합니다.
 *
 * ```kotlin
 * val out = listOf(("a" to 1).toTuplePair()).iterator().toUnifiedMap()
 * check(out["a"] == 1)
 * check(out.size == 1)
 * ```
 */
@JvmName("toUnifiedMapFromIteratorEcPair")
fun <K, V, T: EcPair<K, V>> Iterator<T>.toUnifiedMap(destination: UnifiedMap<K, V> = UnifiedMap.newMap()): UnifiedMap<K, V> =
    asIterable().toUnifiedMap(destination)

/**
 * Eclipse [EcPair] 배열을 [UnifiedMap]에 누적합니다.
 *
 * ## 동작/계약
 * - 배열은 mutate 하지 않고 [destination]만 mutate 합니다.
 * - 같은 키가 반복되면 마지막 값으로 덮어써질 수 있습니다.
 * - 내부 구현은 iterable 변환 후 위임합니다.
 *
 * ```kotlin
 * val out = arrayOf(("a" to 1).toTuplePair()).toUnifiedMap()
 * check(out["a"] == 1)
 * check(out.size == 1)
 * ```
 */
@JvmName("toUnifiedMapFromArrayEcPair")
fun <K, V, T: EcPair<K, V>> Array<T>.toUnifiedMap(destination: UnifiedMap<K, V> = UnifiedMap.newMap()): UnifiedMap<K, V> =
    asIterable().toUnifiedMap(destination)

/**
 * Eclipse [EcPair] stream을 [UnifiedMap]에 누적합니다.
 *
 * ## 동작/계약
 * - stream을 한 번 소비하며 [destination]을 mutate 합니다.
 * - 같은 키가 반복되면 마지막 값으로 덮어써질 수 있습니다.
 * - 내부 구현은 iterable 변환 후 위임합니다.
 *
 * ```kotlin
 * val out = Stream.of(("a" to 1).toTuplePair()).toUnifiedMap()
 * check(out["a"] == 1)
 * check(out.size == 1)
 * ```
 */
@JvmName("toUnifiedMapFromStreamEcPair")
fun <K, V, T: EcPair<K, V>> Stream<T>.toUnifiedMap(destination: UnifiedMap<K, V> = UnifiedMap.newMap()): UnifiedMap<K, V> =
    asIterable().toUnifiedMap(destination)
