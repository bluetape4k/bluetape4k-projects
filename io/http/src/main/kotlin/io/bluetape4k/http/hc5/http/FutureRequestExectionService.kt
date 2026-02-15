package io.bluetape4k.http.hc5.http

import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import org.apache.hc.client5.http.classic.HttpClient
import org.apache.hc.client5.http.impl.classic.FutureRequestExecutionService
import java.util.concurrent.ExecutorService

/**
 * [HttpClient]와 [ExecutorService]를 이용해 [FutureRequestExecutionService]를 생성합니다.
 *
 * ```
 * val executor = Executors.newFixedThreadPool(10)
 * val futureRequestExecutionService = futureRequestExecutionServiceOf(httpClient, executor)
 * ```
 *
 * @param httpclient [HttpClient] 인스턴스
 * @param executor [ExecutorService] 인스턴스 (기본값: [VirtualThreadExecutor])
 * @return 생성된 [FutureRequestExecutionService]
 */
fun futureRequestExecutionServiceOf(
    httpclient: HttpClient,
    executor: ExecutorService = VirtualThreadExecutor,
): FutureRequestExecutionService =
    FutureRequestExecutionService(httpclient, executor)
