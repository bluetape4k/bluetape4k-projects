package io.bluetape4k.aws.kotlin.cloudwatch

import io.bluetape4k.aws.kotlin.AbstractAwsTest
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel

/**
 * AWS Kotlin SDK CloudWatch 테스트를 위한 추상 기반 클래스.
 *
 * LocalStack을 사용하여 로컬 환경에서 AWS CloudWatch / CloudWatch Logs API를 테스트합니다.
 *
 * 클라이언트는 각 테스트에서 [withCloudWatchClient] / [withCloudWatchLogsClient] 패턴으로
 * 직접 생성하고 블록 종료 시 자동 종료합니다.
 *
 * 사용 예시:
 * ```kotlin
 * class MyCloudWatchTest : AbstractKotlinCloudWatchTest() {
 *     @Test
 *     fun `메트릭 게시 테스트`() = runSuspendIO {
 *         withCloudWatchClient(
 *             localStackServer.endpointUrl,
 *             localStackServer.region,
 *             localStackServer.credentialsProvider,
 *         ) { client ->
 *             client.putMetricData("MyNamespace", metricDatum { ... })
 *         }
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
}
