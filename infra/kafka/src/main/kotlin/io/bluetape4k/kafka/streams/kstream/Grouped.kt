@file:JvmMultifileClass
@file:JvmName("KStreamKt")

package io.bluetape4k.kafka.streams.kstream

import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.kstream.Grouped

fun <K, V> groupedOf(processorName: String): Grouped<K, V> = Grouped.`as`(processorName)

fun <K, V> groupedOf(
    keySerde: Serde<K>,
    valueSerde: Serde<V>,
    name: String? = null,
): Grouped<K, V> =
    Grouped.with(name, keySerde, valueSerde)
