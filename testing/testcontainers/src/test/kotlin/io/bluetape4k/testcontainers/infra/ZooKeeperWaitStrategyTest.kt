package io.bluetape4k.testcontainers.infra

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.apache.curator.framework.CuratorFramework
import org.junit.jupiter.api.Test
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget
import java.util.concurrent.TimeUnit

class ZooKeeperWaitStrategyTest {

    @Test
    fun `실제 Curator 세션이 연결된 뒤에만 readiness 를 통과한다`() {
        val target = waitStrategyTarget()
        val curator = mockk<CuratorFramework>(relaxed = true)
        every { curator.blockUntilConnected(any<Int>(), any<TimeUnit>()) } returns true
        var connectString: String? = null

        ZooKeeperWaitStrategy {
            connectString = it
            curator
        }.waitUntilReady(target)

        connectString shouldBeEqualTo "127.0.0.1:32181"
        verifyOrder {
            curator.start()
            curator.blockUntilConnected(any<Int>(), any<TimeUnit>())
            curator.close()
        }
    }

    @Test
    fun `Curator 세션 연결에 실패하면 readiness 를 통과시키지 않는다`() {
        val target = waitStrategyTarget()
        val curator = mockk<CuratorFramework>(relaxed = true)
        every { curator.blockUntilConnected(any<Int>(), any<TimeUnit>()) } returns false

        val exception = assertFailsWith<IllegalStateException> {
            ZooKeeperWaitStrategy { curator }.waitUntilReady(target)
        }

        exception.message shouldBeEqualTo "ZooKeeper Curator session was not established at 127.0.0.1:32181"
        verify { curator.close() }
    }

    private fun waitStrategyTarget(): WaitStrategyTarget = mockk {
        every { host } returns "127.0.0.1"
        every { getMappedPort(ZooKeeperServer.PORT) } returns 32181
    }
}
