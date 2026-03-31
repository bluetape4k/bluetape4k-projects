package io.bluetape4k.aws.kotlin.kms

import io.bluetape4k.aws.kotlin.AbstractAwsTest
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.aws.LocalStackServer

/**
 * AWS Kotlin SDK KMS 테스트를 위한 추상 기반 클래스.
 *
 * LocalStack을 사용하여 로컬 환경에서 AWS KMS API를 테스트합니다.
 * [LocalStackServer]를 통해 KMS 서비스 컨테이너를 자동으로 시작하고,
 * 각 테스트에서 [withKmsClient] 패턴으로 클라이언트를 생성합니다.
 *
 * 사용 예시:
 * ```kotlin
 * class MyKmsTest : AbstractKmsTest() {
 *     @Test
 *     fun `키 생성 테스트`() = runTest {
 *         withKmsClient(localStackServer.endpointUrl, localStackServer.region, localStackServer.credentialsProvider) { client ->
 *             val response = client.createKey {
 *                 description = "테스트 키"
 *             }
 *             // assertions ...
 *         }
 *     }
 * }
 * ```
 */
abstract class AbstractKmsTest: AbstractAwsTest() {

    companion object: KLoggingChannel() {
        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(min: Int = 256, max: Int = 2048): String {
            return Fakers.randomString(min, max)
        }
    }
}
