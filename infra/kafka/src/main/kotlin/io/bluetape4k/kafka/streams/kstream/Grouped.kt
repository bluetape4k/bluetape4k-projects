@file:JvmMultifileClass
@file:JvmName("KStreamKt")

package io.bluetape4k.kafka.streams.kstream

import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.kstream.Grouped

/**
 * Kafka Streams에서 그룹화 작업을 수행할 때 사용할 [Grouped] 인스턴스를 생성합니다.
 *
 * 프로세서 이름을 지정하여 Topology에서 식별 가능한 이름을 부여합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val grouped = groupedOf<String, String>("group-by-key")
 * val groupedStream = stream.groupByKey(grouped)
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @param processorName Topology에서 사용할 프로세서 이름
 * @return [Grouped] 인스턴스
 */
fun <K, V> groupedOf(processorName: String): Grouped<K, V> = Grouped.`as`(processorName)

/**
 * Kafka Streams에서 그룹화 작업을 수행할 때 사용할 [Grouped] 인스턴스를 생성합니다.
 *
 * [Grouped]는 KStream을 그룹화하거나 KTable을 그룹화할 때 필요한 Serde와
 * 작업 이름을 정의합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val grouped = groupedOf(
 *     keySerde = Serdes.String(),
 *     valueSerde = Serdes.Long().asSerde(),
 *     name = "group-by-user"
 * )
 * val groupedStream = stream.groupByKey(grouped)
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @param keySerde 키를 직렬화/역직렬화할 Serde
 * @param valueSerde 값을 직렬화/역직렬화할 Serde
 * @param name 그룹화 작업의 이름 (선택적)
 * @return [Grouped] 인스턴스
 */
fun <K, V> groupedOf(
    keySerde: Serde<K>,
    valueSerde: Serde<V>,
    name: String? = null,
): Grouped<K, V> = Grouped.with(name, keySerde, valueSerde)
