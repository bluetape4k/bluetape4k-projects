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
 * ## 동작/계약
 * - [KmsAsyncClient.builder]에 [builder]를 적용한 뒤 `build()`를 호출합니다.
 * - 생성된 클라이언트 인스턴스를 [ShutdownQueue]에 등록합니다.
 *
 * ```kotlin
 * val client = kmsAsyncClient {
 *     region(Region.AP_NORTHEAST_2)
 * }
 * // client.serviceName() == "kms"
 * ```
 */
inline fun kmsAsyncClient(
    builder: KmsAsyncClientBuilder.() -> Unit,
): KmsAsyncClient =
    KmsAsyncClient.builder().apply(builder).build()
        .apply {
            ShutdownQueue.register(this)
        }

/**
 * 주요 파라미터를 직접 지정하여 비동기식 [KmsAsyncClient]를 생성합니다.
 *
 * ## 동작/계약
 * - `null`이 아닌 인자만 [KmsAsyncClientBuilder]에 반영합니다.
 * - [httpClient]는 항상 `httpClient(httpClient)`로 설정합니다.
 * - 마지막에 [builder]를 호출하고 [kmsAsyncClient]를 통해 클라이언트를 생성/등록합니다.
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
 */
inline fun kmsAsyncClientOf(
    endpointOverride: URI? = null,
    region: Region? = null,
    credentialsProvider: AwsCredentialsProvider? = null,
    httpClient: SdkAsyncHttpClient = SdkAsyncHttpClientProvider.defaultHttpClient,
    builder: KmsAsyncClientBuilder.() -> Unit,
): KmsAsyncClient = kmsAsyncClient {
    endpointOverride?.let { endpointOverride(it) }
    region?.let { region(it) }
    credentialsProvider?.let { credentialsProvider(it) }
    httpClient(httpClient)

    builder()
}
