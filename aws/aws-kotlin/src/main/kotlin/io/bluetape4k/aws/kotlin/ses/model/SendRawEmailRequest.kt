package io.bluetape4k.aws.kotlin.ses.model

import aws.sdk.kotlin.services.ses.model.RawMessage
import aws.sdk.kotlin.services.ses.model.SendRawEmailRequest

/**
 * [RawMessage]로 [SendRawEmailRequest]를 생성합니다.
 *
 * ```kotlin
 * val request = sendRawEmailRequestOf(rawMessageOf(mimeBytes))
 * ```
 *
 * @param rawMessage 전송할 [RawMessage]
 * @return [SendRawEmailRequest] 인스턴스
 */
inline fun sendRawEmailRequestOf(
    rawMessage: RawMessage,
    crossinline builder: SendRawEmailRequest.Builder.() -> Unit = {},
): SendRawEmailRequest =
    SendRawEmailRequest {
        this.rawMessage = rawMessage
        builder()
    }
