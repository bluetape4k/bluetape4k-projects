@file:JvmMultifileClass
@file:JvmName("KStreamKt")

package io.bluetape4k.kafka.streams.kstream

import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.kstream.Joined

/**
 * Kafka Streams에서 두 개의 KStream을 조인할 때 사용할 [Joined] 인스턴스를 생성합니다.
 *
 * [Joined]는 KStream-KStream 조인 작업에서 양쪽 스트림의 Serde와
 * 조인 작업의 이름을 정의합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val joined = joinedOf(
 *     keySerde = Serdes.String(),
 *     valueSerde = Serdes.String(),
 *     otherValueSerde = Serdes.Long().asSerde(),
 *     name = "stream-join"
 * )
 * val joinedStream = leftStream.join(rightStream, joiner, joined)
 * ```
 *
 * @param K 키 타입
 * @param V 왼쪽 스트림의 값 타입
 * @param V0 오른쪽 스트림의 값 타입
 * @param keySerde 키를 직렬화/역직렬화할 Serde
 * @param valueSerde 왼쪽 스트림 값을 직렬화/역직렬화할 Serde
 * @param otherValueSerde 오른쪽 스트림 값을 직렬화/역직렬화할 Serde
 * @param name 조인 작업의 이름 (선택적)
 * @return [Joined] 인스턴스
 */
fun <K, V, V0> joinedOf(
    keySerde: Serde<K>,
    valueSerde: Serde<V>,
    otherValueSerde: Serde<V0>,
    name: String? = null,
): Joined<K, V, V0> = Joined.with(keySerde, valueSerde, otherValueSerde, name)

/**
 * Kafka Streams에서 두 개의 KStream을 조인할 때 사용할 [Joined] 인스턴스를 생성합니다.
 *
 * 조인 작업의 이름만 지정하여 Topology에서 식별 가능한 이름을 부여합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val joined = joinedOf<String, String, Long>("user-order-join")
 * val joinedStream = leftStream.join(rightStream, joiner, joined)
 * ```
 *
 * @param K 키 타입
 * @param V 왼쪽 스트림의 값 타입
 * @param V0 오른쪽 스트림의 값 타입
 * @param name Topology에서 사용할 조인 작업의 이름
 * @return [Joined] 인스턴스
 */
fun <K, V, V0> joinedOf(name: String): Joined<K, V, V0> = Joined.`as`(name)
