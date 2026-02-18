package io.bluetape4k.kafka.streams

import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.streams.StreamsConfig

/**
 * Kafka Streams 설정을 정의하는 [ConfigDef] 인스턴스입니다.
 *
 * 이 설정 정의는 Kafka Streams 애플리케이션에서 사용 가능한 모든 설정 옵션을 포함합니다.
 * StreamsConfig를 통해 사용 가능한 설정 항목들을 프로그래밍적으로 확인할 때 유용합니다.
 *
 * 사용 예시:
 * ```kotlin
 * // 사용 가능한 모든 설정 옵션 출력
 * streamsConfigDef.configKeys().forEach { (name, key) ->
 *     println("$name: ${key.documentation}")
 * }
 * ```
 *
 * @see StreamsConfig
 */
val streamsConfigDef: ConfigDef = StreamsConfig.configDef()
