package io.bluetape4k.collections.eclipse

import org.eclipse.collections.impl.tuple.Tuples

typealias EcPair<K, V> = org.eclipse.collections.api.tuple.Pair<K, V>

fun <K, V> Pair<K, V>.toTuplePair(): EcPair<K, V> =
    Tuples.pair(first, second)

fun <K, V> EcPair<K, V>.toPair(): Pair<K, V> = one to two
