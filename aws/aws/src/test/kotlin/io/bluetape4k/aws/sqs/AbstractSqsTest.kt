package io.bluetape4k.aws.sqs

import io.bluetape4k.aws.AbstractAwsTest
import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.logging.KLogging
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.SqsClient

abstract class AbstractSqsTest: AbstractAwsTest() {

    companion object: KLogging()

    protected val client: SqsClient by lazy {
        SqsClientFactory.Sync.create(
            endpointOverride = localStackServer.endpoint,
            region = localStackServer.region(),
            credentialsProvider = localStackServer.credentialsProvider,
            httpClient = SdkHttpClientProvider.defaultHttpClient
        )
    }

    protected val asyncClient: SqsAsyncClient by lazy {
        SqsClientFactory.Async.create(
            endpointOverride = localStackServer.endpoint,
            region = localStackServer.region(),
            credentialsProvider = localStackServer.credentialsProvider,
            httpClient = SdkAsyncHttpClientProvider.defaultHttpClient
        )
    }
}
