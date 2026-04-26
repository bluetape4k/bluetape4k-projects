package io.bluetape4k.aws.kotlin

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.net.url.Url
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.aws.AwsEmulatorServer
import io.bluetape4k.testcontainers.aws.FlociServer
import io.bluetape4k.testcontainers.aws.LocalStackServer

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

        /**
         * AWS 에뮬레이터 서버 인스턴스.
         *
         * `bluetape4k.aws.emulator` 시스템 프로퍼티 값에 따라 선택됩니다:
         * - `"floci"` → [FlociServer]
         * - 그 외 (기본 `"localstack"`) → [LocalStackServer]
         */
        @JvmStatic
        val awsEmulator: AwsEmulatorServer by lazy {
            when (System.getProperty("bluetape4k.aws.emulator", "localstack")) {
                "floci" -> FlociServer.Launcher.floci
                else -> LocalStackServer.Launcher.getLocalStack(*services.toTypedArray())
            }
        }

        @JvmStatic
        @Deprecated("awsEmulator로 대체됩니다.", ReplaceWith("awsEmulator"))
        val localStackServer by lazy {
            LocalStackServer.Launcher.getLocalStack(*services.toTypedArray())
        }

        /**
         * [AwsEmulatorServer]의 endpoint를 AWS Kotlin SDK의 [Url]로 변환합니다.
         *
         * @return AWS Kotlin SDK [Url] 인스턴스
         */
        val AwsEmulatorServer.endpointUrl: Url
            get() = Url.parse(this.awsEndpoint.toString())

        /**
         * [AwsEmulatorServer]를 사용하기 위한 [CredentialsProvider]를 반환합니다.
         *
         * @return [StaticCredentialsProvider] 인스턴스
         */
        val AwsEmulatorServer.credentialsProvider: StaticCredentialsProvider
            get() =
                StaticCredentialsProvider {
                    accessKeyId = this@credentialsProvider.awsAccessKey
                    secretAccessKey = this@credentialsProvider.awsSecretKey
                }


        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(min: Int = 256, max: Int = 2048): String {
            return Fakers.randomString(min, max)
        }
    }
}
