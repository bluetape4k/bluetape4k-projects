package io.bluetape4k.testcontainers.infra

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.RetryForever
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy
import java.time.Duration
import java.util.concurrent.TimeUnit

@PublishedApi
internal const val CURATOR_BLOCK_TIMEOUT_SECONDS = 10
private const val READINESS_CURATOR_RETRY_DELAY_MS = 1_000
private const val READINESS_CURATOR_CONNECTION_TIMEOUT_MS = 10_000

@PublishedApi
internal inline fun curatorFrameworkOf(
    builder: CuratorFrameworkFactory.Builder.() -> Unit,
): CuratorFramework {
    return CuratorFrameworkFactory.builder().apply(builder).build()
}

@PublishedApi
internal inline fun <T> withCuratorFramework(
    zookeeper: ZooKeeperServer,
    block: CuratorFramework.() -> T,
): T {
    return ZooKeeperServer.Launcher.getCuratorFramework(zookeeper).use { curator ->
        curator.start()
        check(curator.blockUntilConnected(CURATOR_BLOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            "ZooKeeper Curator session was not established at ${zookeeper.url}"
        }
        curator.block()
    }
}

/**
 * 컨테이너 포트가 아닌 Curator session 수립을 ZooKeeper readiness 기준으로 사용합니다.
 *
 * 컨테이너의 2181 포트는 ZooKeeper가 실제 요청을 처리하기 전에 열릴 수 있으므로,
 * Testcontainers가 서버를 시작된 것으로 판정하기 전에 실제 애플리케이션 연결을 확인합니다.
 */
internal class ZooKeeperWaitStrategy(
    private val curatorFactory: (String) -> CuratorFramework = ::createReadinessCurator,
): AbstractWaitStrategy() {

    init {
        withStartupTimeout(READINESS_TIMEOUT)
    }

    override fun waitUntilReady() {
        val target = waitStrategyTarget
        val connectString = "${target.host}:${target.getMappedPort(ZooKeeperServer.PORT)}"

        curatorFactory(connectString).use { curator ->
            curator.start()
            check(
                curator.blockUntilConnected(READINESS_TIMEOUT.toMillis().toInt(), TimeUnit.MILLISECONDS),
            ) {
                "ZooKeeper Curator session was not established at $connectString"
            }
        }
    }

    private companion object {
        val READINESS_TIMEOUT: Duration = Duration.ofMinutes(2)
    }
}

private fun createReadinessCurator(connectString: String): CuratorFramework = curatorFrameworkOf {
    connectString(connectString)
    retryPolicy(RetryForever(READINESS_CURATOR_RETRY_DELAY_MS))
    connectionTimeoutMs(READINESS_CURATOR_CONNECTION_TIMEOUT_MS)
}
