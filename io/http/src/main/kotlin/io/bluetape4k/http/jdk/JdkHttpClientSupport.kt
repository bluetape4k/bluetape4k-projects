package io.bluetape4k.http.jdk

import io.bluetape4k.logging.KLogging
import java.net.http.HttpClient
import java.time.Duration
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * JDK 내장 [HttpClient] 를 생성하는 지원 함수 모음입니다.
 *
 * java.net.http.HttpClient 는 JDK 11 이상에서 제공하며, HTTP/1.1 및 HTTP/2 를 지원합니다.
 * Virtual Threads 를 함께 사용하면 높은 동시성을 달성할 수 있습니다.
 */
object JdkHttpClients: KLogging() {
    /** 기본 HTTP/2 + HTTP/1.1 클라이언트 */
    val default: HttpClient by lazy { jdkHttpClientOf() }

    /** Virtual Thread executor 기반 클라이언트 */
    val virtualThread: HttpClient by lazy { jdkVirtualThreadHttpClientOf() }
}

/**
 * JDK [HttpClient] 를 생성합니다.
 *
 * ```kotlin
 * val client = jdkHttpClientOf(connectTimeout = Duration.ofSeconds(10))
 * ```
 *
 * @param connectTimeout 연결 타임아웃 (기본 5초)
 * @param executor 요청을 실행할 [Executor] (기본값: null — JDK 기본 executor 사용)
 * @param version 사용할 HTTP 프로토콜 버전 (기본값: HTTP/2)
 * @return [HttpClient] 인스턴스
 */
fun jdkHttpClientOf(
    connectTimeout: Duration = Duration.ofSeconds(5),
    executor: Executor? = null,
    version: HttpClient.Version = HttpClient.Version.HTTP_2,
): HttpClient = HttpClient.newBuilder()
    .connectTimeout(connectTimeout)
    .version(version)
    .apply { executor?.let { executor(it) } }
    .build()

/**
 * Virtual Thread executor 기반 JDK [HttpClient] 를 생성합니다.
 *
 * 블로킹 I/O 를 Virtual Thread 풀에서 실행하여 높은 동시성을 달성합니다.
 *
 * ```kotlin
 * val client = jdkVirtualThreadHttpClientOf()
 * val response = client.send(request, HttpResponse.BodyHandlers.ofString())
 * ```
 *
 * @param connectTimeout 연결 타임아웃 (기본 5초)
 * @param version 사용할 HTTP 프로토콜 버전 (기본값: HTTP/2)
 * @return Virtual Thread 기반 [HttpClient] 인스턴스
 */
fun jdkVirtualThreadHttpClientOf(
    connectTimeout: Duration = Duration.ofSeconds(5),
    version: HttpClient.Version = HttpClient.Version.HTTP_2,
): HttpClient = jdkHttpClientOf(
    connectTimeout = connectTimeout,
    executor = Executors.newVirtualThreadPerTaskExecutor(),
    version = version,
)
