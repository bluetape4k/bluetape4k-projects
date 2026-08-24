package io.bluetape4k.testcontainers.storage

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
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
 * Ignite2Server().use { ignite2 ->
 *     ignite2.start()
 *     Ignition.startClient(ClientConfiguration().setAddresses(ignite2.url)).use { client ->
 *         val cache = client.getOrCreateCache<String, String>("example-cache")
 *         cache.put("key", "value")
 *         check(cache.get("key") == "value")
 *     }
 * }
 * ```
 *
 * canonical 이미지는 `x86_64`/`amd64`에서 `2.18.0`, `aarch64`/`arm64`에서
 * `2.18.0-arm64`로 지연 해석됩니다. canonical tag를 생략한 상태에서 지원하지
 * 않는 아키텍처를 만나면 즉시 실패합니다. Custom image는 명시적인 tag가
 * 필요하며, 명시한 custom tag는 canonical resolver를 우회합니다.
 *
 * @param imageName Docker 이미지 이름 ([DockerImageName])
 * @param useDefaultPort 기본 포트(10800)를 그대로 사용할지 여부. `false`이면 임의 포트가 할당됩니다.
 * @param reuse 컨테이너 재사용 여부
 */
class Ignite2Server private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<Ignite2Server>(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        /** Apache Ignite 2.x Docker Hub 이미지 이름 */
        const val IMAGE = "apacheignite/ignite"

        /** 기본 태그 (안정 버전) */
        const val TAG = "2.18.0"

        /**
         * 현재 아키텍처에 맞는 기본 태그.
         * arm64(Apple Silicon 등)에서는 에뮬레이션 없이 실행하기 위해 `2.18.0-arm64` 태그를 사용합니다.
         * 지원하지 않는 아키텍처에서는 기본 생성자 경로를 조용히 다른 이미지로 완화하지 않습니다.
         */
        val DEFAULT_TAG: String
            get() = defaultTagForArchitecture(System.getProperty("os.arch"))

        private const val DEFAULT_TAG_SENTINEL = "__ignite2_default_tag__"
        private const val STARTUP_TIMEOUT_MINUTES = 5L

        private fun defaultTagForArchitecture(architecture: String?): String = when (architecture?.lowercase()) {
            "x86_64", "amd64" -> TAG
            "aarch64", "arm64" -> "$TAG-arm64"
            else -> error("Unsupported Ignite2 default image architecture: $architecture")
        }

        /** 시스템 프로퍼티 등록 시 사용하는 서버 이름 */
        const val NAME = "ignite2"

        /** Ignite 2.x 씬 클라이언트 기본 포트 */
        const val PORT = 10800

        /**
         * [DockerImageName]으로 [Ignite2Server]를 생성합니다.
         *
         * ```kotlin
         * val image = DockerImageName.parse("apacheignite/ignite").withTag(DEFAULT_TAG)
         * val server = Ignite2Server(image)
         * // server.isRunning == false
         * ```
         *
         * @param imageName      Docker 이미지 이름
         * @param useDefaultPort `true`면 10800 포트를 고정 바인딩합니다.
         * @param reuse          컨테이너 재사용 여부입니다.
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = false,
        ): Ignite2Server {
            val resolvedImageName = if (
                imageName.getUnversionedPart() == IMAGE && imageName.getVersionPart() == "latest"
            ) {
                imageName.withTag(DEFAULT_TAG)
            } else {
                require(imageName.getVersionPart() != "latest" || imageName.getUnversionedPart() == IMAGE) {
                    "Custom Ignite2 DockerImageName must include an explicit tag"
                }
                imageName
            }
            return Ignite2Server(resolvedImageName, useDefaultPort, reuse)
        }

        /**
         * 이미지 이름과 태그로 [Ignite2Server]를 생성합니다.
         *
         * ```kotlin
         * val server = Ignite2Server(image = "apacheignite/ignite", tag = DEFAULT_TAG)
         * // server.url.contains(":10800") == true (시작 후)
         * ```
         *
         * @param image          Docker 이미지 이름, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param tag            Docker 이미지 태그, blank이면 [IllegalArgumentException]이 발생합니다
         *                       (기본: [DEFAULT_TAG]; aarch64에서는 [TAG]-arm64).
         * @param useDefaultPort `true`면 10800 포트를 고정 바인딩합니다.
         * @param reuse          컨테이너 재사용 여부입니다.
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = DEFAULT_TAG_SENTINEL,
            useDefaultPort: Boolean = false,
            reuse: Boolean = false,
        ): Ignite2Server {
            image.requireNotBlank("image")
            val resolvedTag = if (tag == DEFAULT_TAG_SENTINEL) {
                require(image == IMAGE) {
                    "Custom Ignite2 image requires an explicit tag"
                }
                DEFAULT_TAG
            } else {
                tag.requireNotBlank("tag")
                tag
            }
            val imageName = DockerImageName.parse(image).withTag(resolvedTag)
            return Ignite2Server(imageName, useDefaultPort, reuse)
        }
    }

    /** 씬 클라이언트 연결 포트 (매핑된 포트) */
    override val port: Int get() = getMappedPort(PORT)

    /** 씬 클라이언트 연결 주소 (`host:port` 형식) */
    override val url: String get() = "$host:$port"

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf("host", "port", "url")

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
    )

    init {
        addExposedPorts(PORT)
        withReuse(reuse)
        // Ignite 2.x 노드가 완전히 초기화될 때까지 로그 메시지로 대기
        waitingFor(
            Wait.forLogMessage(".*Ignite node started OK.*", 1)
                .withStartupTimeout(Duration.ofMinutes(STARTUP_TIMEOUT_MINUTES))
        )

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties()
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
