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

/**
 * Kafka Streams에서 상태 저장소를 구성할 때 사용할 [Materialized] 인스턴스를 생성합니다.
 *
 * 저장소 유형을 지정하여 Materialized 인스턴스를 생성합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val materialized = materializedOf<String, String, KeyValueStore<Bytes, ByteArray>>(
 *     Materialized.StoreType.IN_MEMORY
 * )
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @param S StateStore 하위 타입
 * @param storeType 저장소 유형 (IN_MEMORY, ROCKS_DB 등)
 * @return [Materialized] 인스턴스
 */
fun <K, V, S: StateStore> materializedOf(storeType: StoreType): Materialized<K, V, S> = Materialized.`as`(storeType)

/**
 * Kafka Streams에서 상태 저장소를 구성할 때 사용할 [Materialized] 인스턴스를 생성합니다.
 *
 * 저장소 이름을 지정하여 Materialized 인스턴스를 생성합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val materialized = materializedOf<String, String, KeyValueStore<Bytes, ByteArray>>("my-store")
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @param S StateStore 하위 타입
 * @param storeName 저장소 이름
 * @return [Materialized] 인스턴스
 */
fun <K, V, S: StateStore> materializedOf(storeName: String): Materialized<K, V, S> = Materialized.`as`(storeName)

/**
 * Kafka Streams에서 상태 저장소를 구성할 때 사용할 [Materialized] 인스턴스를 생성합니다.
 *
 * 키와 값의 Serde를 지정하여 Materialized 인스턴스를 생성합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val materialized = materializedOf(
 *     keySerde = Serdes.String(),
 *     valueSerde = Serdes.Long().asSerde()
 * )
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @param S StateStore 하위 타입
 * @param keySerde 키를 직렬화/역직렬화할 Serde
 * @param valueSerde 값을 직렬화/역직렬화할 Serde
 * @return [Materialized] 인스턴스
 */
fun <K, V, S: StateStore> materializedOf(
    keySerde: Serde<K>,
    valueSerde: Serde<V>,
): Materialized<K, V, S> = Materialized.with(keySerde, valueSerde)

/**
 * Kafka Streams에서 윈도우 상태 저장소를 구성할 때 사용할 [Materialized] 인스턴스를 생성합니다.
 *
 * WindowBytesStoreSupplier를 사용하여 윈도우 저장소를 구성합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val windowSupplier = Stores.windowStoreBuilder(
 *     Stores.persistentWindowStore("window-store", Duration.ofHours(1), Duration.ofMinutes(5), false),
 *     Serdes.String(),
 *     Serdes.Long()
 * ).build()
 * val materialized = materializedOf<String, Long>(windowSupplier)
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @param supplier WindowBytesStoreSupplier 인스턴스
 * @return [Materialized] 인스턴스
 */
fun <K, V> materializedOf(supplier: WindowBytesStoreSupplier): Materialized<K, V, WindowStore<Bytes, ByteArray>> =
    Materialized.`as`(supplier)

/**
 * Kafka Streams에서 세션 상태 저장소를 구성할 때 사용할 [Materialized] 인스턴스를 생성합니다.
 *
 * SessionBytesStoreSupplier를 사용하여 세션 저장소를 구성합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val sessionSupplier = Stores.sessionStoreBuilder(
 *     Stores.persistentSessionStore("session-store", Duration.ofMinutes(30)),
 *     Serdes.String(),
 *     Serdes.Long()
 * ).build()
 * val materialized = materializedOf<String, Long>(sessionSupplier)
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @param supplier SessionBytesStoreSupplier 인스턴스
 * @return [Materialized] 인스턴스
 */
fun <K, V> materializedOf(supplier: SessionBytesStoreSupplier): Materialized<K, V, SessionStore<Bytes, ByteArray>> =
    Materialized.`as`(supplier)

/**
 * Kafka Streams에서 키-값 상태 저장소를 구성할 때 사용할 [Materialized] 인스턴스를 생성합니다.
 *
 * KeyValueBytesStoreSupplier를 사용하여 키-값 저장소를 구성합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val kvSupplier = Stores.keyValueStoreBuilder(
 *     Stores.persistentKeyValueStore("kv-store"),
 *     Serdes.String(),
 *     Serdes.Long()
 * ).build()
 * val materialized = materializedOf<String, Long>(kvSupplier)
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @param supplier KeyValueBytesStoreSupplier 인스턴스
 * @return [Materialized] 인스턴스
 */
fun <K, V> materializedOf(supplier: KeyValueBytesStoreSupplier): Materialized<K, V, KeyValueStore<Bytes, ByteArray>> =
    Materialized.`as`(supplier)
