@file:JvmMultifileClass
@file:JvmName("KStreamKt")

package io.bluetape4k.kafka.streams.kstream

import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.processor.TimestampExtractor

/**
 * Kafka Streams에서 토픽을 소비할 때 사용할 [Consumed] 인스턴스를 생성합니다.
 *
 * [Consumed]는 토픽에서 데이터를 읽어올 때 필요한 Serde와 설정을 정의합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val consumed = consumedOf(
 *     keySerde = Serdes.String(),
 *     valueSerde = Serdes.String(),
 *     resetPolicy = Topology.AutoOffsetReset.EARLIEST
 * )
 * val stream = builder.stream("input-topic", consumed)
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @param keySerde 키를 직렬화/역직렬화할 Serde
 * @param valueSerde 값을 직렬화/역직렬화할 Serde
 * @param timestampExtractor 레코드의 타임스탬프를 추출할 extractor (선택적)
 * @param resetPolicy 오프셋 리셋 정책 (선택적)
 * @return [Consumed] 인스턴스
 */
fun <K, V> consumedOf(
    keySerde: Serde<K>,
    valueSerde: Serde<V>,
    timestampExtractor: TimestampExtractor? = null,
    resetPolicy: Topology.AutoOffsetReset? = null,
): Consumed<K, V> = Consumed.with(keySerde, valueSerde, timestampExtractor, resetPolicy)
