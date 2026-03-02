package io.bluetape4k.jackson3.uuid

/**
 * UUID JSON 표현 방식을 정의합니다.
 *
 * ## 동작/계약
 * - [JsonUuidEncoder]와 함께 사용되어 UUID 직렬화/역직렬화 전략을 선택합니다.
 *
 * ```kotlin
 * val type = JsonUuidEncoderType.BASE62
 * // type.name == "BASE62"
 * ```
 */
enum class JsonUuidEncoderType {
    /**
     * UUID를 Base62 문자열로 직렬화하고 Base62 입력을 UUID로 복원합니다.
     */
    BASE62,

    /**
     * UUID를 표준 문자열 형식으로 직렬화/역직렬화합니다.
     */
    PLAIN
}
