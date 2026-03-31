package io.bluetape4k.aws.kotlin.sesv2

import aws.sdk.kotlin.services.sesv2.SesV2Client
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.net.url.Url
import io.bluetape4k.aws.kotlin.http.HttpClientEngineProvider
import io.bluetape4k.support.useSafe

/**
 * AWS Kotlin SDK [SesV2Client] 인스턴스를 생성합니다.
 *
 * ```kotlin
 * val client = sesV2ClientOf(
 *     endpointUrl = Url.parse("http://localhost:4566"),
 *     region = "us-east-1",
 *     credentialsProvider = credentialsProvider
 * )
 * ```
 *
 * @param endpointUrl SES V2 서비스 엔드포인트 URL. null이면 기본 AWS 엔드포인트를 사용합니다.
 * @param region AWS 리전. null이면 환경 설정에서 자동으로 감지합니다.
 * @param credentialsProvider AWS 인증 정보 제공자. null이면 기본 자격 증명 체인을 사용합니다.
 * @param httpClient HTTP 클라이언트 엔진. 기본값은 [HttpClientEngineProvider.defaultHttpEngine]입니다.
 * @param builder [SesV2Client.Config.Builder]에 대한 추가 설정 람다.
 * @return 설정된 [SesV2Client] 인스턴스.
 */
inline fun sesV2ClientOf(
    endpointUrl: Url? = null,
    region: String? = null,
    credentialsProvider: CredentialsProvider? = null,
    httpClient: HttpClientEngine? = HttpClientEngineProvider.defaultHttpEngine,
    crossinline builder: SesV2Client.Config.Builder.() -> Unit = {},
): SesV2Client = SesV2Client {
    endpointUrl?.let { this.endpointUrl = it }
    region?.let { this.region = it }
    credentialsProvider?.let { this.credentialsProvider = it }
    httpClient?.let { this.httpClient = it }

    builder()
}

/**
 * [SesV2Client]를 생성하고 [block]을 실행한 후 자동으로 닫습니다.
 *
 * SDK가 내부 HTTP 엔진을 직접 관리하므로 close() 시 엔진도 함께 종료됩니다.
 *
 * ```kotlin
 * withSesV2Client(endpointUrl, region, credentialsProvider) { client ->
 *     client.sendEmail { ... }
 * }
 * ```
 *
 * @param block suspend 블록. AWS SDK의 모든 operations는 suspend 함수이므로 이 블록도 suspend로 선언합니다.
 */
suspend fun <R> withSesV2Client(
    endpointUrl: Url? = null,
    region: String? = null,
    credentialsProvider: CredentialsProvider? = null,
    block: suspend (SesV2Client) -> R,
): R = sesV2ClientOf(endpointUrl, region, credentialsProvider).useSafe { client ->
    block(client)
}
