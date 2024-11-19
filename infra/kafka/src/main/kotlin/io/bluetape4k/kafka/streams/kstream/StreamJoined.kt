@file:JvmMultifileClass
@file:JvmName("KStreamKt")

package io.bluetape4k.kafka.streams.kstream

import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.kstream.StreamJoined
import org.apache.kafka.streams.state.WindowBytesStoreSupplier

fun <K, V1, V2> streamJoinedOf(
    storeSupplier: WindowBytesStoreSupplier,
    otherStoreSupplier: WindowBytesStoreSupplier,
): StreamJoined<K, V1, V2> =
    StreamJoined.with(storeSupplier, otherStoreSupplier)

fun <K, V1, V2> streamJoinedOf(storeName: String): StreamJoined<K, V1, V2> =
    StreamJoined.`as`(storeName)

fun <K, V1, V2> streamJoinedOf(
    keySerde: Serde<K>,
    valueSerde: Serde<V1>,
    otherValueSerde: Serde<V2>,
): StreamJoined<K, V1, V2> =
    StreamJoined.with(keySerde, valueSerde, otherValueSerde)
