package io.bluetape4k.aws.ses

import kotlinx.coroutines.future.await
import software.amazon.awssdk.services.ses.SesAsyncClient
import software.amazon.awssdk.services.ses.model.SendBulkTemplatedEmailRequest
import software.amazon.awssdk.services.ses.model.SendBulkTemplatedEmailResponse
import software.amazon.awssdk.services.ses.model.SendEmailRequest
import software.amazon.awssdk.services.ses.model.SendEmailResponse
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse
import software.amazon.awssdk.services.ses.model.SendTemplatedEmailRequest
import software.amazon.awssdk.services.ses.model.SendTemplatedEmailResponse

/**
 * [SendEmailRequest] 정보를 바탕으로 email을 코루틴 방식으로 전송합니다.
 */
suspend inline fun SesAsyncClient.send(request: SendEmailRequest): SendEmailResponse =
    sendAsync(request).await()

/**
 * [SendRawEmailRequest] 정보를 바탕으로 email을 코루틴 방식으로 전송합니다.
 */
suspend inline fun SesAsyncClient.sendRaw(request: SendRawEmailRequest): SendRawEmailResponse =
    sendRawAsync(request).await()

/**
 * [SendTemplatedEmailRequest] 정보를 바탕으로 email을 코루틴 방식으로 전송합니다.
 */
suspend inline fun SesAsyncClient.sendTemplated(request: SendTemplatedEmailRequest): SendTemplatedEmailResponse =
    sendTemplatedAsync(request).await()

/**
 * [SendBulkTemplatedEmailRequest] 정보를 바탕으로 email을 코루틴 방식으로 전송합니다.
 */
suspend inline fun SesAsyncClient.sendBulkTemplated(
    request: SendBulkTemplatedEmailRequest,
): SendBulkTemplatedEmailResponse =
    sendBulkTemplatedAsync(request).await()
