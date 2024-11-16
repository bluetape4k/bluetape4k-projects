package io.bluetape4k.collections

import java.util.*

/**
 * [Pair]를 [Map.Entry]로 변환합니다.
 *
 * ```
 * val pair = "key" to "value"
 * val entry = pair.toMapEntry()
 * ```
 */
fun <K, V> Pair<K, V>.toMapEntry(): Map.Entry<K, V> = AbstractMap.SimpleEntry(first, second)

/**
 * [Map.Entry]를 [Pair]로 변환합니다.
 *
 * ```
 * val entry = AbstractMap.SimpleEntry("key", "value")
 * val pair = entry.toPair()
 * ```
 */
fun <K, V> Map.Entry<K, V>.toPair(): Pair<K, V> = this.key to this.value
