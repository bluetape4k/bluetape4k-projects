package io.bluetape4k.exposed.core.jackson

import io.bluetape4k.jackson.JacksonSerializer

/**
 * Exposed Jackson 확장에서 기본으로 사용하는 [JacksonSerializer] 인스턴스입니다.
 *
 * ## 동작/계약
 * - `lazy` 초기화로 첫 접근 시점에 한 번만 생성됩니다.
 * - 생성된 인스턴스는 프로세스 내에서 재사용됩니다.
 * - 초기화 과정에서 mapper 구성 오류가 발생하면 접근 시 예외가 전파됩니다.
 *
 * ```kotlin
 * val serializer = DefaultJacksonSerializer
 * val same = serializer === DefaultJacksonSerializer
 * // same == true
 * ```
 */
val DefaultJacksonSerializer: JacksonSerializer by lazy { JacksonSerializer() }
