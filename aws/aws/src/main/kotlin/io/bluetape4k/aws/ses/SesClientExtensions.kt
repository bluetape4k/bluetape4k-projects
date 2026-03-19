package io.bluetape4k.aws.ses

import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.model.SendBulkTemplatedEmailRequest
import software.amazon.awssdk.services.ses.model.SendBulkTemplatedEmailResponse
import software.amazon.awssdk.services.ses.model.SendEmailRequest
import software.amazon.awssdk.services.ses.model.SendEmailResponse
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse
import software.amazon.awssdk.services.ses.model.SendTemplatedEmailRequest
import software.amazon.awssdk.services.ses.model.SendTemplatedEmailResponse

/**
 * [SendEmailRequest] 정보를 바탕으로 email을 전송합니다.
 *
 * ```
 * val response = client.send(request)
 * response.messageId().shouldNotBeEmpty()
 * log.debug { "response=$response" }
 * ```
 *
 * @param request [SendEmailRequest] email 전송 요청 정보
 * @return [SendEmailResponse] email 전송 응답 정보
 */
fun SesClient.send(request: SendEmailRequest): SendEmailResponse =
    sendEmail(request)

/**
 * [SendRawEmailRequest] 정보를 바탕으로 email을 전송합니다.
 *
 * ```
 * val response = client.sendRaw(rawEmailRequest)
 * log.debug { "response=$response" }
 * ```
 *
 * @param request [SendRawEmailRequest] email 전송 요청 정보
 * @return [SendRawEmailResponse] email 전송 응답 정보
 */
fun SesClient.sendRaw(request: SendRawEmailRequest): SendRawEmailResponse =
    sendRawEmail(request)

/**
 * [SendTemplatedEmailRequest] 정보를 바탕으로 email을 전송합니다.
 *
 * ```
 * val response = client.sendTemplated(templatedEmailRequest)
 * log.debug { "response=$response" }
 * ```
 *
 * @param request [SendTemplatedEmailRequest] email 전송 요청 정보
 * @return [SendTemplatedEmailResponse] email 전송 응답 정보
 */
fun SesClient.sendTemplated(request: SendTemplatedEmailRequest): SendTemplatedEmailResponse =
    sendTemplatedEmail(request)

/**
 * [SendBulkTemplatedEmailRequest] 정보를 바탕으로 email을 전송합니다.
 *
 * ```
 * val response = client.sendBulkTemplated(bulkTemplatedEmailRequest)
 * log.debug { "response=$response" }
 * ```
 *
 * @param request [SendBulkTemplatedEmailRequest] email 전송 요청 정보
 * @return [SendBulkTemplatedEmailResponse] email 전송 응답 정보
 */
fun SesClient.sendBulkTemplated(request: SendBulkTemplatedEmailRequest): SendBulkTemplatedEmailResponse =
    sendBulkTemplatedEmail(request)
