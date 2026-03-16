package io.bluetape4k.aws.sts

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldNotBeBlank
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

/**
 * [StsAsyncClient] 코루틴 확장 함수 테스트.
 */
@Execution(ExecutionMode.SAME_THREAD)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class StsAsyncClientCoroutinesExtensionsTest: AbstractStsTest() {

    companion object: KLogging()

    @Test
    @Order(1)
    fun `코루틴으로 호출자 신원 조회`() = runTest {
        val response = asyncClient.getCallerIdentityAsync().await()

        log.debug { "userId=${response.userId()}, account=${response.account()}, arn=${response.arn()}" }

        response.shouldNotBeNull()
        response.userId().shouldNotBeBlank()
        response.account().shouldNotBeBlank()
        response.arn().shouldNotBeBlank()
    }

    @Test
    @Order(2)
    fun `코루틴으로 IAM 역할 임시 맡기 (AssumeRole)`() = runTest {
        val roleArn = "arn:aws:iam::000000000000:role/TestRole"
        val sessionName = "coroutine-test-session"

        val response = asyncClient.assumeRole(roleArn, sessionName)

        log.debug { "assumeRole credentials=${response.credentials()}" }

        response.shouldNotBeNull()
        response.credentials().shouldNotBeNull()
        response.credentials().accessKeyId().shouldNotBeBlank()
        response.credentials().secretAccessKey().shouldNotBeBlank()
        response.credentials().sessionToken().shouldNotBeBlank()
    }

    @Test
    @Order(3)
    fun `코루틴으로 임시 세션 자격 증명 발급 (GetSessionToken)`() = runTest {
        val response = asyncClient.getSessionToken(durationSeconds = 900)

        log.debug { "sessionToken credentials=${response.credentials()}" }

        response.shouldNotBeNull()
        response.credentials().shouldNotBeNull()
        response.credentials().accessKeyId().shouldNotBeBlank()
        response.credentials().secretAccessKey().shouldNotBeBlank()
        response.credentials().sessionToken().shouldNotBeBlank()
    }

    @Test
    @Order(4)
    fun `코루틴 AssumeRole durationSeconds 범위 검증`() = runTest {
        assertThrows<IllegalArgumentException> {
            asyncClient.assumeRole(
                roleArn = "arn:aws:iam::000000000000:role/TestRole",
                sessionName = "invalid-coroutine-session",
                durationSeconds = 899,
            )
        }
    }

    @Test
    @Order(5)
    fun `코루틴 GetSessionToken durationSeconds 범위 검증`() = runTest {
        assertThrows<IllegalArgumentException> {
            asyncClient.getSessionToken(durationSeconds = 899)
        }
    }
}
