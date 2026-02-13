package io.bluetape4k.aws.sns

import io.bluetape4k.aws.auth.staticCredentialsProviderOf
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.aws.LocalStackServer
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.localstack.LocalStackContainer
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.SnsClient

abstract class AbstractSnsTest {

    companion object: KLogging() {
        @JvmStatic
        private val AwsSNS: LocalStackServer by lazy {
            LocalStackServer.Launcher.localStack.withServices(LocalStackContainer.Service.SNS)
        }

        @JvmStatic
        protected val endpoint by lazy {
            AwsSNS.getEndpointOverride(LocalStackContainer.Service.SNS)
        }

        @JvmStatic
        protected val credentialsProvider: StaticCredentialsProvider by lazy {
            staticCredentialsProviderOf(AwsSNS.accessKey, AwsSNS.secretKey)
        }

        @JvmStatic
        protected val region: Region
            get() = Region.of(AwsSNS.region)

        @JvmStatic
        protected val client: SnsClient by lazy {
            snsClient {
                credentialsProvider(credentialsProvider)
                endpointOverride(endpoint)
                region(region)
            }.apply {
                ShutdownQueue.register(this)
            }
        }

        @JvmStatic
        protected val asyncClient: SnsAsyncClient by lazy {
            snsAsyncClient {
                credentialsProvider(credentialsProvider)
                endpointOverride(endpoint)
                region(region)
            }.apply {
                ShutdownQueue.register(this)
            }
        }

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(minLength: Int = 256, maxLength: Int = 2048): String {
            return Fakers.randomString(minLength, maxLength)
        }
    }
}
