package io.bluetape4k.aws.http

import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Crt 기반의 [SdkAsyncHttpClient]를 생성합니다.
 *
 * 참고: [AWSCRT 기반 HTTP 클라이언트 설정](https://docs.aws.amazon.com/ko_kr/sdk-for-java/latest/developer-guide/http-configuration-crt.html)
 *
 * NOTE: [AwsCrtAsyncHttpClient]를 사용하려면 참조에서 `netty-nio-client` 를 제거해야 합니다. (동시 사용은 불가능)
 *
 * @param initializer [AwsCrtAsyncHttpClient.Builder]를 초기화하는 람다입니다.
 * @receiver
 * @return [SdkAsyncHttpClient] 인스턴스
 */
inline fun awsCrtAsyncHttpClient(
    initializer: AwsCrtAsyncHttpClient.Builder.() -> Unit,
): SdkAsyncHttpClient {
    return AwsCrtAsyncHttpClient.builder().apply(initializer).build()
}

fun awsCrtAsyncHttpClientOf(
    maxConcurrency: Int = 100,
    readBufferSize: Long = 2 * 1024 * 1024,
    connectionMaxIdleTime: Duration = 30.seconds,
    connectionTimeout: Duration = 5.seconds,
    postQuantumTlsEnabled: Boolean = false,
    initializer: AwsCrtAsyncHttpClient.Builder.() -> Unit = {},
): SdkAsyncHttpClient = awsCrtAsyncHttpClient {
    this.maxConcurrency(maxConcurrency)
    this.readBufferSizeInBytes(readBufferSize)
    this.connectionMaxIdleTime(connectionMaxIdleTime.toJavaDuration())
    this.connectionTimeout(connectionTimeout.toJavaDuration())
    this.postQuantumTlsEnabled(postQuantumTlsEnabled)

    initializer()
}
