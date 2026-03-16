package io.bluetape4k.aws.http

import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * CRT 빌더 DSL로 [SdkAsyncHttpClient]를 생성합니다.
 *
 * ## 동작/계약
 * - [AwsCrtAsyncHttpClient.builder]에 [builder]를 적용한 뒤 `build()`를 반환한다.
 * - [builder]에서 지정한 설정이 최종 클라이언트에 반영된다.
 *
 * 참고: [AWSCRT 기반 HTTP 클라이언트 설정](https://docs.aws.amazon.com/ko_kr/sdk-for-java/latest/developer-guide/http-configuration-crt.html)
 *
 * NOTE: [AwsCrtAsyncHttpClient]를 사용하려면 참조에서 `netty-nio-client` 를 제거해야 합니다. (동시 사용은 불가능)
 *
 * ```kotlin
 * val client = awsCrtAsyncHttpClient {
 *     maxConcurrency(64)
 * }
 * // client != null
 * ```
 */
inline fun awsCrtAsyncHttpClient(
    builder: AwsCrtAsyncHttpClient.Builder.() -> Unit,
): SdkAsyncHttpClient {
    return AwsCrtAsyncHttpClient.builder().apply(builder).build()
}

/**
 * 기본 동시성/버퍼/타임아웃 설정을 적용한 CRT [SdkAsyncHttpClient]를 생성합니다.
 *
 * ## 동작/계약
 * - 기본값은 `maxConcurrency=100`, `readBufferSize=2*1024*1024`, `connectionMaxIdleTime=30.seconds`, `connectionTimeout=5.seconds`, `postQuantumTlsEnabled=false`다.
 * - Kotlin [Duration]을 Java Duration으로 변환해 CRT 빌더에 적용한다.
 * - 마지막에 전달된 [builder]를 적용하므로 기본값을 선택적으로 덮어쓸 수 있다.
 *
 * ```kotlin
 * val client = awsCrtAsyncHttpClientOf(
 *     maxConcurrency = 128,
 *     postQuantumTlsEnabled = true,
 * )
 * // client != null
 * ```
 */
inline fun awsCrtAsyncHttpClientOf(
    maxConcurrency: Int = 100,
    readBufferSize: Long = 2 * 1024 * 1024,
    connectionMaxIdleTime: Duration = 30.seconds,
    connectionTimeout: Duration = 5.seconds,
    postQuantumTlsEnabled: Boolean = false,
    builder: AwsCrtAsyncHttpClient.Builder.() -> Unit = {},
): SdkAsyncHttpClient = awsCrtAsyncHttpClient {
    this.maxConcurrency(maxConcurrency)
    this.readBufferSizeInBytes(readBufferSize)
    this.connectionMaxIdleTime(connectionMaxIdleTime.toJavaDuration())
    this.connectionTimeout(connectionTimeout.toJavaDuration())
    this.postQuantumTlsEnabled(postQuantumTlsEnabled)

    builder()
}
