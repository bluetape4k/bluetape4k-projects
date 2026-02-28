package io.bluetape4k.aws.kms

import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.utils.ShutdownQueue
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kms.KmsClient
import software.amazon.awssdk.services.kms.KmsClientBuilder
import java.net.URI

/**
 * DSL 스타일의 빌더 람다로 동기식 [KmsClient]를 생성합니다.
 *
 * 생성된 클라이언트는 JVM 종료 시 자동으로 닫히도록 [ShutdownQueue]에 등록됩니다.
 *
 * @param builder [KmsClientBuilder]에 대한 설정 람다.
 * @return 설정된 [KmsClient] 인스턴스.
 */
inline fun kmsClient(
    @BuilderInference builder: KmsClientBuilder.() -> Unit,
): KmsClient =
    KmsClient.builder().apply(builder).build()
        .apply {
            ShutdownQueue.register(this)
        }

/**
 * 주요 파라미터를 직접 지정하여 동기식 [KmsClient]를 생성합니다.
 *
 * 생성된 클라이언트는 JVM 종료 시 자동으로 닫히도록 [ShutdownQueue]에 등록됩니다.
 *
 * 예시:
 * ```kotlin
 * val client = kmsClientOf(
 *     endpointOverride = URI.create("http://localhost:4566"),
 *     region = Region.US_EAST_1,
 *     credentialsProvider = StaticCredentialsProvider.create(...)
 * )
 * ```
 *
 * @param endpointOverride KMS 서비스 엔드포인트 URI (LocalStack 등 로컬 환경에서 사용).
 * @param region AWS 리전.
 * @param credentialsProvider AWS 인증 정보 제공자.
 * @param httpClient 동기 HTTP 클라이언트. 기본값은 [SdkHttpClientProvider.defaultHttpClient].
 * @param builder [KmsClientBuilder]에 대한 추가 설정 람다.
 * @return 설정된 [KmsClient] 인스턴스.
 */
inline fun kmsClientOf(
    endpointOverride: URI? = null,
    region: Region? = null,
    credentialsProvider: AwsCredentialsProvider? = null,
    httpClient: SdkHttpClient = SdkHttpClientProvider.defaultHttpClient,
    @BuilderInference builder: KmsClientBuilder.() -> Unit = {},
): KmsClient = kmsClient {
    endpointOverride?.let { endpointOverride(it) }
    region?.let { region(it) }
    credentialsProvider?.let { credentialsProvider(it) }
    httpClient(httpClient)

    builder()
}
