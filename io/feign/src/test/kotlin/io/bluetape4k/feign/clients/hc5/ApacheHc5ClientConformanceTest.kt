package io.bluetape4k.feign.clients.hc5

import feign.AsyncClient
import feign.Client
import feign.Request
import feign.hc5.ApacheHttp5Client
import feign.hc5.AsyncApacheHttp5Client
import io.bluetape4k.feign.clients.FeignAsyncClientConformanceTest
import io.bluetape4k.feign.clients.FeignSyncClientConformanceTest
import io.bluetape4k.http.hc5.async.httpAsyncClientOf
import io.bluetape4k.http.hc5.classic.httpClientOf
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.feign.feignRequestOf
import io.bluetape4k.support.closeSafe
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.api.AfterEach
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import java.util.concurrent.TimeUnit
import org.apache.hc.client5.http.protocol.HttpClientContext
import java.util.*

class ApacheHc5ClientConformanceTest: FeignSyncClientConformanceTest() {

    private lateinit var transport: CloseableHttpClient

    override fun newClient(): Client =
        ApacheHttp5Client(httpClientOf().also { transport = it })

    @AfterEach
    fun closeTransport() {
        transport.close()
    }

    @ParameterizedTest
    @ValueSource(longs = [2147483648L, Long.MAX_VALUE])
    fun `Int 범위를 넘는 streaming 응답 길이는 null이며 음수로 좁히지 않는다`(length: Long) {
        MockWebServer().use { server ->
            server.start()
            // 대용량 본문을 만들지 않고 응답 헤더의 long-to-int 변환만 검증합니다.
            server.enqueue(
                MockResponse().setBody("")
                    .setHeader("Content-Length", length)
                    .setSocketPolicy(SocketPolicy.DISCONNECT_AT_END)
            )
            httpClientOf().use { transport ->
                val response = ApacheHttp5Client(transport).execute(
                    feignRequestOf(server.url("/").toString()),
                    Request.Options(1, TimeUnit.SECONDS, 2, TimeUnit.SECONDS, true)
                )
                try {
                    response.status() shouldBeEqualTo 200
                    response.body().length().shouldBeNull()
                } finally {
                    // 의도적으로 잘린 body의 close 오류는 길이 메타데이터 검증 대상이 아닙니다.
                    response.closeSafe()
                }
            }
        }
    }
}

class ApacheHc5AsyncClientConformanceTest: FeignAsyncClientConformanceTest<HttpClientContext>() {

    override fun newAsyncClient(): AsyncClient<HttpClientContext> =
        AsyncApacheHttp5Client(httpAsyncClientOf())

    override fun requestContext(): Optional<HttpClientContext> =
        Optional.empty()
}
