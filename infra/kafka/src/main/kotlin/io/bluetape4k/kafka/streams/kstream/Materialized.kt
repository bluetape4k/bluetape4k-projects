@file:JvmMultifileClass
@file:JvmName("KStreamKt")

package io.bluetape4k.kafka.streams.kstream

import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.kstream.Materialized.StoreType
import org.apache.kafka.streams.processor.StateStore
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.SessionBytesStoreSupplier
import org.apache.kafka.streams.state.SessionStore
import org.apache.kafka.streams.state.WindowBytesStoreSupplier
import org.apache.kafka.streams.state.WindowStore

fun <K, V, S: StateStore> materializedOf(storeType: StoreType): Materialized<K, V, S> =
    Materialized.`as`(storeType)

fun <K, V, S: StateStore> materializedOf(storeName: String): Materialized<K, V, S> =
    Materialized.`as`(storeName)

fun <K, V, S: StateStore> materializedOf(keySerde: Serde<K>, valueSerde: Serde<V>): Materialized<K, V, S> =
    Materialized.with(keySerde, valueSerde)

fun <K, V> materializedOf(
    supplier: WindowBytesStoreSupplier,
): Materialized<K, V, WindowStore<Bytes, ByteArray>> =
    Materialized.`as`(supplier)

fun <K, V> materializedOf(
    supplier: SessionBytesStoreSupplier,
): Materialized<K, V, SessionStore<Bytes, ByteArray>> =
    Materialized.`as`(supplier)

fun <K, V> materializedOf(
    supplier: KeyValueBytesStoreSupplier,
): Materialized<K, V, KeyValueStore<Bytes, ByteArray>> =
    Materialized.`as`(supplier)
