package io.bluetape4k.testcontainers.infra

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory
import java.util.concurrent.TimeUnit

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
        curator.blockUntilConnected(10, TimeUnit.SECONDS)
        curator.block()
    }
}
