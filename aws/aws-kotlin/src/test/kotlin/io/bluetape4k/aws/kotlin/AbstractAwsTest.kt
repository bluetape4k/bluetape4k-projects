package io.bluetape4k.aws.kotlin

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.net.url.Url
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.aws.LocalStackServer
import org.testcontainers.localstack.LocalStackContainer

abstract class AbstractAwsTest {

    companion object: KLoggingChannel() {

        val services = listOf(
            "cloudwatch",
            "logs",
            "dynamodb",
            "kinesis",
            "kms",
            "s3",
            "ses",
            "sns",
            "sqs",
            "sts"
        )

        @JvmStatic
        val localStackServer by lazy {
            LocalStackServer.Launcher.getLocalStack(*services.toTypedArray())
        }

        /**
         * [LocalStackContainer]의 endpoint를 AWS Kotlin SDK의 [Url]로 변환합니다.
         *
         * @return AWS Kotlin SDK [Url] 인스턴스
         */
        val LocalStackContainer.endpointUrl: Url
            get() = Url.parse(this.endpoint.toString())

        /**
         * [LocalStackContainer]를 사용하기 위한 [CredentialsProvider]를 반환합니다.
         *
         * @return [StaticCredentialsProvider] 인스턴스
         */
        val LocalStackContainer.credentialsProvider: StaticCredentialsProvider
            get() =
                StaticCredentialsProvider {
                    accessKeyId = this@credentialsProvider.accessKey
                    secretAccessKey = this@credentialsProvider.secretKey
                }


        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(min: Int = 256, max: Int = 2048): String {
            return Fakers.randomString(min, max)
        }
    }
}
