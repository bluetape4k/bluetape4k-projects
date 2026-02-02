package io.bluetape4k.collections.eclipse

import io.bluetape4k.collections.asIterable
import org.eclipse.collections.api.factory.Maps
import org.eclipse.collections.impl.map.mutable.UnifiedMap

fun <K, V> emptyUnifiedMap(): MutableMap<K, V> = Maps.mutable.empty()

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

fun <K, V> unifiedMapOf(vararg pairs: Pair<K, V>): UnifiedMap<K, V> =
    UnifiedMap.newMapWith(pairs.map { it.toTuplePair() })

fun <K, V> unifiedMapOf(size: Int): UnifiedMap<K, V> = UnifiedMap.newMap(size)

fun <K, V> Map<K, V>.toUnifiedMap(): UnifiedMap<K, V> = when (this) {
    is UnifiedMap<K, V> -> this
    else -> UnifiedMap.newMap(this)
}

@JvmName("toUnifiedMapFromIterablePair")
fun <K, V, T: Pair<K, V>> Iterable<T>.toUnifiedMap(destination: UnifiedMap<K, V> = UnifiedMap.newMap()): UnifiedMap<K, V> {
    forEach {
        destination[it.first] = it.second
    }
    return destination
}

@JvmName("toUnifiedMapFromSequencePair")
fun <K, V, T: Pair<K, V>> Sequence<T>.toUnifiedMap(destination: UnifiedMap<K, V> = UnifiedMap.newMap()): UnifiedMap<K, V> =
    asIterable().toUnifiedMap(destination)

@JvmName("toUnifiedMapFromIteratorPair")
fun <K, V, T: Pair<K, V>> Iterator<T>.toUnifiedMap(destination: UnifiedMap<K, V> = UnifiedMap.newMap()): UnifiedMap<K, V> =
    asIterable().toUnifiedMap(destination)

@JvmName("toUnifiedMapFromArrayPair")
fun <K, V, T: Pair<K, V>> Array<T>.toUnifiedMap(destination: UnifiedMap<K, V> = UnifiedMap.newMap()): UnifiedMap<K, V> =
    asIterable().toUnifiedMap(destination)

@JvmName("toUnifiedMapFromIterableEcPair")
fun <K, V, T: EcPair<K, V>> Iterable<T>.toUnifiedMap(destination: UnifiedMap<K, V> = UnifiedMap.newMap()): UnifiedMap<K, V> {
    forEach {
        destination.add(it)
    }
    return destination
}

@JvmName("toUnifiedMapFromSequenceEcPair")
fun <K, V, T: EcPair<K, V>> Sequence<T>.toUnifiedMap(destination: UnifiedMap<K, V> = UnifiedMap.newMap()): UnifiedMap<K, V> =
    asIterable().toUnifiedMap(destination)

@JvmName("toUnifiedMapFromIteratorEcPair")
fun <K, V, T: EcPair<K, V>> Iterator<T>.toUnifiedMap(destination: UnifiedMap<K, V> = UnifiedMap.newMap()): UnifiedMap<K, V> =
    asIterable().toUnifiedMap(destination)

@JvmName("toUnifiedMapFromArrayEcPair")
fun <K, V, T: EcPair<K, V>> Array<T>.toUnifiedMap(destination: UnifiedMap<K, V> = UnifiedMap.newMap()): UnifiedMap<K, V> =
    asIterable().toUnifiedMap(destination)
