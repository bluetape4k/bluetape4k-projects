package io.bluetape4k.testcontainers.storage

import io.bluetape4k.LibraryName
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.redisson.api.redisnode.RedisNodes
import kotlin.collections.set

@Execution(ExecutionMode.SAME_THREAD)
class RedisClusterServerTest {

    companion object: KLogging()

    @Test
    fun `create redis cluster server`() {
        RedisClusterServer().use { redisCluster ->
            redisCluster.start()
            redisCluster.isRunning.shouldBeTrue()

            verifyWithLettuce(redisCluster)
            verifyWithRedisson(redisCluster)
        }
    }

    // Mac AirPlay 가 Port 5000, 7000 을 사용해서 충돌이 생길 경우 AirPlay 를 끄세요 :)
    @Disabled("Mac AirPlay 사용 시 Port 5000, 7000를 사용해서 충돌이 생깁니다")
    @Test
    fun `create redis server with default port`() {
        RedisClusterServer(useDefaultPort = true).use { redisCluster ->
            redisCluster.start()
            redisCluster.isRunning.shouldBeTrue()
            redisCluster.port shouldBeEqualTo RedisClusterServer.PORTS[0]

            verifyWithRedisson(redisCluster)
            verifyWithLettuce(redisCluster)
        }
    }

    private fun verifyWithLettuce(redisCluster: RedisClusterServer) {
        RedisClusterServer.Launcher.LettuceLib.getClusterClient(redisCluster).use { clusterClient ->
            clusterClient.connect().use { connection ->
                val commands = connection.sync()
                log.info { "cluster info: ${commands.clusterInfo()}" }

                commands.ping() shouldBeEqualTo "PONG"
                // 모든 노드에 PING 을 실행해봅니다.
                commands.all().commands().ping().forEach { it shouldBeEqualTo "PONG" }
                commands.set("key", "value")
                commands.get("key") shouldBeEqualTo "value"
            }
        }
    }

    private fun verifyWithRedisson(redisCluster: RedisClusterServer) {
        val redisson = RedisClusterServer.Launcher.RedissonLib.getRedisson(redisCluster)
        try {
            redisson.getRedisNodes(RedisNodes.CLUSTER).masters.all { it.ping() }.shouldBeTrue()
            redisson.getRedisNodes(RedisNodes.CLUSTER).pingAll().shouldBeTrue()

            val map = redisson.getMap<String, String>("$LibraryName:map")
            map["key"] = "value"
            map["key"] shouldBeEqualTo "value"
        } finally {
            redisson.shutdown()
        }
    }
}
