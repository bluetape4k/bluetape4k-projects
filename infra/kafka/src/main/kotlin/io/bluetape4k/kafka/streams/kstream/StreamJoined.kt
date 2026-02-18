@file:JvmMultifileClass
@file:JvmName("KStreamKt")

package io.bluetape4k.kafka.streams.kstream

import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.kstream.StreamJoined
import org.apache.kafka.streams.state.WindowBytesStoreSupplier

/**
 * Kafka Streams에서 스트림-스트림 조인을 수행할 때 사용할 [StreamJoined] 인스턴스를 생성합니다.
 *
 * 양쪽 스트림의 윈도우 저장소를 지정하여 스트림 조인을 구성합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val leftStore = Stores.persistentWindowStore("left-store", Duration.ofMinutes(5), Duration.ofMinutes(1), true)
 * val rightStore = Stores.persistentWindowStore("right-store", Duration.ofMinutes(5), Duration.ofMinutes(1), true)
 * val streamJoined = streamJoinedOf<String, String, String>(leftStore, rightStore)
 * val joinedStream = leftStream.join(rightStream, joiner, streamJoined)
 * ```
 *
 * @param K 키 타입
 * @param V1 왼쪽 스트림의 값 타입
 * @param V2 오른쪽 스트림의 값 타입
 * @param storeSupplier 왼쪽 스트림의 윈도우 저장소 공급자
 * @param otherStoreSupplier 오른쪽 스트림의 윈도우 저장소 공급자
 * @return [StreamJoined] 인스턴스
 */
fun <K, V1, V2> streamJoinedOf(
    storeSupplier: WindowBytesStoreSupplier,
    otherStoreSupplier: WindowBytesStoreSupplier,
): StreamJoined<K, V1, V2> = StreamJoined.with(storeSupplier, otherStoreSupplier)

/**
 * Kafka Streams에서 스트림-스트림 조인을 수행할 때 사용할 [StreamJoined] 인스턴스를 생성합니다.
 *
 * 저장소 이름을 지정하여 스트림 조인을 구성합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val streamJoined = streamJoinedOf<String, String, String>("stream-join-store")
 * val joinedStream = leftStream.join(rightStream, joiner, streamJoined)
 * ```
 *
 * @param K 키 타입
 * @param V1 왼쪽 스트림의 값 타입
 * @param V2 오른쪽 스트림의 값 타입
 * @param storeName 저장소 이름
 * @return [StreamJoined] 인스턴스
 */
fun <K, V1, V2> streamJoinedOf(storeName: String): StreamJoined<K, V1, V2> = StreamJoined.`as`(storeName)

/**
 * Kafka Streams에서 스트림-스트림 조인을 수행할 때 사용할 [StreamJoined] 인스턴스를 생성합니다.
 *
 * 키와 양쪽 스트림의 값에 대한 Serde를 지정하여 스트림 조인을 구성합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val streamJoined = streamJoinedOf(
 *     keySerde = Serdes.String(),
 *     valueSerde = Serdes.String(),
 *     otherValueSerde = Serdes.Long().asSerde()
 * )
 * val joinedStream = leftStream.join(rightStream, joiner, streamJoined)
 * ```
 *
 * @param K 키 타입
 * @param V1 왼쪽 스트림의 값 타입
 * @param V2 오른쪽 스트림의 값 타입
 * @param keySerde 키를 직렬화/역직렬화할 Serde
 * @param valueSerde 왼쪽 스트림 값을 직렬화/역직렬화할 Serde
 * @param otherValueSerde 오른쪽 스트림 값을 직렬화/역직렬화할 Serde
 * @return [StreamJoined] 인스턴스
 */
fun <K, V1, V2> streamJoinedOf(
    keySerde: Serde<K>,
    valueSerde: Serde<V1>,
    otherValueSerde: Serde<V2>,
): StreamJoined<K, V1, V2> = StreamJoined.with(keySerde, valueSerde, otherValueSerde)
