package io.bluetape4k.testcontainers.infra

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.AbstractContainerTest
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertFailsWith

/**
 * [ToxiproxyServer] 테스트입니다.
 *
 * TODO: proxy를 통한 upstream 통신 테스트는 macOS Docker Desktop 네트워킹 이슈로 보류 중입니다.
 *       ToxiproxyContainer의 proxy 포트(8666)를 통한 트래픽이 upstream에 전달되지 않는 현상이 있습니다.
 *       raw RESP 소켓, Lettuce, Jedis 등 다양한 클라이언트로 시도했으나 동일하게 실패합니다.
 *       향후 Docker Desktop 업데이트 또는 Linux 환경에서 재검증이 필요합니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ToxiproxyServerTest: AbstractContainerTest() {

    companion object: KLogging()

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { ToxiproxyServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { ToxiproxyServer(tag = " ") }
    }

    @Test
    fun `toxiproxy 서버가 정상적으로 시작된다`() {
        val server = ToxiproxyServer()
        server.start()
        try {
            server.isRunning.shouldBeTrue()
            log.debug { "ToxiproxyServer control port: ${server.controlPort}" }
        } finally {
            server.stop()
        }
    }

    // TODO: proxy를 통한 upstream 통신 테스트 (Redis latency toxic 등)는
    //       macOS Docker Desktop 환경에서 포트 매핑 이슈로 보류합니다.
    //       공식 testcontainers-java ToxiproxyContainerTest 패턴(Redis + Jedis)과 동일하게
    //       구성했으나, getMappedPort(8666)를 통한 프록시 접속 시 연결이 즉시 종료됩니다.
}
