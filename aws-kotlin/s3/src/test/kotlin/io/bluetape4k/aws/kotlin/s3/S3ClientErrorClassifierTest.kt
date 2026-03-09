package io.bluetape4k.aws.kotlin.s3

import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.ServiceErrorMetadata
import aws.smithy.kotlin.runtime.ServiceException
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

@OptIn(InternalApi::class)
class S3ClientErrorClassifierTest {

    @Test
    fun `isMissingBucketError는 NoSuchBucket과 404를 미존재로 판별한다`() {
        serviceException(errorCode = "NoSuchBucket").isMissingBucketError().shouldBeTrue()
        serviceException(statusCode = 404).isMissingBucketError().shouldBeTrue()
    }

    @Test
    fun `isMissingBucketError는 기타 서비스 오류를 미존재로 판별하지 않는다`() {
        serviceException(errorCode = "AccessDenied", statusCode = 403).isMissingBucketError().shouldBeFalse()
    }

    @Test
    fun `isMissingObjectError는 NoSuchKey와 404를 미존재로 판별한다`() {
        serviceException(errorCode = "NoSuchKey").isMissingObjectError().shouldBeTrue()
        serviceException(statusCode = 404).isMissingObjectError().shouldBeTrue()
    }

    @Test
    fun `isMissingObjectError는 기타 서비스 오류를 미존재로 판별하지 않는다`() {
        serviceException(errorCode = "AccessDenied", statusCode = 403).isMissingObjectError().shouldBeFalse()
    }

    private fun serviceException(
        errorCode: String? = null,
        statusCode: Int? = null,
    ): ServiceException {
        val exception = ServiceException("test error")
        errorCode?.let { exception.sdkErrorMetadata.attributes[ServiceErrorMetadata.ErrorCode] = it }
        statusCode?.let {
            exception.sdkErrorMetadata.attributes[ServiceErrorMetadata.ProtocolResponse] =
                HttpResponse(status = HttpStatusCode.fromValue(it))
        }
        return exception
    }
}
