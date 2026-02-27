package io.bluetape4k.aws.kotlin.kms

import aws.sdk.kotlin.services.kms.KmsClient
import io.bluetape4k.aws.kotlin.http.HttpClientEngineProvider
import io.bluetape4k.aws.kotlin.tests.endpointUrl
import io.bluetape4k.aws.kotlin.tests.getCredentialsProvider
import io.bluetape4k.aws.kotlin.tests.getLocalStackServer
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.testcontainers.containers.localstack.LocalStackContainer

abstract class AbstractKmsTest {

    companion object: KLoggingChannel() {
        @JvmStatic
        val kmsServer: LocalStackContainer by lazy {
            getLocalStackServer(LocalStackContainer.Service.KMS)
        }

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(min: Int = 256, max: Int = 2048): String {
            return Fakers.randomString(min, max)
        }
    }

    protected val client: KmsClient by lazy {
        kmsClientOf(
            kmsServer.endpointUrl,
            kmsServer.region,
            kmsServer.getCredentialsProvider(),
            HttpClientEngineProvider.defaultHttpEngine
        )
    }
}
