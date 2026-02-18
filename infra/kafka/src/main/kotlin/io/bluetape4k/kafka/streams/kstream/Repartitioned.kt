@file:JvmMultifileClass
@file:JvmName("KStreamKt")

package io.bluetape4k.kafka.streams.kstream

import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.kstream.Repartitioned
import org.apache.kafka.streams.processor.StreamPartitioner

/**
 * Kafka Streams에서 데이터를 리파티셔닝할 때 사용할 [Repartitioned] 인스턴스를 생성합니다.
 *
 * 작업 이름을 지정하여 리파티셔닝 작업을 구성합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val repartitioned = repartitionedOf<String, String>("repartition-step")
 * val repartitionedStream = stream.repartition(repartitioned)
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @param name 리파티셔닝 작업의 이름
 * @return [Repartitioned] 인스턴스
 */
fun <K, V> repartitionedOf(name: String): Repartitioned<K, V> = Repartitioned.`as`(name)

/**
 * Kafka Streams에서 데이터를 리파티셔닝할 때 사용할 [Repartitioned] 인스턴스를 생성합니다.
 *
 * 키와 값의 Serde를 지정하여 리파티셔닝 작업을 구성합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val repartitioned = repartitionedOf(
 *     keySerde = Serdes.String(),
 *     valueSerde = Serdes.Long().asSerde()
 * )
 * val repartitionedStream = stream.repartition(repartitioned)
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @param keySerde 키를 직렬화/역직렬화할 Serde
 * @param valueSerde 값을 직렬화/역직렬화할 Serde
 * @return [Repartitioned] 인스턴스
 */
fun <K, V> repartitionedOf(
    keySerde: Serde<K>,
    valueSerde: Serde<V>,
): Repartitioned<K, V> = Repartitioned.with(keySerde, valueSerde)

/**
 * Kafka Streams에서 데이터를 리파티셔닝할 때 사용할 [Repartitioned] 인스턴스를 생성합니다.
 *
 * 커스텀 파티셔너를 지정하여 리파티셔닝 작업을 구성합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val partitioner = StreamPartitioner<String, String> { topic, key, value, numPartitions ->
 *     key.hashCode() % numPartitions
 * }
 * val repartitioned = repartitionedOf(partitioner)
 * val repartitionedStream = stream.repartition(repartitioned)
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @param partitioner 파티션 할당 전략
 * @return [Repartitioned] 인스턴스
 */
fun <K, V> repartitionedOf(partitioner: StreamPartitioner<K, V>): Repartitioned<K, V> =
    Repartitioned.streamPartitioner(partitioner)

/**
 * Kafka Streams에서 데이터를 리파티셔닝할 때 사용할 [Repartitioned] 인스턴스를 생성합니다.
 *
 * 파티션 수를 지정하여 리파티셔닝 작업을 구성합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val repartitioned = repartitionedOf<String, String>(6)
 * val repartitionedStream = stream.repartition(repartitioned)
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @param numberOfPartitions 파티션 수
 * @return [Repartitioned] 인스턴스
 */
fun <K, V> repartitionedOf(numberOfPartitions: Int): Repartitioned<K, V> =
    Repartitioned.numberOfPartitions(numberOfPartitions)
