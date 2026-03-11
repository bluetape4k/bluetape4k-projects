package io.bluetape4k.testcontainers.aws

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.testcontainers.containers.Network
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.time.Duration
import kotlin.test.assertFailsWith

/**
 * LocalStaciServer 실행 시 다음과 같은 예외가 발생하다면,
 * `~/colima/docker.sock` 파일을 삭제한 후 다시 실행해 보세요.
 */
class LocalStackServerTest: AbstractContainerTest() {

    companion object: KLogging()

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { LocalStackServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { LocalStackServer(tag = " ") }
    }

    @Test
    fun `run S3 Service`() {
        LocalStackServer().withServices("S3").use { server ->
            server.start()

            // AWS SDK V2 사용
            val credentialProvider = server.getCredentialProvider()

            val s3Client = S3Client.builder()
                .endpointOverride(server.endpoint)
                .region(Region.of(server.region))
                .credentialsProvider(credentialProvider)
                .build()
                .apply {
                    ShutdownQueue.register(this)
                }

            s3Client.createBucket(CreateBucketRequest.builder().bucket("foo").build())

            val putRequest = PutObjectRequest.builder()
                .bucket("foo")
                .key("bar")
                .build()
            s3Client.putObject(putRequest, RequestBody.fromString("baz"))

            val getRequest = GetObjectRequest.builder()
                .bucket("foo")
                .key("bar")
                .build()

            var content: String? = null
            await atMost Duration.ofSeconds(5) until {
                runCatching {
                    content = s3Client.getObjectAsBytes(getRequest).asUtf8String()
                    content == "baz"
                }.getOrDefault(false)
            }

            content shouldBeEqualTo "baz"
        }
    }

    // NOTE: custom network 사용 시 Ryuk(Testcontainers 리소스 정리 컨테이너)가 Network.newNetwork() 에서
    //       최초 Docker 작업으로 기동될 때 Docker Desktop macOS 레이스 컨디션으로 NotFoundException 이 발생한다.
    //       또한 custom network 사용 후 다른 테스트에서도 예외가 발생하는 부작용이 있어 비활성화한다.
    @Disabled("custom network 은 Ryuk 레이스 컨디션으로 Gradle 테스트 시 실패한다 (Docker Desktop macOS 이슈)")
    @Test
    fun `run multiple services with custom network`() {
        Network.newNetwork().use { network ->
            LocalStackServer()
                .withNetwork(network)
                .withNetworkAliases("notthis", "localstack")
                .use { server ->
                    server.start()
                    server.isRunning.shouldBeTrue()
                }
        }
    }
}
