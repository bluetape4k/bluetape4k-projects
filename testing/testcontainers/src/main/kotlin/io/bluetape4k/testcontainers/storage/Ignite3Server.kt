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
 * [Apache Ignite 3.x](https://ignite.apache.org/) 서버 컨테이너입니다.
 *
 * Ignite 3.x는 씬 클라이언트 전용으로, 외부 서버에 연결하여 사용합니다.
 * 이 컨테이너는 테스트 환경에서 Ignite 3.x 서버를 Docker로 실행합니다.
 *
 * Docker Hub: [apacheignite/ignite3](https://hub.docker.com/r/apacheignite/ignite3/tags)
 *
 * **사용 예시:**
 * ```kotlin
 * val ignite3 = Ignite3Server().apply { start() }
 * val client = IgniteClient.builder()
 *     .addresses("${ignite3.host}:${ignite3.port}")
 *     .build()
 * ```
 *
 * 또는 싱글턴 [Launcher]를 통해 사용:
 * ```kotlin
 * val client = IgniteClient.builder()
 *     .addresses(Ignite3Server.Launcher.ignite3.url)
 *     .build()
 * ```
 *
 * @param imageName Docker 이미지 이름 ([DockerImageName])
 * @param useDefaultPort 기본 포트(10800)를 그대로 사용할지 여부. `false`이면 임의 포트가 할당됩니다.
 * @param reuse 컨테이너 재사용 여부
 */
class Ignite3Server private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<Ignite3Server>(imageName), GenericServer {

    companion object: KLogging() {
        /** Apache Ignite 3.x Docker Hub 이미지 이름 */
        const val IMAGE = "apacheignite/ignite3"

        /** 기본 태그 (안정 버전) */
        const val TAG = "3.1.0"

        /** 시스템 프로퍼티 등록 시 사용하는 서버 이름 */
        const val NAME = "ignite3"

        /** Ignite 3.x 씬 클라이언트 기본 포트 */
        const val CLIENT_PORT = 10800

        /** Ignite 3.x REST API 기본 포트 */
        const val REST_PORT = 10300

        /**
         * [DockerImageName]으로 [Ignite3Server]를 생성합니다.
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): Ignite3Server = Ignite3Server(imageName, useDefaultPort, reuse)

        /**
         * 이미지 이름과 태그로 [Ignite3Server]를 생성합니다.
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): Ignite3Server {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            val imageName = DockerImageName.parse(image).withTag(tag)
            return Ignite3Server(imageName, useDefaultPort, reuse)
        }
    }

    /** 씬 클라이언트 연결 포트 (매핑된 포트) */
    override val port: Int get() = getMappedPort(CLIENT_PORT)

    /** REST API 포트 (매핑된 포트) */
    val restPort: Int get() = getMappedPort(REST_PORT)

    /** 씬 클라이언트 연결 주소 (`host:port` 형식) */
    override val url: String get() = "$host:$port"

    init {
        addExposedPorts(CLIENT_PORT, REST_PORT)
        withReuse(reuse)

        // Ignite 3.x 노드가 완전히 초기화될 때까지 로그 메시지로 대기
        waitingFor(
            Wait.forLogMessage(".*Components started.*", 1)
                .withStartupTimeout(Duration.ofMinutes(2))
        )

        if (useDefaultPort) {
            exposeCustomPorts(CLIENT_PORT, REST_PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties(NAME, mapOf("rest.port" to restPort))
    }

    /**
     * 테스트 환경에서 공유하는 싱글턴 [Ignite3Server] 인스턴스를 제공합니다.
     */
    object Launcher {
        val ignite3: Ignite3Server by lazy {
            Ignite3Server().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}
