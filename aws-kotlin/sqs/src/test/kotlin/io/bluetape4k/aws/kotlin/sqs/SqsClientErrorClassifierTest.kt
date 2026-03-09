package io.bluetape4k.aws.kotlin.sqs

import aws.sdk.kotlin.services.sqs.model.QueueDoesNotExist
import aws.sdk.kotlin.services.sqs.model.ResourceNotFoundException
import aws.sdk.kotlin.services.sqs.model.SqsException
import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.ServiceErrorMetadata
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

@OptIn(InternalApi::class)
class SqsClientErrorClassifierTest {

    @Test
    fun `isMissingQueueError는 QueueDoesNotExist를 미존재로 판별한다`() {
        QueueDoesNotExist { message = "missing queue" }.isMissingQueueError().shouldBeTrue()
    }

    @Test
    fun `isMissingQueueError는 ResourceNotFoundException을 미존재로 판별한다`() {
        ResourceNotFoundException { message = "missing resource" }.isMissingQueueError().shouldBeTrue()
    }

    @Test
    fun `isMissingQueueError는 HTTP 404를 미존재로 판별한다`() {
        serviceException(statusCode = 404).isMissingQueueError().shouldBeTrue()
    }

    @Test
    fun `isMissingQueueError는 기타 서비스 오류를 미존재로 판별하지 않는다`() {
        serviceException(errorCode = "AccessDenied", statusCode = 403).isMissingQueueError().shouldBeFalse()
    }

    private fun serviceException(
        errorCode: String? = null,
        statusCode: Int? = null,
    ): SqsException {
        val exception = SqsException("test error")
        errorCode?.let { exception.sdkErrorMetadata.attributes[ServiceErrorMetadata.ErrorCode] = it }
        statusCode?.let {
            exception.sdkErrorMetadata.attributes[ServiceErrorMetadata.ProtocolResponse] =
                HttpResponse(status = HttpStatusCode.fromValue(it))
        }
        return exception
    }
}
