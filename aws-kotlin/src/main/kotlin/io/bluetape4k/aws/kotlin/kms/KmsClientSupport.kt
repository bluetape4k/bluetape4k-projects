package io.bluetape4k.aws.kotlin.kms

import aws.sdk.kotlin.services.kms.KmsClient
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.net.url.Url
import io.bluetape4k.aws.kotlin.http.HttpClientEngineProvider
import io.bluetape4k.utils.ShutdownQueue

/**
 * AWS Kotlin SDK KMS 클라이언트를 생성합니다.
 *
 * AWS Key Management Service(KMS)는 암호화 키를 생성하고 관리하며,
 * 광범위한 AWS 서비스 및 애플리케이션에서 사용할 수 있는 관리형 서비스입니다.
 *
 * 예시:
 * ```kotlin
 * val client = kmsClientOf(
 *     endpointUrl = Url.parse("http://localhost:4566"),
 *     region = "us-east-1",
 *     credentialsProvider = myCredentialsProvider
 * )
 * ```
 *
 * @param endpointUrl KMS 서비스 엔드포인트 URL. null이면 기본 AWS 엔드포인트를 사용합니다.
 * @param region AWS 리전. null이면 환경 설정에서 자동으로 감지합니다.
 * @param credentialsProvider AWS 인증 정보 제공자. null이면 기본 자격 증명 체인을 사용합니다.
 * @param httpClient HTTP 클라이언트 엔진. 기본값은 [HttpClientEngineProvider.defaultHttpEngine]입니다.
 * @param builder [KmsClient.Config.Builder]에 대한 추가 설정 람다.
 * @return 설정된 [KmsClient] 인스턴스.
 */
inline fun kmsClientOf(
    endpointUrl: Url? = null,
    region: String? = null,
    credentialsProvider: CredentialsProvider? = null,
    httpClient: HttpClientEngine = HttpClientEngineProvider.defaultHttpEngine,
    @BuilderInference crossinline builder: KmsClient.Config.Builder.() -> Unit = {},
): KmsClient = KmsClient {
    endpointUrl?.let { this.endpointUrl = it }
    region?.let { this.region = it }
    credentialsProvider?.let { this.credentialsProvider = it }
    this.httpClient = httpClient

    builder()
}.apply {
    ShutdownQueue.register(this)
}
