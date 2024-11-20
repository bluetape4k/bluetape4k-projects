package io.bluetape4k.aws.kotlin.tests

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.net.url.Url
import io.bluetape4k.testcontainers.aws.LocalStackServer
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.localstack.LocalStackContainer

/**
 * AWS 특정 서비스([LocalStackContainer.Service])를 제공하는 [LocalStackServer]를 실행하고, 인스턴스를 반환합니다.
 */
fun getLocalStackServer(vararg services: LocalStackContainer.Service): LocalStackServer {
    return LocalStackServer().apply {
        withServices(*services)
        start()
        ShutdownQueue.register(this)
    }
}

/**
 * [LocalStackContainer]의 endpoint를 Aws Kotlin SDK의 [Url]로 변환합니다.
 */
val LocalStackContainer.endpointUrl: Url
    get() = Url.parse(this.endpoint.toString())

/**
 * [LocalStackContainer]를 사용하기 위한 [CredentialsProvider]를 반환합니다.
 *
 * @return [StaticCredentialsProvider] 인스턴스
 */
fun LocalStackContainer.getCredentialsProvider(): StaticCredentialsProvider {
    return StaticCredentialsProvider {
        accessKeyId = this@getCredentialsProvider.accessKey
        secretAccessKey = this@getCredentialsProvider.secretKey
    }
}
