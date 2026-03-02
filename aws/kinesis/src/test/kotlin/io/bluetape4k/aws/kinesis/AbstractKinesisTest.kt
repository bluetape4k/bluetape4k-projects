package io.bluetape4k.aws.kinesis

import io.bluetape4k.aws.auth.staticCredentialsProviderOf
import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.aws.LocalStackServer
import org.testcontainers.containers.localstack.LocalStackContainer
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisClient
import java.net.URI

/**
 * Kinesis 테스트를 위한 추상 기반 클래스.
 *
 * LocalStack을 사용하여 로컬 환경에서 AWS Kinesis API를 테스트합니다.
 * [LocalStackServer]를 통해 Kinesis 서비스 컨테이너를 자동으로 시작하고,
 * [KinesisClient] 및 [KinesisAsyncClient]를 생성하여 테스트에서 재사용합니다.
 */
abstract class AbstractKinesisTest {

    companion object: KLogging() {
        @JvmStatic
        private val AwsKinesis: LocalStackContainer by lazy {
            LocalStackServer.Launcher.localStack.withServices(LocalStackContainer.Service.KINESIS)
        }

        @JvmStatic
        protected val endpointOverride: URI by lazy {
            AwsKinesis.getEndpointOverride(LocalStackContainer.Service.KINESIS)
        }

        @JvmStatic
        protected val credentialsProvider: StaticCredentialsProvider by lazy {
            staticCredentialsProviderOf(AwsKinesis.accessKey, AwsKinesis.secretKey)
        }

        @JvmStatic
        protected val region: Region
            get() = Region.of(AwsKinesis.region)

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(minLength: Int = 256, maxLength: Int = 2048): String {
            return Fakers.randomString(minLength, maxLength)
        }
    }

    protected val client: KinesisClient by lazy {
        KinesisClientFactory.Sync.create(
            endpointOverride = endpointOverride,
            region = region,
            credentialsProvider = credentialsProvider,
            httpClient = SdkHttpClientProvider.defaultHttpClient
        )
    }

    protected val asyncClient: KinesisAsyncClient by lazy {
        KinesisClientFactory.Async.create(
            endpointOverride = endpointOverride,
            region = region,
            credentialsProvider = credentialsProvider,
            httpClient = SdkAsyncHttpClientProvider.defaultHttpClient,
        )
    }
}
