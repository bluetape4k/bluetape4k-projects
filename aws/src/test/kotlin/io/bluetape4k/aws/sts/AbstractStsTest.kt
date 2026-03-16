package io.bluetape4k.aws.sts

import io.bluetape4k.aws.auth.staticCredentialsProviderOf
import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.aws.LocalStackServer
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sts.StsAsyncClient
import software.amazon.awssdk.services.sts.StsClient
import java.net.URI

/**
 * STS 테스트를 위한 추상 기반 클래스.
 *
 * LocalStack을 사용하여 로컬 환경에서 AWS STS API를 테스트합니다.
 * [LocalStackServer]를 통해 STS 서비스 컨테이너를 자동으로 시작하고,
 * [StsClient] 및 [StsAsyncClient]를 생성하여 테스트에서 재사용할 수 있도록 제공합니다.
 */
abstract class AbstractStsTest {

    companion object: KLogging() {
        @JvmStatic
        private val AwsSTS: LocalStackServer by lazy {
            LocalStackServer.Launcher.localStack.withServices("sts")
        }

        @JvmStatic
        protected val endpointOverride: URI by lazy {
            AwsSTS.endpoint
        }

        @JvmStatic
        protected val credentialsProvider: StaticCredentialsProvider by lazy {
            staticCredentialsProviderOf(AwsSTS.accessKey, AwsSTS.secretKey)
        }

        @JvmStatic
        protected val region: Region
            get() = Region.of(AwsSTS.region)

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(minLength: Int = 256, maxLength: Int = 2048): String {
            return Fakers.randomString(minLength, maxLength)
        }
    }

    protected val client: StsClient by lazy {
        StsClientFactory.Sync.create(
            endpointOverride = endpointOverride,
            region = region,
            credentialsProvider = credentialsProvider,
            httpClient = SdkHttpClientProvider.defaultHttpClient
        )
    }

    protected val asyncClient: StsAsyncClient by lazy {
        StsClientFactory.Async.create(
            endpointOverride = endpointOverride,
            region = region,
            credentialsProvider = credentialsProvider,
            httpClient = SdkAsyncHttpClientProvider.defaultHttpClient,
        )
    }
}
