package io.bluetape4k.aws.sqs

import io.bluetape4k.aws.auth.staticCredentialsProviderOf
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.aws.LocalStackServer
import org.testcontainers.containers.localstack.LocalStackContainer
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.SqsClient

abstract class AbstractSqsTest {

    companion object: KLogging() {
        @JvmStatic
        private val AwsSQS: LocalStackServer by lazy {
            LocalStackServer.Launcher.localStack.withServices(LocalStackContainer.Service.SQS)
        }

        @JvmStatic
        protected val endpoint by lazy {
            AwsSQS.getEndpointOverride(LocalStackContainer.Service.SQS)
        }

        @JvmStatic
        protected val credentialsProvider: StaticCredentialsProvider by lazy {
            staticCredentialsProviderOf(AwsSQS.accessKey, AwsSQS.secretKey)
        }

        @JvmStatic
        protected val region: Region
            get() = Region.of(AwsSQS.region)

        @JvmStatic
        protected val client: SqsClient by lazy {
            SqsFactory.Sync.create(
                endpoint,
                region,
                credentialsProvider
            )
        }

        @JvmStatic
        protected val asyncClient: SqsAsyncClient by lazy {
            SqsFactory.Async.create(
                endpoint,
                region,
                credentialsProvider
            )
        }

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(): String {
            return Fakers.randomString(256, 2048)
        }
    }
}
