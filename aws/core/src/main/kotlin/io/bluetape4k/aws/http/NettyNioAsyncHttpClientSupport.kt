package io.bluetape4k.aws.http

import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Netty NIO 빌더 DSL로 [SdkAsyncHttpClient]를 생성합니다.
 *
 * ## 동작/계약
 * - [NettyNioAsyncHttpClient.builder]에 [builder]를 적용한 뒤 `build()`를 반환한다.
 * - [builder]에서 지정한 설정이 최종 클라이언트에 반영된다.
 *
 * ```kotlin
 * val client = nettyNioAsyncHttpClient {
 *     maxConcurrency(32)
 * }
 * // client != null
 * ```
 */
inline fun nettyNioAsyncHttpClient(
    @BuilderInference builder: NettyNioAsyncHttpClient.Builder.() -> Unit,
): SdkAsyncHttpClient {
    return NettyNioAsyncHttpClient.builder().apply(builder).build()
}

/**
 * 기본 타임아웃/동시성 값을 적용한 Netty NIO [SdkAsyncHttpClient]를 생성합니다.
 *
 * ## 동작/계약
 * - 기본값은 `maxConcurrency=100`, 각 타임아웃은 `30.seconds`다.
 * - 내부에서 Java Duration으로 변환해 `maxConcurrency/connectionMaxIdleTime/connectionTimeout/readTimeout/writeTimeout`를 순서대로 설정한다.
 * - 마지막에 전달된 [builder]를 추가 적용해 기본값을 덮어쓸 수 있다.
 *
 * ```kotlin
 * val client = nettyNioAsyncHttpClientOf(maxConcurrency = 64) {
 *     writeTimeout(java.time.Duration.ofSeconds(10))
 * }
 * // client != null
 * ```
 */
inline fun nettyNioAsyncHttpClientOf(
    maxConcurrency: Int = 100,
    connectionMaxIdleTime: Duration = 30.seconds,
    connectionTimeout: Duration = 30.seconds,
    readTimeout: Duration = 30.seconds,
    writeTimeout: Duration = 30.seconds,
    @BuilderInference builder: NettyNioAsyncHttpClient.Builder.() -> Unit = {},
): SdkAsyncHttpClient = nettyNioAsyncHttpClient {
    this.maxConcurrency(maxConcurrency)
    this.connectionMaxIdleTime(connectionMaxIdleTime.toJavaDuration())
    this.connectionTimeout(connectionTimeout.toJavaDuration())
    this.readTimeout(readTimeout.toJavaDuration())
    this.writeTimeout(writeTimeout.toJavaDuration())

    builder()
}
