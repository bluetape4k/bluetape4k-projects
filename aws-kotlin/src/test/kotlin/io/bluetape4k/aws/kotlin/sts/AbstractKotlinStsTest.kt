package io.bluetape4k.aws.kotlin.sts

import aws.sdk.kotlin.services.sts.StsClient
import io.bluetape4k.aws.kotlin.http.HttpClientEngineProvider
import io.bluetape4k.aws.kotlin.tests.endpointUrl
import io.bluetape4k.aws.kotlin.tests.getCredentialsProvider
import io.bluetape4k.aws.kotlin.tests.getLocalStackServer
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.aws.LocalStackServer

/**
 * AWS Kotlin SDK STS 테스트를 위한 추상 기반 클래스.
 *
 * LocalStack을 사용하여 로컬 환경에서 AWS STS API를 테스트합니다.
 * [LocalStackServer]를 통해 STS 서비스 컨테이너를 자동으로 시작하고,
 * [StsClient]를 생성하여 테스트에서 재사용할 수 있도록 제공합니다.
 *
 * 사용 예시:
 * ```kotlin
 * class MyStsTest : AbstractKotlinStsTest() {
 *     @Test
 *     fun `호출자 신원 조회`() = runTest {
 *         val response = client.getCallerIdentity()
 *         // assertions ...
 *     }
 * }
 * ```
 */
abstract class AbstractKotlinStsTest {

    companion object: KLoggingChannel() {
        @JvmStatic
        val stsServer: LocalStackServer by lazy {
            getLocalStackServer("sts")
        }

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(min: Int = 256, max: Int = 2048): String {
            return Fakers.randomString(min, max)
        }
    }

    protected val client: StsClient by lazy {
        stsClientOf(
            stsServer.endpointUrl,
            stsServer.region,
            stsServer.getCredentialsProvider(),
            HttpClientEngineProvider.defaultHttpEngine
        )
    }
}
