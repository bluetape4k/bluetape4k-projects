package io.bluetape4k.testcontainers.infra

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import io.bluetape4k.testcontainers.AbstractContainerTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ZooKeeperServerTest: AbstractContainerTest() {

    companion object: KLogging()

    @Nested
    inner class UseDefaultPort {
        @Test
        fun `create zookeeper with default port`() {
            ZooKeeperServer(useDefaultPort = true).use { zks ->
                zks.start()
                zks.isRunning.shouldBeTrue()
                zks.port shouldBeEqualTo ZooKeeperServer.PORT

                verifyZookeeper(zks)
            }
        }
    }

    @Nested
    inner class UseDockerPort {
        @Test
        fun `create zookeeper with docker port`() {
            ZooKeeperServer.Launcher.zookeeper.use { zks ->
                zks.isRunning.shouldBeTrue()
                verifyZookeeper(zks)
            }

        }
    }

    private fun verifyZookeeper(zks: ZooKeeperServer) {
        val path = "/messages/zk-tc"
        val content = "Running Zookeeper with Testcontainers"
        withCuratorFramework(zks) {
            create().creatingParentsIfNeeded().forPath(path, content.toUtf8Bytes())

            val retrieved = data.forPath(path).toUtf8String()
            retrieved shouldBeEqualTo content
        }
    }
}
