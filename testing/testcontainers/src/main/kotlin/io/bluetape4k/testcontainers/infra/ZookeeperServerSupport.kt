package io.bluetape4k.testcontainers.infra

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory

@PublishedApi
internal inline fun curatorFrameworkOf(
    @BuilderInference builder: CuratorFrameworkFactory.Builder.() -> Unit,
): CuratorFramework {
    return CuratorFrameworkFactory.builder().apply(builder).build()
}

@PublishedApi
internal inline fun <T> withCuratorFramework(
    zookeeper: ZooKeeperServer,
    @BuilderInference block: CuratorFramework.() -> T,
): T {
    return ZooKeeperServer.Launcher.getCuratorFramework(zookeeper).use { curator ->
        curator.start()
        curator.block()
    }
}
