package io.bluetape4k.aws.kotlin.tests

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.net.url.Url
import io.bluetape4k.testcontainers.aws.LocalStackServer
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.localstack.LocalStackContainer

/**
 * AWS 특정 서비스([LocalStackContainer.Service])를 제공하는 [LocalStackServer]를 실행하고, 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val localStack = getLocalStackServer("sns", "sqs")
 * val client = snsClientOf(endpointUrl = localStack.endpointUrl)
 * ```
 *
 * @param services 활성화할 LocalStack 서비스 이름 목록 (예: "sns", "sqs", "s3")
 * @return 시작된 [LocalStackServer] 인스턴스
 */
fun getLocalStackServer(vararg services: String): LocalStackServer =
    LocalStackServer().apply {
        withServices(*services)
        start()
        ShutdownQueue.register(this)
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
fun LocalStackContainer.getCredentialsProvider(): StaticCredentialsProvider =
    StaticCredentialsProvider {
        accessKeyId = this@getCredentialsProvider.accessKey
        secretAccessKey = this@getCredentialsProvider.secretKey
    }
