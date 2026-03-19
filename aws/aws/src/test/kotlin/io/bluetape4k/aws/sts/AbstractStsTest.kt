package io.bluetape4k.aws.sts

import io.bluetape4k.aws.AbstractAwsTest
import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.aws.LocalStackServer
import software.amazon.awssdk.services.sts.StsAsyncClient
import software.amazon.awssdk.services.sts.StsClient

/**
 * STS 테스트를 위한 추상 기반 클래스.
 *
 * LocalStack을 사용하여 로컬 환경에서 AWS STS API를 테스트합니다.
 * [LocalStackServer]를 통해 STS 서비스 컨테이너를 자동으로 시작하고,
 * [StsClient] 및 [StsAsyncClient]를 생성하여 테스트에서 재사용할 수 있도록 제공합니다.
 */
abstract class AbstractStsTest: AbstractAwsTest() {

    companion object: KLogging()

    protected val client: StsClient by lazy {
        StsClientFactory.Sync.create(
            endpointOverride = localStackServer.endpoint,
            region = localStackServer.region(),
            credentialsProvider = localStackServer.credentialsProvider,
            httpClient = SdkHttpClientProvider.defaultHttpClient
        )
    }

    protected val asyncClient: StsAsyncClient by lazy {
        StsClientFactory.Async.create(
            endpointOverride = localStackServer.endpoint,
            region = localStackServer.region(),
            credentialsProvider = localStackServer.credentialsProvider,
            httpClient = SdkAsyncHttpClientProvider.defaultHttpClient,
        )
    }
}
