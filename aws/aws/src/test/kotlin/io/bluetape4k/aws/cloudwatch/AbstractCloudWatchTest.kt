package io.bluetape4k.aws.cloudwatch

import io.bluetape4k.aws.AbstractAwsTest
import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.logging.KLogging
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClient
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient

/**
 * CloudWatch 테스트를 위한 추상 기반 클래스.
 *
 * LocalStack을 사용하여 로컬 환경에서 AWS CloudWatch API를 테스트합니다.
 */
abstract class AbstractCloudWatchTest: AbstractAwsTest() {

    companion object: KLogging()

    protected val client: CloudWatchClient by lazy {
        CloudWatchClientFactory.Sync.create(
            endpointOverride = localStackServer.endpoint,
            region = localStackServer.region(),
            credentialsProvider = localStackServer.credentialsProvider,
            httpClient = SdkHttpClientProvider.defaultHttpClient
        )
    }

    protected val asyncClient: CloudWatchAsyncClient by lazy {
        CloudWatchClientFactory.Async.create(
            endpointOverride = localStackServer.endpoint,
            region = localStackServer.region(),
            credentialsProvider = localStackServer.credentialsProvider,
            httpClient = SdkAsyncHttpClientProvider.defaultHttpClient
        )
    }

    protected val logsClient: CloudWatchLogsClient by lazy {
        CloudWatchLogsClientFactory.Sync.create(
            endpointOverride = localStackServer.endpoint,
            region = localStackServer.region(),
            credentialsProvider = localStackServer.credentialsProvider,
            httpClient = SdkHttpClientProvider.defaultHttpClient
        )
    }

    protected val logsAsyncClient: CloudWatchLogsAsyncClient by lazy {
        CloudWatchLogsClientFactory.Async.create(
            endpointOverride = localStackServer.endpoint,
            region = localStackServer.region(),
            credentialsProvider = localStackServer.credentialsProvider,
            httpClient = SdkAsyncHttpClientProvider.defaultHttpClient
        )
    }
}
