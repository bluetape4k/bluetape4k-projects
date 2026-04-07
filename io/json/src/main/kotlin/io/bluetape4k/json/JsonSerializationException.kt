package io.bluetape4k.json

import java.io.Serializable

/**
 * JSON 직렬화/역직렬화 실패를 나타내는 런타임 예외입니다.
 *
 * ## 동작/계약
 * - checked 예외가 아닌 [RuntimeException] 계열로 전파됩니다.
 * - 메시지/원인(cause) 생성자 조합을 통해 실패 원인을 전달할 수 있습니다.
 * - 직렬화/역직렬화 공통 실패를 표현하는 베이스 예외로 사용됩니다.
 * - [Serializable]을 구현하여 분산 환경에서 안전하게 전달될 수 있습니다.
 *
 * ```kotlin
 * val ex = JsonSerializationException("deserialize failed")
 * // ex.message == "deserialize failed"
 * ```
 */
open class JsonSerializationException:
    RuntimeException,
    Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }

    constructor(): super()
    constructor(message: String): super(message)
    constructor(message: String, cause: Throwable?): super(message, cause)
    constructor(cause: Throwable?): super(cause)
}
