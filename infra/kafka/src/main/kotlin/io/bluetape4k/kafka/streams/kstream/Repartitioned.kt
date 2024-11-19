@file:JvmMultifileClass
@file:JvmName("KStreamKt")

package io.bluetape4k.kafka.streams.kstream

import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.kstream.Repartitioned
import org.apache.kafka.streams.processor.StreamPartitioner

fun <K, V> repartitionedOf(name: String): Repartitioned<K, V> =
    Repartitioned.`as`(name)

fun <K, V> repartitionedOf(keySerde: Serde<K>, valueSerde: Serde<V>): Repartitioned<K, V> =
    Repartitioned.with(keySerde, valueSerde)

fun <K, V> repartitionedOf(partitioner: StreamPartitioner<K, V>): Repartitioned<K, V> =
    Repartitioned.streamPartitioner(partitioner)

fun <K, V> repartitionedOf(numberOfPartitions: Int): Repartitioned<K, V> =
    Repartitioned.numberOfPartitions(numberOfPartitions)
