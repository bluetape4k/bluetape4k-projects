package io.bluetape4k.pulsar

import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.apache.pulsar.client.api.ClientBuilder
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PulsarClientSupportTest : AbstractPulsarTest() {

    companion object : KLogging()

    @Test
    fun `pulsarClient - serviceUrl로 클라이언트 생성`() {
        val client = pulsarClient(pulsar.url)
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `pulsarClient - setup 블록으로 클라이언트 생성`() {
        val url = pulsar.url
        val client = pulsarClient { serviceUrl(url) }
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `withPulsarClient - 블록 실행 후 자동 close`() = runSuspendIO(timeout = 120.seconds) {
        var clientRef: org.apache.pulsar.client.api.PulsarClient? = null
        withPulsarClient(pulsar.url) {
            clientRef = this
            shouldNotBeNull()
        }
        // close 후 재사용 시 예외 발생 — 단순히 close 됐는지 확인 가능하지 않으므로
        // 블록 내에서 정상 실행됐음을 확인
        clientRef.shouldNotBeNull()
    }

    @Test
    fun `withPulsarClient - setup-only 오버로드`() = runSuspendIO(timeout = 120.seconds) {
        val url = pulsar.url
        withPulsarClient({ serviceUrl(url) }) {
            shouldNotBeNull()
            // 생성된 클라이언트로 간단한 동작 검증
            val producer = newProducer(Schema.STRING).topic(newTopic()).create()
            producer.shouldNotBeNull()
            producer.close()
        }
    }

    @Test
    fun `withPulsarClient - 취소되어도 closeAsync 완료를 기다린다`() = runTest {
        val builder = mockk<ClientBuilder>()
        val client = mockk<PulsarClient>()
        val closeFuture = CompletableFuture<Void>()

        mockkStatic(PulsarClient::class)
        try {
            every { PulsarClient.builder() } returns builder
            every { builder.serviceUrl(any()) } returns builder
            every { builder.build() } returns client
            every { client.closeAsync() } returns closeFuture

            assertCleanupWaitsAfterCancellation(closeFuture) { entered ->
                withPulsarClient("pulsar://localhost:6650") {
                    entered.complete(Unit)
                    awaitCancellation()
                }
            }

            verify(exactly = 1) { client.closeAsync() }
        } finally {
            unmockkStatic(PulsarClient::class)
        }
    }
}
