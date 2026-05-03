package io.bluetape4k.kafka.streams

import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.streams.StreamsConfig

/**
 * Kafka Streams 기본 설정 정의([ConfigDef])를 제공합니다.
 *
 * ## 동작/계약
 * - `StreamsConfig.configDef()` 결과를 모듈 전역 상수로 노출합니다.
 * - 설정 키/문서/타입 메타데이터 조회에 사용할 수 있습니다.
 * - 실행 시점에 값이 변경되지 않는 불변 참조입니다.
 *
 * ```kotlin
 * val hasAppId = streamsConfigDef.configKeys().containsKey("application.id")
 * // hasAppId == true
 * ```
 *
 * @see StreamsConfig
 */
val streamsConfigDef: ConfigDef = StreamsConfig.configDef()
