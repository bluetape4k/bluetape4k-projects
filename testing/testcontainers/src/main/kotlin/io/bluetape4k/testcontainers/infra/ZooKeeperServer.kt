package io.bluetape4k.testcontainers.infra

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
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
): GenericContainer<ZooKeeperServer>(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        /** ZooKeeper 서버의 Docker Hub 이미지 이름입니다. */
        const val IMAGE = "zookeeper"

        /** 기본 Docker 이미지 태그입니다. */
        const val TAG = "3.9"

        /** 시스템 프로퍼티 등록 시 사용하는 서버 식별자입니다. */
        const val NAME = "zookeeper"

        /** ZooKeeper 클라이언트 연결 포트입니다. */
        const val PORT = 2181

        /**
         * [DockerImageName]으로 [ZooKeeperServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val image = DockerImageName.parse("zookeeper").withTag("3.9")
         * val server = ZooKeeperServer(image)
         * // server.isRunning == false
         * ```
         *
         * @param imageName      Docker 이미지 이름
         * @param useDefaultPort `true`면 2181 포트를 고정 바인딩합니다.
         * @param reuse          컨테이너 재사용 여부입니다.
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): ZooKeeperServer {
            return ZooKeeperServer(imageName, useDefaultPort, reuse)
        }

        /**
         * 이미지 이름/태그로 [ZooKeeperServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val server = ZooKeeperServer(image = "zookeeper", tag = "3.9")
         * // server.url.contains(":2181") == true (시작 후)
         * ```
         *
         * @param image          Docker 이미지 이름, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param tag            Docker 이미지 태그, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param useDefaultPort `true`면 2181 포트를 고정 바인딩합니다.
         * @param reuse          컨테이너 재사용 여부입니다.
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): ZooKeeperServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(PORT)
    override val url: String get() = "$host:$port"

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf("host", "port", "url")

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
    )

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
        writeToSystemProperties()
    }

    /**
     * 테스트에서 재사용할 ZooKeeper 서버 싱글턴과 Curator 헬퍼를 제공합니다.
     */
    object Launcher {
        val zookeeper: ZooKeeperServer by lazy {
            ZooKeeperServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }

        /**
         * [ZooKeeperServer] 접속용 [CuratorFramework] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val curator = ZooKeeperServer.Launcher.getCuratorFramework(ZooKeeperServer.Launcher.zookeeper)
         * curator.start()
         * // curator.state == CuratorFramework.State.STARTED
         * ```
         *
         * @param zookeeper 연결할 [ZooKeeperServer] 인스턴스
         * @return [CuratorFramework] 인스턴스
         */
        fun getCuratorFramework(zookeeper: ZooKeeperServer): CuratorFramework {
            return curatorFrameworkOf {
                connectString(zookeeper.url)
                retryPolicy(RetryOneTime(1000))
                connectionTimeoutMs(10_000)
            }
        }
    }
}
