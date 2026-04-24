package io.bluetape4k.http.hc5.classic

import io.bluetape4k.logging.KLogging
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder

/**
 * Virtual Threads 기반 HC5 Classic HTTP 클라이언트를 생성합니다.
 * 블로킹 I/O를 Virtual Thread 에서 실행하여 높은 동시성을 달성합니다.
 */
object VirtualThreadHttpClients: KLogging()

/**
 * Virtual Threads 기반 HC5 Classic [CloseableHttpClient]를 생성합니다.
 *
 * 블로킹 I/O 경로에서도 Virtual Thread의 경량 스케줄링을 활용하여 높은 동시성을 달성합니다.
 *
 * ```kotlin
 * val client = virtualThreadHttpClientOf(maxConnTotal = 200, maxConnPerRoute = 50)
 * client.use {
 *     val response = it.execute(HttpGet("https://example.com"))
 *     println(response.code)
 * }
 * ```
 *
 * @param maxConnTotal 전체 최대 커넥션 수 (기본값: 200)
 * @param maxConnPerRoute 라우트당 최대 커넥션 수 (기본값: 100)
 * @return 생성된 [CloseableHttpClient] 인스턴스
 */
fun virtualThreadHttpClientOf(
    maxConnTotal: Int = 200,
    maxConnPerRoute: Int = 100,
): CloseableHttpClient {
    val connManager = PoolingHttpClientConnectionManagerBuilder.create()
        .setMaxConnTotal(maxConnTotal)
        .setMaxConnPerRoute(maxConnPerRoute)
        .build()
    return HttpClients.custom()
        .setConnectionManager(connManager)
        .build()
}
