package io.bluetape4k.aws.kotlin.kms

import aws.sdk.kotlin.services.kms.KmsClient
import io.bluetape4k.aws.kotlin.http.HttpClientEngineProvider
import io.bluetape4k.aws.kotlin.tests.endpointUrl
import io.bluetape4k.aws.kotlin.tests.getCredentialsProvider
import io.bluetape4k.aws.kotlin.tests.getLocalStackServer
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.testcontainers.containers.localstack.LocalStackContainer

/**
 * AWS Kotlin SDK KMS 테스트를 위한 추상 기반 클래스.
 *
 * LocalStack을 사용하여 로컬 환경에서 AWS KMS API를 테스트합니다.
 * [LocalStackContainer]를 통해 KMS 서비스 컨테이너를 자동으로 시작하고,
 * [KmsClient]를 생성하여 테스트에서 재사용할 수 있도록 제공합니다.
 *
 * 사용 예시:
 * ```kotlin
 * class MyKmsTest : AbstractKmsTest() {
 *     @Test
 *     fun `키 생성 테스트`() = runTest {
 *         val response = client.createKey {
 *             description = "테스트 키"
 *         }
 *         // assertions ...
 *     }
 * }
 * ```
 */
abstract class AbstractKmsTest {

    companion object: KLoggingChannel() {
        @JvmStatic
        val kmsServer: LocalStackContainer by lazy {
            getLocalStackServer(LocalStackContainer.Service.KMS)
        }

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(min: Int = 256, max: Int = 2048): String {
            return Fakers.randomString(min, max)
        }
    }

    protected val client: KmsClient by lazy {
        kmsClientOf(
            kmsServer.endpointUrl,
            kmsServer.region,
            kmsServer.getCredentialsProvider(),
            HttpClientEngineProvider.defaultHttpEngine
        )
    }
}
