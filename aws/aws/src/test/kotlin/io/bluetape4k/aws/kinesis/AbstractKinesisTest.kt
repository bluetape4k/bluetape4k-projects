package io.bluetape4k.aws.kinesis

import io.bluetape4k.aws.AbstractAwsTest
import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.aws.LocalStackServer
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisClient

/**
 * Kinesis 테스트를 위한 추상 기반 클래스.
 *
 * LocalStack을 사용하여 로컬 환경에서 AWS Kinesis API를 테스트합니다.
 * [LocalStackServer]를 통해 Kinesis 서비스 컨테이너를 자동으로 시작하고,
 * [KinesisClient] 및 [KinesisAsyncClient]를 생성하여 테스트에서 재사용합니다.
 */
abstract class AbstractKinesisTest: AbstractAwsTest() {

    companion object: KLogging()

    protected val client: KinesisClient by lazy {
        KinesisClientFactory.Sync.create(
            endpointOverride = localStackServer.endpoint,
            region = localStackServer.region(),
            credentialsProvider = localStackServer.credentialsProvider,
            httpClient = SdkHttpClientProvider.defaultHttpClient
        )
    }

    protected val asyncClient: KinesisAsyncClient by lazy {
        KinesisClientFactory.Async.create(
            endpointOverride = localStackServer.endpoint,
            region = localStackServer.region(),
            credentialsProvider = localStackServer.credentialsProvider,
            httpClient = SdkAsyncHttpClientProvider.defaultHttpClient,
        )
    }
}
