package io.bluetape4k.aws.http

import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

inline fun nettyNioAsyncHttpClient(
    @BuilderInference builder: NettyNioAsyncHttpClient.Builder.() -> Unit,
): SdkAsyncHttpClient {
    return NettyNioAsyncHttpClient.builder().apply(builder).build()
}

fun nettyNioAsyncHttpClientOf(
    maxConcurrency: Int = 100,
    connectionMaxIdleTime: Duration = 30.seconds,
    connectionTimeout: Duration = 30.seconds,
    readTimeout: Duration = 30.seconds,
    writeTimeout: Duration = 30.seconds,
    initializer: NettyNioAsyncHttpClient.Builder.() -> Unit = {},
): SdkAsyncHttpClient = nettyNioAsyncHttpClient {
    this.maxConcurrency(maxConcurrency)
    this.connectionMaxIdleTime(connectionMaxIdleTime.toJavaDuration())
    this.connectionTimeout(connectionTimeout.toJavaDuration())
    this.readTimeout(readTimeout.toJavaDuration())
    this.writeTimeout(writeTimeout.toJavaDuration())

    initializer()
}
