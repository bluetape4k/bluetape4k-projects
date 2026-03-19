package io.bluetape4k.aws.kotlin.ses.model

import aws.sdk.kotlin.services.ses.model.RawMessage

/**
 * 바이트 배열로 SES [RawMessage]를 생성합니다.
 *
 * ```kotlin
 * val raw = rawMessageOf(mimeBytes)
 * ```
 *
 * @param data MIME 형식의 원시 이메일 바이트 배열 (비어 있으면 안 됨)
 * @return [RawMessage] 인스턴스
 */
fun rawMessageOf(data: ByteArray): RawMessage {
    require(data.isNotEmpty()) { "data must not be empty." }

    return RawMessage {
        this.data = data
    }
}
