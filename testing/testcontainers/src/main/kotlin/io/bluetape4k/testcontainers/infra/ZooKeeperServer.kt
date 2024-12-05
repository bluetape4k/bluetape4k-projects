package io.bluetape4k.testcontainers.infra

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.retry.RetryOneTime
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

/**
 * [ZooKeeper](https://zookeeper.apache.org/) 서버의 docker image를 testcontainers 환경 하에서 실행하는 클래스입니다.
 *
 * 참고: [ZooKeeper Docker image](https://hub.docker.com/_/zookeeper/tags)
 *
 * @param imageName      Docker image name ([DockerImageName])
 * @param useDefaultPort Default port 를 사용할지 여부
 * @param reuse          재사용 여부
 */
class ZooKeeperServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean = false,
    reuse: Boolean = true,
): GenericContainer<ZooKeeperServer>(imageName), GenericServer {

    companion object: KLogging() {
        const val IMAGE = "zookeeper"
        const val TAG = "3.9"
        const val NAME = "zookeeper"
        const val PORT = 2181

        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): ZooKeeperServer {
            return ZooKeeperServer(imageName, useDefaultPort, reuse)
        }

        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): ZooKeeperServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(IMAGE).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(PORT)
    override val url: String get() = "$host:$port"

    init {
        withExposedPorts(PORT)
        withReuse(reuse)
        waitingFor(Wait.forListeningPort())

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties(NAME)
    }

    object Launcher {
        val zookeeper: ZooKeeperServer by lazy {
            ZooKeeperServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }

        fun getCuratorFramework(zookeeper: ZooKeeperServer): CuratorFramework {
            return curatorFrameworkOf {
                connectString(zookeeper.url)
                retryPolicy(RetryOneTime(100))
                connectionTimeoutMs(3000)
            }
        }
    }
}
