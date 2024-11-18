package io.bluetape4k.jackson.uuid

/**
 * UUID 값을 인코딩/디코딩하는 방법을 정의합니다.
 */
enum class JsonUuidEncoderType {
    /**
     * Base62 알고리즘을 이용하여 UUID 값을 인코딩/디코딩합니다.
     */
    BASE62,

    /**
     * UUID 값을 단순히 문자열로 변환합니다.
     */
    PLAIN
}
