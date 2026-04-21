package io.bluetape4k.mockserver

import io.bluetape4k.logging.KLogging
import okhttp3.OkHttpClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import java.util.concurrent.TimeUnit

/**
 * mock-web-server 통합 테스트 베이스 클래스.
 *
 * [SpringBootTest.WebEnvironment.RANDOM_PORT]로 임의의 포트에 Spring Boot 애플리케이션을 기동하고,
 * OkHttp 클라이언트를 통해 실제 HTTP 요청을 전송하여 엔드포인트 계약을 검증한다.
 */
@SpringBootTest(
    classes = [MockServerApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
abstract class MockServerTestBase {
    companion object : KLogging()

    @LocalServerPort
    protected var port: Int = 0

    /**
     * 테스트 대상 베이스 URL (예: `http://localhost:54321`).
     */
    protected val baseUrl: String get() = "http://localhost:$port"

    /**
     * 테스트에서 공용으로 사용할 OkHttp 클라이언트.
     *
     * 연결 타임아웃 5초, 읽기 타임아웃 30초로 설정한다.
     */
    protected val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
}
