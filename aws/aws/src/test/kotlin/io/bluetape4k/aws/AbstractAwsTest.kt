package io.bluetape4k.aws

import io.bluetape4k.aws.auth.staticCredentialsProviderOf
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.aws.AwsEmulatorServer
import io.bluetape4k.testcontainers.aws.FlociServer
import io.bluetape4k.testcontainers.aws.LocalStackServer
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region


abstract class AbstractAwsTest {

    companion object: KLogging() {
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
         * 시스템 프로퍼티 `bluetape4k.aws.emulator` 값에 따라 AWS 에뮬레이터를 선택합니다.
         *
         * - `"floci"`: [FlociServer]를 사용합니다.
         * - 그 외(기본값 `"localstack"`): [LocalStackServer]를 사용합니다.
         */
        @JvmStatic
        val awsEmulator: AwsEmulatorServer by lazy {
            val name = System.getProperty("bluetape4k.aws.emulator", "localstack").lowercase()
            when (name) {
                "floci" -> FlociServer.Launcher.floci
                "localstack" -> LocalStackServer.Launcher.getLocalStack(*services.toTypedArray())
                else -> error("Unknown bluetape4k.aws.emulator='$name'. Allowed: localstack, floci")
            }
        }

        @JvmStatic
        @Deprecated("awsEmulator로 대체됩니다.", ReplaceWith("awsEmulator"))
        val localStackServer: LocalStackServer by lazy {
            LocalStackServer.Launcher.getLocalStack(*services.toTypedArray())
        }

        fun AwsEmulatorServer.region(): Region = Region.of(this.regionName)

        /**
         * [AwsEmulatorServer]를 사용하기 위한 [StaticCredentialsProvider]를 반환합니다.
         *
         * @return [StaticCredentialsProvider] 인스턴스
         */
        val AwsEmulatorServer.credentialsProvider
            get() = staticCredentialsProviderOf(this.awsAccessKey, this.awsSecretKey)


        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(): String {
            return Fakers.randomString(256, 2048)
        }
    }
}
