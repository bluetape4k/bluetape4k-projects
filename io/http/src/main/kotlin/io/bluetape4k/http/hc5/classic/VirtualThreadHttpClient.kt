package io.bluetape4k.http.hc5.classic

import io.bluetape4k.logging.KLogging
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import java.util.concurrent.Executors

/**
 * Virtual Threads 기반 HC5 Classic HTTP 클라이언트를 생성합니다.
 * 블로킹 I/O를 Virtual Thread 에서 실행하여 높은 동시성을 달성합니다.
 */
object VirtualThreadHttpClients: KLogging()

fun virtualThreadHttpClientOf(
    maxConnTotal: Int = 200,
    maxConnPerRoute: Int = 100,
): CloseableHttpClient {
    val vtExecutor = Executors.newVirtualThreadPerTaskExecutor()
    val connManager = PoolingHttpClientConnectionManagerBuilder.create()
        .setMaxConnTotal(maxConnTotal)
        .setMaxConnPerRoute(maxConnPerRoute)
        .build()
    return HttpClients.custom()
        .setConnectionManager(connManager)
        .build()
}
