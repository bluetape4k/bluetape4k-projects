package io.bluetape4k.nats.client

import io.bluetape4k.LibraryName

/**
 * 메시지 헤더에 바이너리 serializer 이름을 기록할 때 사용하는 표준 키입니다.
 *
 * ## 동작/계약
 * - 값은 `"X-$LibraryName-BinarySerializer"` 형식의 불변 상수입니다.
 * - producer/consumer가 동일 키를 공유해 serializer 메타데이터를 교환할 때 사용합니다.
 *
 * ```kotlin
 * val header = NATS_HEADER_BINARY_SERIALIZER
 * // header.startsWith("X-") == true
 * ```
 */
const val NATS_HEADER_BINARY_SERIALIZER = "X-$LibraryName-BinarySerializer"
