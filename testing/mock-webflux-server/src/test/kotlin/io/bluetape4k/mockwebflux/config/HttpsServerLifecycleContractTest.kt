package io.bluetape4k.mockwebflux.config

import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeEqualTo
import io.bluetape4k.mockwebflux.AbstractMockWebfluxServerTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.server.reactive.HttpHandler

/**
 * 테스트 HTTPS lifecycle의 포트 격리 계약을 검증한다.
 */
class HttpsServerLifecycleContractTest : AbstractMockWebfluxServerTest() {

    @Autowired
    private lateinit var lifecycle: HttpsServerLifecycle

    @Test
    fun `test HTTPS lifecycle binds an ephemeral port`() {
        lifecycle.isRunning.shouldBeTrue()

        lifecycle.boundPort shouldBeGreaterThan 0
        lifecycle.boundPort shouldNotBeEqualTo 8443
    }

    @Test
    fun `standalone HTTPS lifecycle stops and clears its bound port`() {
        val standalone = HttpsServerLifecycle(
            HttpHandler { _, response -> response.setComplete() },
            0,
            "changeit",
        )

        standalone.start()
        try {
            standalone.isRunning.shouldBeTrue()
            standalone.boundPort shouldBeGreaterThan 0
        } finally {
            standalone.stop()
        }

        standalone.isRunning.shouldBeFalse()
        standalone.boundPort shouldBeEqualTo 0
    }
}
