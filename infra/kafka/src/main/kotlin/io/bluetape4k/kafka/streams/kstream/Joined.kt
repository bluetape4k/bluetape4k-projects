@file:JvmMultifileClass
@file:JvmName("KStreamKt")

package io.bluetape4k.kafka.streams.kstream

import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.kstream.Joined

fun <K, V, V0> joinedOf(
    keySerde: Serde<K>,
    valueSerde: Serde<V>,
    otherValueSerde: Serde<V0>,
    name: String? = null,
): Joined<K, V, V0> =
    Joined.with(keySerde, valueSerde, otherValueSerde, name)

fun <K, V, V0> joinedOf(name: String): Joined<K, V, V0> = Joined.`as`(name)
