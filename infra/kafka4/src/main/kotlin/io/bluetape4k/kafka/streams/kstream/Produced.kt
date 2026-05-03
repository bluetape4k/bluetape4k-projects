@file:JvmMultifileClass
@file:JvmName("KStreamKt")

package io.bluetape4k.kafka.streams.kstream

import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.processor.StreamPartitioner

/**
 * Kafka Streams에서 토픽으로 데이터를 생산할 때 사용할 [Produced] 인스턴스를 생성합니다.
 *
 * [Produced]는 KStream의 데이터를 출력 토픽으로 복사할 때 필요한 Serde와
 * 파티셔닝 전략을 정의합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val produced = producedOf(
 *     keySerde = Serdes.String(),
 *     valueSerde = Serdes.String(),
 *     partitioner = CustomPartitioner()
 * )
 * stream.to("output-topic", produced)
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @param keySerde 키를 직렬화할 Serde
 * @param valueSerde 값을 직렬화할 Serde
 * @param partitioner 파티션 할당 전략 (선택적)
 * @return [Produced] 인스턴스
 */
fun <K, V> producedOf(
    keySerde: Serde<K>,
    valueSerde: Serde<V>,
    partitioner: StreamPartitioner<K, V>? = null,
): Produced<K, V> = Produced.with(keySerde, valueSerde, partitioner)

/**
 * Kafka Streams에서 토픽으로 데이터를 생산할 때 사용할 [Produced] 인스턴스를 생성합니다.
 *
 * 프로세서 이름을 지정하여 Topology에서 식별 가능한 이름을 부여합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val produced = producedOf<String, String>("output-processor")
 * stream.to("output-topic", produced)
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @param processorName Topology에서 사용할 프로세서 이름
 * @return [Produced] 인스턴스
 */
fun <K, V> producedOf(processorName: String): Produced<K, V> = Produced.`as`(processorName)
