package io.bluetape4k.aws.kotlin.s3

import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.net.url.Url
import io.bluetape4k.aws.kotlin.http.HttpClientEngineProvider
import io.bluetape4k.support.useSafe

/**
 * [S3Client] 를 생성합니다.
 *
 * ```kotlin
 * val s3Client = s3ClientOf(
 *    endpointUrl = Url.parse("http://localhost:4566"),
 *    region = "us-west-2",
 *    credentialsProvider = StaticCredentialsProvider { accessKeyId = "test"; secretAccessKey = "test" }
 * ) {
 *    clientName = "bluetape4k-s3-client"
 * }
 * ```
 *
 * @param endpointUrl S3 엔드포인트 URL
 * @param region AWS 리전
 * @param credentialsProvider AWS 자격 증명 제공자
 * @param httpClient [HttpClientEngine] 엔진 (기본적으로 [aws.smithy.kotlin.runtime.http.engine.crt.CrtHttpEngine] 를 사용합니다.)
 * @param builder [S3Client.Config.Builder] 를 통해 [S3Client.Config] 를 설정합니다.
 * @return [S3Client] 인스턴스
 */
inline fun s3ClientOf(
    endpointUrl: Url? = null,
    region: String? = null,
    credentialsProvider: CredentialsProvider? = null,
    httpClient: HttpClientEngine? = HttpClientEngineProvider.defaultHttpEngine,
    crossinline builder: S3Client.Config.Builder.() -> Unit = {},
): S3Client =
    S3Client {
        endpointUrl?.let { this.endpointUrl = it }
        region?.let { this.region = it }
        credentialsProvider?.let { this.credentialsProvider = it }
        httpClient?.let { this.httpClient = it }

        builder()
    }

/**
 * [S3Client]를 생성하고 [block]을 실행한 후 자동으로 닫습니다.
 *
 * SDK가 내부 HTTP 엔진을 직접 관리하므로 close() 시 엔진도 함께 종료됩니다.
 *
 * ```kotlin
 * withS3Client(endpointUrl, region, credentialsProvider) { client ->
 *     client.putObject { ... }
 * }
 * ```
 *
 * @param block suspend 블록. AWS SDK의 모든 operations는 suspend 함수이므로 이 블록도 suspend로 선언합니다.
 */
suspend fun <R> withS3Client(
    endpointUrl: Url? = null,
    region: String? = null,
    credentialsProvider: CredentialsProvider? = null,
    block: suspend (S3Client) -> R,
): R = s3ClientOf(endpointUrl, region, credentialsProvider).useSafe { client ->
    block(client)
}
