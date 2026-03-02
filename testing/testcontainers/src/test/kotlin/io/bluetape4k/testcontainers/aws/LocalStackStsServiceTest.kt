package io.bluetape4k.testcontainers.aws

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldNotBeBlank
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.testcontainers.containers.localstack.LocalStackContainer
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sts.StsClient
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest

/**
 * LocalStack을 사용한 AWS STS 서비스 예제 테스트.
 *
 * 각 테스트는 독립적인 [LocalStackServer]를 사용하여 격리된 환경에서 실행됩니다.
 */
class LocalStackStsServiceTest: AbstractContainerTest() {

    companion object: KLogging()

    private fun buildStsClient(server: LocalStackServer): StsClient =
        StsClient.builder()
            .endpointOverride(server.getEndpointOverride(LocalStackContainer.Service.STS))
            .region(Region.of(server.region))
            .credentialsProvider(server.getCredentialProvider())
            .build()
            .apply { ShutdownQueue.register(this) }

    @Test
    fun `STS GetCallerIdentity로 호출자 신원 정보 조회`() {
        LocalStackServer().withServices(LocalStackContainer.Service.STS).use { server ->
            server.start()
            val stsClient = buildStsClient(server)

            val identity = stsClient.getCallerIdentity(GetCallerIdentityRequest.builder().build())
            identity.account().shouldNotBeBlank()
            identity.userId().shouldNotBeBlank()
            identity.arn().shouldNotBeBlank()
        }
    }

    @Test
    fun `STS AssumeRole로 임시 자격 증명 획득`() {
        LocalStackServer().withServices(LocalStackContainer.Service.STS).use { server ->
            server.start()
            val stsClient = buildStsClient(server)

            // 현재 계정 ID 조회
            val accountId = stsClient.getCallerIdentity(
                GetCallerIdentityRequest.builder().build()
            ).account()

            // AssumeRole로 임시 자격 증명 획득
            val credentials = stsClient.assumeRole(
                AssumeRoleRequest.builder()
                    .roleArn("arn:aws:iam::$accountId:role/test-execution-role")
                    .roleSessionName("bluetape4k-test-session")
                    .durationSeconds(3600)
                    .build()
            ).credentials()

            credentials.shouldNotBeNull()
            credentials.accessKeyId().shouldNotBeBlank()
            credentials.secretAccessKey().shouldNotBeBlank()
            credentials.sessionToken().shouldNotBeBlank()
            credentials.expiration().shouldNotBeNull()
        }
    }
}
