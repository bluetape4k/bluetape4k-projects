@file:JvmMultifileClass
@file:JvmName("KStreamKt")

package io.bluetape4k.kafka.streams.kstream

import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.processor.StreamPartitioner

fun <K, V> producedOf(
    keySerde: Serde<K>,
    valueSerde: Serde<V>,
    partitioner: StreamPartitioner<K, V>? = null,
): Produced<K, V> =
    Produced.with(keySerde, valueSerde, partitioner)

fun <K, V> producedOf(processorName: String): Produced<K, V> = Produced.`as`(processorName)
