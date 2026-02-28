package io.bluetape4k.aws.kms

import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.utils.ShutdownQueue
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kms.KmsAsyncClient
import software.amazon.awssdk.services.kms.KmsAsyncClientBuilder
import java.net.URI

/**
 * DSL 스타일의 빌더 람다로 비동기식 [KmsAsyncClient]를 생성합니다.
 *
 * 생성된 클라이언트는 JVM 종료 시 자동으로 닫히도록 [ShutdownQueue]에 등록됩니다.
 *
 * @param builder [KmsAsyncClientBuilder]에 대한 설정 람다.
 * @return 설정된 [KmsAsyncClient] 인스턴스.
 */
inline fun kmsAsyncClient(
    @BuilderInference builder: KmsAsyncClientBuilder.() -> Unit,
): KmsAsyncClient =
    KmsAsyncClient.builder().apply(builder).build()
        .apply {
            ShutdownQueue.register(this)
        }

/**
 * 주요 파라미터를 직접 지정하여 비동기식 [KmsAsyncClient]를 생성합니다.
 *
 * 비동기 클라이언트는 [CompletableFuture]를 반환하며, Coroutines에서는
 * `kotlinx-coroutines-jdk8`의 `.await()`를 사용하여 suspend function으로 변환할 수 있습니다.
 *
 * 생성된 클라이언트는 JVM 종료 시 자동으로 닫히도록 [ShutdownQueue]에 등록됩니다.
 *
 * 예시:
 * ```kotlin
 * val client = kmsAsyncClientOf(
 *     endpointOverride = URI.create("http://localhost:4566"),
 *     region = Region.US_EAST_1,
 *     credentialsProvider = StaticCredentialsProvider.create(...)
 * )
 * val response = client.createKey { ... }.await()
 * ```
 *
 * @param endpointOverride KMS 서비스 엔드포인트 URI (LocalStack 등 로컬 환경에서 사용).
 * @param region AWS 리전.
 * @param credentialsProvider AWS 인증 정보 제공자.
 * @param httpClient 비동기 HTTP 클라이언트. 기본값은 [SdkAsyncHttpClientProvider.defaultHttpClient].
 * @param builder [KmsAsyncClientBuilder]에 대한 추가 설정 람다.
 * @return 설정된 [KmsAsyncClient] 인스턴스.
 */
inline fun kmsAsyncClientOf(
    endpointOverride: URI,
    region: Region,
    credentialsProvider: AwsCredentialsProvider,
    httpClient: SdkAsyncHttpClient = SdkAsyncHttpClientProvider.defaultHttpClient,
    @BuilderInference builder: KmsAsyncClientBuilder.() -> Unit,
): KmsAsyncClient = kmsAsyncClient {
    endpointOverride(endpointOverride)
    region(region)
    credentialsProvider(credentialsProvider)
    httpClient(httpClient)

    builder()
}
