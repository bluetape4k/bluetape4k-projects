package io.bluetape4k.testcontainers.infra

import eu.rekawek.toxiproxy.ToxiproxyClient
import eu.rekawek.toxiproxy.model.ToxicDirection
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.testcontainers.http.HttpbinServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.Network
import kotlin.test.assertFailsWith
import kotlin.time.measureTime


/**
 * [ToxiproxyServer] 통합 테스트입니다.
 *
 * - Singleton 패턴을 사용하지 않아 toxic 상태 누수를 방지합니다.
 * - 각 테스트에서 컨테이너를 직접 생성하고 [AfterEach]에서 정리합니다.
 * - upstream 서버로 [HttpbinServer]를 사용하며 동일한 Docker [Network]에 연결합니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ToxiproxyServerTest: AbstractContainerTest() {

    companion object: KLogging() {
        /** 프록시가 수신할 컨테이너 내부 포트 번호입니다 (ToxiproxyContainer 노출 범위: 8666-8697). */
        private const val PROXY_PORT = 8666

        /** 레이턴시 테스트에서 주입할 지연 시간(ms)입니다. */
        private const val LATENCY_MS = 500L

        /** 레이턴시 복구 후 허용할 최대 응답 시간(ms)입니다. */
        private const val FAST_RESPONSE_MAX_MS = 450L
    }

    private var network: Network? = null
    private var httpbin: HttpbinServer? = null
    private var toxiproxy: ToxiproxyServer? = null

    /**
     * 각 테스트에서 공유 Docker 네트워크, httpbin upstream, toxiproxy 서버를 생성하고 시작합니다.
     */
    private fun setup() {
        network = Network.newNetwork()
        httpbin = HttpbinServer().apply {
            withNetwork(network)
            withNetworkAliases("httpbin")
            start()
        }
        toxiproxy = ToxiproxyServer().apply {
            withNetwork(network)
            start()
        }
    }

    /**
     * 각 테스트 종료 후 컨테이너와 네트워크를 정리합니다.
     */
    @AfterEach
    fun cleanup() {
        toxiproxy?.stop()
        httpbin?.stop()
        network?.close()
        toxiproxy = null
        httpbin = null
        network = null
    }

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { ToxiproxyServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { ToxiproxyServer(tag = " ") }
    }

    @Test
    fun `toxiproxy 서버가 정상적으로 시작된다`() {
        setup()
        toxiproxy!!.isRunning.shouldBeTrue()
        log.debug { "ToxiproxyServer URL: ${toxiproxy!!.url}" }
        log.debug { "ToxiproxyServer port: ${toxiproxy!!.port}" }
    }

    @Test
    fun `proxy를 통해 httpbin에 정상 응답을 받는다`() {
        setup()
        val server = toxiproxy!!

        // ToxiproxyClient로 프록시 생성: httpbin 컨테이너 내부 별칭과 포트로 upstream 지정
        val client = ToxiproxyClient(server.host, server.port)
        val proxy = client.createProxy("httpbin-proxy", "0.0.0.0:$PROXY_PORT", "httpbin:${HttpbinServer.PORT}")
        proxy.shouldNotBeNull()

        // 호스트에서 접근 가능한 프록시 매핑 포트
        val mappedProxyPort = server.getMappedPort(PROXY_PORT)
        val proxyUrl = "http://${server.host}:$mappedProxyPort/get"
        log.debug { "Proxied URL: $proxyUrl" }

        val httpClient = OkHttpClient.Builder().build()
        val request = Request.Builder().url(proxyUrl).get().build()

        // 1단계: 프록시를 통해 정상 응답(200) 확인
        httpClient.newCall(request).execute().use { response ->
            log.debug { "Response code: ${response.code}" }
            response.code shouldBeEqualTo 200
        }
    }

    @Test
    fun `latency toxic 주입 후 응답 시간이 증가한다`() {
        setup()
        val server = toxiproxy!!

        val client = ToxiproxyClient(server.host, server.port)
        val proxy = client.createProxy("httpbin-latency", "0.0.0.0:$PROXY_PORT", "httpbin:${HttpbinServer.PORT}")

        val mappedProxyPort = server.getMappedPort(PROXY_PORT)
        val proxyUrl = "http://${server.host}:$mappedProxyPort/get"
        val httpClient = OkHttpClient.Builder()
            .callTimeout(10_000, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()
        val request = Request.Builder().url(proxyUrl).get().build()

        // 2단계: latency toxic 주입 → 응답 시간 >= LATENCY_MS 확인
        proxy.toxics().latency("latency", ToxicDirection.DOWNSTREAM, LATENCY_MS)

        val latencyDuration = measureTime {
            httpClient.newCall(request).execute().use { response ->
                log.debug { "Response code with latency: ${response.code}" }
                response.code shouldBeEqualTo 200
            }
        }
        log.debug { "Response time with latency toxic: ${latencyDuration.inWholeMilliseconds}ms" }
        latencyDuration.inWholeMilliseconds shouldBeGreaterOrEqualTo LATENCY_MS

        // 3단계: latency toxic 제거 → 응답 시간 정상 복구 (< FAST_RESPONSE_MAX_MS) 확인
        proxy.toxics().get("latency").remove()

        val fastDuration = measureTime {
            httpClient.newCall(request).execute().use { response ->
                log.debug { "Response code after removing latency: ${response.code}" }
                response.code shouldBeEqualTo 200
            }
        }
        log.debug { "Response time after removing latency toxic: ${fastDuration.inWholeMilliseconds}ms" }
        fastDuration.inWholeMilliseconds shouldBeLessOrEqualTo FAST_RESPONSE_MAX_MS
    }
}
