package io.bluetape4k.aws.cloudwatch

import io.bluetape4k.aws.auth.staticCredentialsProviderOf
import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.aws.LocalStackServer
import org.testcontainers.containers.localstack.LocalStackContainer
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClient
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient
import java.net.URI

/**
 * CloudWatch 테스트를 위한 추상 기반 클래스.
 *
 * LocalStack을 사용하여 로컬 환경에서 AWS CloudWatch API를 테스트합니다.
 */
abstract class AbstractCloudWatchTest {

    companion object: KLogging() {
        @JvmStatic
        private val AwsCloudWatch: LocalStackContainer by lazy {
            LocalStackServer.Launcher.localStack
                .withServices(LocalStackContainer.Service.CLOUDWATCH, LocalStackContainer.Service.CLOUDWATCHLOGS)
        }

        @JvmStatic
        protected val cloudWatchEndpoint: URI by lazy {
            AwsCloudWatch.getEndpointOverride(LocalStackContainer.Service.CLOUDWATCH)
        }

        @JvmStatic
        protected val cloudWatchLogsEndpoint: URI by lazy {
            AwsCloudWatch.getEndpointOverride(LocalStackContainer.Service.CLOUDWATCHLOGS)
        }

        @JvmStatic
        protected val credentialsProvider: StaticCredentialsProvider by lazy {
            staticCredentialsProviderOf(AwsCloudWatch.accessKey, AwsCloudWatch.secretKey)
        }

        @JvmStatic
        protected val region: Region
            get() = Region.of(AwsCloudWatch.region)

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(minLength: Int = 256, maxLength: Int = 2048): String {
            return Fakers.randomString(minLength, maxLength)
        }
    }

    protected val client: CloudWatchClient by lazy {
        CloudWatchClientFactory.Sync.create(
            endpointOverride = cloudWatchEndpoint,
            region = region,
            credentialsProvider = credentialsProvider,
            httpClient = SdkHttpClientProvider.defaultHttpClient
        )
    }

    protected val asyncClient: CloudWatchAsyncClient by lazy {
        CloudWatchClientFactory.Async.create(
            endpointOverride = cloudWatchEndpoint,
            region = region,
            credentialsProvider = credentialsProvider,
            httpClient = SdkAsyncHttpClientProvider.defaultHttpClient
        )
    }

    protected val logsClient: CloudWatchLogsClient by lazy {
        CloudWatchLogsClientFactory.Sync.create(
            endpointOverride = cloudWatchLogsEndpoint,
            region = region,
            credentialsProvider = credentialsProvider,
            httpClient = SdkHttpClientProvider.defaultHttpClient
        )
    }

    protected val logsAsyncClient: CloudWatchLogsAsyncClient by lazy {
        CloudWatchLogsClientFactory.Async.create(
            endpointOverride = cloudWatchLogsEndpoint,
            region = region,
            credentialsProvider = credentialsProvider,
            httpClient = SdkAsyncHttpClientProvider.defaultHttpClient
        )
    }
}
