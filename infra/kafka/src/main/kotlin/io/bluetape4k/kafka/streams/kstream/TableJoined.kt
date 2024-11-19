@file:JvmMultifileClass
@file:JvmName("KStreamKt")

package io.bluetape4k.kafka.streams.kstream

import org.apache.kafka.streams.kstream.TableJoined
import org.apache.kafka.streams.processor.StreamPartitioner

fun <K, K0> tableJoinedOf(
    partitioner: StreamPartitioner<K, Void>,
    otherPartitioner: StreamPartitioner<K0, Void>,
): TableJoined<K, K0> =
    TableJoined.with(partitioner, otherPartitioner)

fun <K, K0> tableJoinedOf(name: String): TableJoined<K, K0> = TableJoined.`as`(name)
