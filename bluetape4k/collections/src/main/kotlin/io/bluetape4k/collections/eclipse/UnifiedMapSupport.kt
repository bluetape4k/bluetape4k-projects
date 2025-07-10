package io.bluetape4k.collections.eclipse

import io.bluetape4k.collections.asIterable
import org.eclipse.collections.api.factory.Maps
import org.eclipse.collections.api.map.ImmutableMap
import org.eclipse.collections.impl.map.mutable.UnifiedMap

fun <K, V> emptyUnifiedMap(): ImmutableMap<K, V> = Maps.immutable.empty()

inline fun <K, V> UnifiedMap(size: Int, initializer: (Int) -> Pair<K, V>): UnifiedMap<K, V> =
    UnifiedMap.newMap<K, V>(size).apply {
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

fun <K, V, T: Pair<K, V>> Iterable<T>.toUnifiedMap(): UnifiedMap<K, V> =
    UnifiedMap.newMapWith(this.map { it.toTuplePair() })

fun <K, V, T: Pair<K, V>> Sequence<T>.toUnifiedMap(): UnifiedMap<K, V> = asIterable().toUnifiedMap()
fun <K, V, T: Pair<K, V>> Iterator<T>.toUnifiedMap(): UnifiedMap<K, V> = asIterable().toUnifiedMap()
fun <K, V, T: Pair<K, V>> Array<T>.toUnifiedMap(): UnifiedMap<K, V> = asIterable().toUnifiedMap()

fun <K, V, T: EcPair<K, V>> Iterable<T>.toUnifiedMap(): UnifiedMap<K, V> = UnifiedMap.newMapWith(this)
fun <K, V, T: EcPair<K, V>> Sequence<T>.toUnifiedMap(): UnifiedMap<K, V> = asIterable().toUnifiedMap()
fun <K, V, T: EcPair<K, V>> Iterator<T>.toUnifiedMap(): UnifiedMap<K, V> = asIterable().toUnifiedMap()
fun <K, V, T: EcPair<K, V>> Array<T>.toUnifiedMap(): UnifiedMap<K, V> = asIterable().toUnifiedMap()
