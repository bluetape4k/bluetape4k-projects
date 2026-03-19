package io.bluetape4k.aws.ses

import software.amazon.awssdk.services.ses.SesAsyncClient
import software.amazon.awssdk.services.ses.model.SendBulkTemplatedEmailRequest
import software.amazon.awssdk.services.ses.model.SendBulkTemplatedEmailResponse
import software.amazon.awssdk.services.ses.model.SendEmailRequest
import software.amazon.awssdk.services.ses.model.SendEmailResponse
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse
import software.amazon.awssdk.services.ses.model.SendTemplatedEmailRequest
import software.amazon.awssdk.services.ses.model.SendTemplatedEmailResponse
import java.util.concurrent.CompletableFuture

/**
 * [SendEmailRequest] 정보를 바탕으로 email을 비동기로 전송합니다.
 *
 * ```
 * val response = client.send(request).await()
 * response.messageId().shouldNotBeEmpty()
 * log.debug { "response=$response" }
 * ```
 *
 * @param emailRequest [SendEmailRequest] email 전송 요청 정보
 * @return [CompletableFuture]<[SendEmailResponse]> email 전송 응답 정보
 */
fun SesAsyncClient.sendAsync(
    emailRequest: SendEmailRequest,
): CompletableFuture<SendEmailResponse> =
    sendEmail(emailRequest)

/**
 * [SendRawEmailRequest] 정보를 바탕으로 email을 비동기로 전송합니다.
 *
 * ```
 * val response = client.sendRaw(request).await()
 * response.messageId().shouldNotBeEmpty()
 * log.debug { "response=$response" }
 * ```
 *
 * @param rawEmailRequest [SendRawEmailRequest] email 전송 요청 정보
 * @return [CompletableFuture]<[SendRawEmailResponse]> email 전송 응답 정보
 */
fun SesAsyncClient.sendRawAsync(
    rawEmailRequest: SendRawEmailRequest,
): CompletableFuture<SendRawEmailResponse> =
    sendRawEmail(rawEmailRequest)

/**
 * [SendTemplatedEmailRequest] 정보를 바탕으로 email을 비동기로 전송합니다.
 *
 * ```
 * val response = client.sendTemplated(request).await()
 * response.messageId().shouldNotBeEmpty()
 * log.debug { "response=$response" }
 * ```
 *
 * @param templatedEmailRequest [SendTemplatedEmailRequest] email 전송 요청 정보
 * @return [CompletableFuture]<[SendTemplatedEmailResponse]> email 전송 응답 정보
 */
fun SesAsyncClient.sendTemplatedAsync(
    templatedEmailRequest: SendTemplatedEmailRequest,
): CompletableFuture<SendTemplatedEmailResponse> =
    sendTemplatedEmail(templatedEmailRequest)

/**
 * [SendBulkTemplatedEmailRequest] 정보를 바탕으로 email을 비동기로 전송합니다.
 *
 * ```
 * val response = client.sendBulkTemplated(request).await()
 * response.messageId().shouldNotBeEmpty()
 * log.debug { "response=$response" }
 * ```
 *
 * @param bulkTemplatedEmailRequest [SendBulkTemplatedEmailRequest] email 전송 요청 정보
 * @return [CompletableFuture]<[SendBulkTemplatedEmailResponse]> email 전송 응답 정보
 */
fun SesAsyncClient.sendBulkTemplatedAsync(
    bulkTemplatedEmailRequest: SendBulkTemplatedEmailRequest,
): CompletableFuture<SendBulkTemplatedEmailResponse> =
    sendBulkTemplatedEmail(bulkTemplatedEmailRequest)
