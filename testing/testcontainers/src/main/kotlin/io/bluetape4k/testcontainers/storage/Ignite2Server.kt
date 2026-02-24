package io.bluetape4k.testcontainers.storage

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.time.Duration

/**
 * [Apache Ignite 2.x](https://ignite.apache.org/) 서버 컨테이너입니다.
 *
 * 테스트 환경에서 Ignite 2.x 서버를 Docker로 실행하여 씬 클라이언트(기본 포트 `10800`)로 연결할 수 있습니다.
 *
 * Docker Hub: [apacheignite/ignite](https://hub.docker.com/r/apacheignite/ignite/tags)
 *
 * **사용 예시:**
 * ```kotlin
 * val ignite2 = Ignite2Server().apply { start() }
 * val clientAddress = ignite2.url
 * ```
 *
 * @param imageName Docker 이미지 이름 ([DockerImageName])
 * @param useDefaultPort 기본 포트(10800)를 그대로 사용할지 여부. `false`이면 임의 포트가 할당됩니다.
 * @param reuse 컨테이너 재사용 여부
 */
class Ignite2Server private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<Ignite2Server>(imageName), GenericServer {

    companion object: KLogging() {
        /** Apache Ignite 2.x Docker Hub 이미지 이름 */
        const val IMAGE = "apacheignite/ignite"

        /** 기본 태그 (안정 버전) */
        const val TAG = "2.17.0"

        /** 시스템 프로퍼티 등록 시 사용하는 서버 이름 */
        const val NAME = "ignite2"

        /** Ignite 2.x 씬 클라이언트 기본 포트 */
        const val PORT = 10800

        /**
         * [DockerImageName]으로 [Ignite2Server]를 생성합니다.
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): Ignite2Server = Ignite2Server(imageName, useDefaultPort, reuse)

        /**
         * 이미지 이름과 태그로 [Ignite2Server]를 생성합니다.
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): Ignite2Server {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            val imageName = DockerImageName.parse(image).withTag(tag)
            return Ignite2Server(imageName, useDefaultPort, reuse)
        }
    }

    /** 씬 클라이언트 연결 포트 (매핑된 포트) */
    override val port: Int get() = getMappedPort(PORT)

    /** 씬 클라이언트 연결 주소 (`host:port` 형식) */
    override val url: String get() = "$host:$port"

    init {
        addExposedPorts(PORT)
        withReuse(reuse)
        // Ignite 2.x 노드가 완전히 초기화될 때까지 로그 메시지로 대기
        waitingFor(
            Wait.forLogMessage(".*Ignite node started OK.*", 1)
                .withStartupTimeout(Duration.ofMinutes(2))
        )

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties(NAME)
    }

    /**
     * 테스트 환경에서 공유하는 싱글턴 [Ignite2Server] 인스턴스를 제공합니다.
     */
    object Launcher {
        val ignite2: Ignite2Server by lazy {
            Ignite2Server().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}
