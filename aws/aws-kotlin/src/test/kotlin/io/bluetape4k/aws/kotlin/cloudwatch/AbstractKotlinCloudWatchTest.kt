package io.bluetape4k.aws.kotlin.cloudwatch

import aws.sdk.kotlin.services.cloudwatch.CloudWatchClient
import aws.sdk.kotlin.services.cloudwatchlogs.CloudWatchLogsClient
import io.bluetape4k.aws.kotlin.AbstractAwsTest
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel

/**
 * AWS Kotlin SDK CloudWatch 테스트를 위한 추상 기반 클래스.
 *
 * LocalStack을 사용하여 로컬 환경에서 AWS CloudWatch / CloudWatch Logs API를 테스트합니다.
 *
 * 사용 예시:
 * ```kotlin
 * class MyCloudWatchTest : AbstractKotlinCloudWatchTest() {
 *     @Test
 *     fun `메트릭 게시 테스트`() = runTest {
 *         val response = cloudWatchClient.putMetricData("MyNamespace", metricDatum { ... })
 *         // assertions ...
 *     }
 * }
 * ```
 */
abstract class AbstractKotlinCloudWatchTest: AbstractAwsTest() {

    companion object: KLoggingChannel() {

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(min: Int = 256, max: Int = 2048): String {
            return Fakers.randomString(min, max)
        }
    }

    protected val cloudWatchClient: CloudWatchClient by lazy {
        cloudWatchClientOf(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        )
    }

    protected val cloudWatchLogsClient: CloudWatchLogsClient by lazy {
        cloudWatchLogsClientOf(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        )
    }
}
