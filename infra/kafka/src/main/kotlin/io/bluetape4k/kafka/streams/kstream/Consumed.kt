@file:JvmMultifileClass
@file:JvmName("KStreamKt")

package io.bluetape4k.kafka.streams.kstream

import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.processor.TimestampExtractor

fun <K, V> consumedOf(
    keySerde: Serde<K>,
    valueSerde: Serde<V>,
    timestampExtractor: TimestampExtractor? = null,
    resetPolicy: Topology.AutoOffsetReset? = null,
): Consumed<K, V> =
    Consumed.with(keySerde, valueSerde, timestampExtractor, resetPolicy)
