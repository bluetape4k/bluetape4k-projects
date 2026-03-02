package io.bluetape4k.aws.sts

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldNotBeBlank
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

/**
 * [StsClient] 편의 확장 함수 테스트.
 */
@Execution(ExecutionMode.SAME_THREAD)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class StsClientTest: AbstractStsTest() {

    companion object: KLogging()

    @Test
    @Order(1)
    fun `호출자 신원 조회`() {
        val response = client.getCallerIdentity()

        log.debug { "userId=${response.userId()}, account=${response.account()}, arn=${response.arn()}" }

        response.shouldNotBeNull()
        response.userId().shouldNotBeBlank()
        response.account().shouldNotBeBlank()
        response.arn().shouldNotBeBlank()
    }

    @Test
    @Order(2)
    fun `IAM 역할 임시 맡기 (AssumeRole)`() {
        // LocalStack 환경에서는 임의의 ARN으로도 동작합니다.
        val roleArn = "arn:aws:iam::000000000000:role/TestRole"
        val sessionName = "test-session"

        val response = client.assumeRole(roleArn, sessionName)

        log.debug { "assumeRole credentials=${response.credentials()}" }

        response.shouldNotBeNull()
        response.credentials().shouldNotBeNull()
        response.credentials().accessKeyId().shouldNotBeBlank()
        response.credentials().secretAccessKey().shouldNotBeBlank()
        response.credentials().sessionToken().shouldNotBeBlank()
    }

    @Test
    @Order(3)
    fun `임시 세션 자격 증명 발급 (GetSessionToken)`() {
        val response = client.getSessionToken(durationSeconds = 900)

        log.debug { "sessionToken credentials=${response.credentials()}" }

        response.shouldNotBeNull()
        response.credentials().shouldNotBeNull()
        response.credentials().accessKeyId().shouldNotBeBlank()
        response.credentials().secretAccessKey().shouldNotBeBlank()
        response.credentials().sessionToken().shouldNotBeBlank()
    }
}
