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
 * [SendEmailRequest]를 코루틴으로 전송합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [sendAsync]를 호출한 뒤 `await()`로 완료를 기다립니다.
 * - 반환값은 비동기 API의 [SendEmailResponse]와 동일합니다.
 *
 * ```kotlin
 * val response = sesAsyncClient.send(sendEmailRequest)
 * val messageId = response.messageId()
 * // messageId.isNotBlank() == true
 * ```
 */
suspend inline fun SesAsyncClient.send(request: SendEmailRequest): SendEmailResponse =
    sendAsync(request).await()

/**
 * [SendRawEmailRequest]를 코루틴으로 전송합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [sendRawAsync]를 호출한 뒤 `await()`로 완료를 기다립니다.
 * - 반환값은 비동기 API의 [SendRawEmailResponse]와 동일합니다.
 *
 * ```kotlin
 * val response = sesAsyncClient.sendRaw(sendRawEmailRequest)
 * val messageId = response.messageId()
 * // messageId.isNotBlank() == true
 * ```
 */
suspend inline fun SesAsyncClient.sendRaw(request: SendRawEmailRequest): SendRawEmailResponse =
    sendRawAsync(request).await()

/**
 * [SendTemplatedEmailRequest]를 코루틴으로 전송합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [sendTemplatedAsync]를 호출한 뒤 `await()`로 완료를 기다립니다.
 * - 반환값은 비동기 API의 [SendTemplatedEmailResponse]와 동일합니다.
 *
 * ```kotlin
 * val response = sesAsyncClient.sendTemplated(sendTemplatedEmailRequest)
 * val messageId = response.messageId()
 * // messageId.isNotBlank() == true
 * ```
 */
suspend inline fun SesAsyncClient.sendTemplated(request: SendTemplatedEmailRequest): SendTemplatedEmailResponse =
    sendTemplatedAsync(request).await()

/**
 * [SendBulkTemplatedEmailRequest]를 코루틴으로 전송합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [sendBulkTemplatedAsync]를 호출한 뒤 `await()`로 완료를 기다립니다.
 * - 반환값은 비동기 API의 [SendBulkTemplatedEmailResponse]와 동일합니다.
 *
 * ```kotlin
 * val response = sesAsyncClient.sendBulkTemplated(sendBulkTemplatedEmailRequest)
 * val statuses = response.status()
 * // statuses.isNotEmpty() == true
 * ```
 */
suspend inline fun SesAsyncClient.sendBulkTemplated(
    request: SendBulkTemplatedEmailRequest,
): SendBulkTemplatedEmailResponse =
    sendBulkTemplatedAsync(request).await()
