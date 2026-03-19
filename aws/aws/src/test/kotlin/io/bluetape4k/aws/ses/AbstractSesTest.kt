package io.bluetape4k.aws.ses

import io.bluetape4k.aws.AbstractAwsTest
import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.logging.KLogging
import software.amazon.awssdk.services.ses.SesAsyncClient
import software.amazon.awssdk.services.ses.SesClient

abstract class AbstractSesTest: AbstractAwsTest() {

    companion object: KLogging() {
        const val domain = "example.com"
        const val senderEmail = "from-user@example.com"
        const val receiverEamil = "to-use@example.com"
    }

    protected val client: SesClient by lazy {
        SesClientFactory.Sync.create(
            endpointOverride = localStackServer.endpoint,
            region = localStackServer.region(),
            credentialsProvider = localStackServer.credentialsProvider,
            httpClient = SdkHttpClientProvider.defaultHttpClient,
        )
    }

    protected val asyncClient: SesAsyncClient by lazy {
        SesClientFactory.Async.create(
            endpointOverride = localStackServer.endpoint,
            region = localStackServer.region(),
            credentialsProvider = localStackServer.credentialsProvider,
            httpClient = SdkAsyncHttpClientProvider.defaultHttpClient,
        )
    }
}
