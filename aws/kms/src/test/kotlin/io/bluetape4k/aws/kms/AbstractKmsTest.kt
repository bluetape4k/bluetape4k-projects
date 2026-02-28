package io.bluetape4k.aws.kms

import io.bluetape4k.aws.auth.staticCredentialsProviderOf
import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.aws.LocalStackServer
import org.testcontainers.containers.localstack.LocalStackContainer
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kms.KmsAsyncClient
import software.amazon.awssdk.services.kms.KmsClient

abstract class AbstractKmsTest {

    companion object: KLogging() {
        @JvmStatic
        private val kmsServer: LocalStackServer by lazy {
            LocalStackServer.Launcher.localStack.withServices(LocalStackContainer.Service.KMS)
        }

        @JvmStatic
        protected val endpoint by lazy {
            kmsServer.getEndpointOverride(LocalStackContainer.Service.KMS)
        }

        @JvmStatic
        protected val credentialsProvider: StaticCredentialsProvider by lazy {
            staticCredentialsProviderOf(kmsServer.accessKey, kmsServer.secretKey)
        }

        @JvmStatic
        protected val region: Region
            get() = Region.of(kmsServer.region)

        @JvmStatic
        protected val client: KmsClient by lazy {
            kmsClient {
                credentialsProvider(credentialsProvider)
                endpointOverride(endpoint)
                region(region)
                httpClient(SdkHttpClientProvider.defaultHttpClient)
            }
        }

        @JvmStatic
        protected val asyncClient: KmsAsyncClient by lazy {
            kmsAsyncClient {
                credentialsProvider(credentialsProvider)
                endpointOverride(endpoint)
                region(region)
                httpClient(SdkAsyncHttpClientProvider.defaultHttpClient)
            }
        }

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(): String {
            return Fakers.randomString(256, 2048)
        }
    }
}
