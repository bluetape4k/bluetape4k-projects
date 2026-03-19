package io.bluetape4k.aws.sns

import io.bluetape4k.aws.AbstractAwsTest
import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.logging.KLogging
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.SnsClient

abstract class AbstractSnsTest: AbstractAwsTest() {

    companion object: KLogging()

    protected val client: SnsClient by lazy {
        SnsClientFactory.Sync.create(
            endpointOverride = localStackServer.endpoint,
            region = localStackServer.region(),
            credentialsProvider = localStackServer.credentialsProvider,
            httpClient = SdkHttpClientProvider.defaultHttpClient
        )
    }

    protected val asyncClient: SnsAsyncClient by lazy {
        SnsClientFactory.Async.create(
            endpointOverride = localStackServer.endpoint,
            region = localStackServer.region(),
            credentialsProvider = localStackServer.credentialsProvider,
            httpClient = SdkAsyncHttpClientProvider.defaultHttpClient,
        )
    }
}
