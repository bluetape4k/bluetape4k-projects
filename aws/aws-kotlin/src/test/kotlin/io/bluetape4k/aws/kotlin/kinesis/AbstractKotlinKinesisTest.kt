package io.bluetape4k.aws.kotlin.kinesis

import aws.sdk.kotlin.services.kinesis.KinesisClient
import io.bluetape4k.aws.kotlin.AbstractAwsTest
import io.bluetape4k.aws.kotlin.http.HttpClientEngineProvider
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.aws.LocalStackServer

/**
 * AWS Kotlin SDK Kinesis 테스트를 위한 추상 기반 클래스.
 *
 * LocalStack을 사용하여 로컬 환경에서 AWS Kinesis API를 테스트합니다.
 * [LocalStackServer]를 통해 Kinesis 서비스 컨테이너를 자동으로 시작하고,
 * [KinesisClient]를 생성하여 테스트에서 재사용할 수 있도록 제공합니다.
 *
 * 사용 예시:
 * ```kotlin
 * class MyKinesisTest : AbstractKotlinKinesisTest() {
 *     @Test
 *     fun `스트림 생성 테스트`() = runTest {
 *         val response = client.createStream("test-stream", shardCount = 1)
 *         // assertions ...
 *     }
 * }
 * ```
 */
abstract class AbstractKotlinKinesisTest: AbstractAwsTest() {

    companion object: KLoggingChannel() {
        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(min: Int = 256, max: Int = 2048): String {
            return Fakers.randomString(min, max)
        }
    }

    protected val client: KinesisClient by lazy {
        kinesisClientOf(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
            HttpClientEngineProvider.defaultHttpEngine
        )
    }
}
