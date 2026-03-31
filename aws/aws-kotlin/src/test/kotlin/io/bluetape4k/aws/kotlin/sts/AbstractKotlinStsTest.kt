package io.bluetape4k.aws.kotlin.sts

import io.bluetape4k.aws.kotlin.AbstractAwsTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.aws.LocalStackServer

/**
 * AWS Kotlin SDK STS 테스트를 위한 추상 기반 클래스.
 *
 * LocalStack을 사용하여 로컬 환경에서 AWS STS API를 테스트합니다.
 * [LocalStackServer]를 통해 STS 서비스 컨테이너를 자동으로 시작하고,
 * 각 테스트에서 [withStsClient] 패턴으로 클라이언트를 생성합니다.
 *
 * 사용 예시:
 * ```kotlin
 * class MyStsTest : AbstractKotlinStsTest() {
 *     @Test
 *     fun `호출자 신원 조회`() = runTest {
 *         withStsClient(localStackServer.endpointUrl, localStackServer.region, localStackServer.credentialsProvider) { client ->
 *             val response = client.getCallerIdentity()
 *             // assertions ...
 *         }
 *     }
 * }
 * ```
 */
abstract class AbstractKotlinStsTest: AbstractAwsTest() {

    companion object: KLoggingChannel()
}
