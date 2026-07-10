package io.bluetape4k.testcontainers.graphdb

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.containers.wait.strategy.WaitAllStrategy
import org.testcontainers.containers.wait.strategy.WaitAllStrategy.Mode.WITH_OUTER_TIMEOUT
import org.testcontainers.utility.DockerImageName
import java.time.Duration

/**
 * [FalkorDB](https://www.falkordb.com/) 그래프 데이터베이스를 Testcontainers로 실행합니다.
 *
 * FalkorDB는 Redis 와이어 프로토콜을 통해 접속하는 그래프 데이터베이스입니다.
 * `jfalkordb` 드라이버를 사용하여 Cypher 쿼리를 실행할 수 있습니다.
 *
 * 참고: [FalkorDB Docker Hub](https://hub.docker.com/r/falkordb/falkordb)
 *
 * ```kotlin
 * val falkordb = FalkorDBServer().apply { start() }
 * val driver = FalkorDB.driver(falkordb.host, falkordb.port)
 * driver.graph("myGraph").use { graph ->
 *     graph.query("CREATE (:Person {name: 'Alice'})")
 * }
 * ```
 *
 * **주의**: [useDefaultPort]를 `true`로 설정하면 호스트의 6379 포트를 점유하므로
 * 다른 RedisServer 인스턴스와 포트 충돌이 발생할 수 있습니다.
 *
 * @param imageName      Docker 이미지 이름 ([DockerImageName])
 * @param useDefaultPort 기본 포트(6379)를 호스트 포트로 고정할지 여부
 * @param reuse          컨테이너 재사용 여부
 */
class FalkorDBServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean = false,
    reuse: Boolean = false,
): GenericContainer<FalkorDBServer>(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        /** FalkorDB 공식 Docker 이미지 이름 */
        const val IMAGE = "falkordb/falkordb"

        /** 기본으로 사용하는 FalkorDB 이미지 태그 */
        const val TAG = "v4.18.1"

        /** 시스템 프로퍼티 접두사에 사용되는 서버 이름 */
        const val NAME = "falkordb"

        /** Redis 와이어 프로토콜 기본 포트 */
        const val REDIS_PORT = 6379

        /** CI 콜드 스타트를 포함한 컨테이너 시작 최대 대기 시간 */
        private val START_TIMEOUT = Duration.ofSeconds(120)

        /** 컨테이너 준비 완료를 판단하는 로그 정규식 */
        private const val READY_LOG_REGEX = ".*Ready to accept connections.*"

        /**
         * [DockerImageName]을 직접 지정하여 [FalkorDBServer] 인스턴스를 생성합니다.
         *
         * @param imageName      Docker 이미지 이름
         * @param useDefaultPort 기본 포트를 호스트에 고정할지 여부
         * @param reuse          컨테이너 재사용 여부
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = false,
        ): FalkorDBServer {
            return FalkorDBServer(imageName, useDefaultPort, reuse)
        }

        /**
         * 이미지명과 태그를 문자열로 지정하여 [FalkorDBServer] 인스턴스를 생성합니다.
         *
         * @param image          Docker 이미지 이름 (기본값: [IMAGE])
         * @param tag            Docker 이미지 태그 (기본값: [TAG])
         * @param useDefaultPort 기본 포트를 호스트에 고정할지 여부
         * @param reuse          컨테이너 재사용 여부
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = false,
        ): FalkorDBServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            val imageName = DockerImageName.parse(image).withTag(tag)
            return FalkorDBServer(imageName, useDefaultPort, reuse)
        }
    }

    /** 호스트에 매핑된 Redis 포트 번호 */
    override val port: Int get() = getMappedPort(REDIS_PORT)

    /** Redis 프로토콜 URL (`redis://host:port` 형식) */
    override val url: String get() = "redis://$host:$port"

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf("host", "port", "url")

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
    )

    init {
        addExposedPorts(REDIS_PORT)
        withReuse(reuse)
        waitingFor(
            WaitAllStrategy(WITH_OUTER_TIMEOUT)
                .withStrategy(Wait.forLogMessage(READY_LOG_REGEX, 1))
                .withStrategy(Wait.forListeningPort())
                .withStartupTimeout(START_TIMEOUT)
        )

        if (useDefaultPort) {
            exposeCustomPorts(REDIS_PORT)
        }
    }

    /**
     * FalkorDB 서버를 시작하고 시스템 프로퍼티에 연결 정보를 등록합니다.
     *
     * 등록되는 시스템 프로퍼티:
     * - `testcontainers.falkordb.host`
     * - `testcontainers.falkordb.port`
     * - `testcontainers.falkordb.url`
     */
    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    /**
     * 테스트에서 재사용할 FalkorDB 서버 싱글턴을 제공합니다.
     *
     * ```kotlin
     * val falkordb = FalkorDBServer.Launcher.falkordb
     * FalkorDB.driver(falkordb.host, falkordb.port).use { driver ->
     *     driver.graph("myGraph").use { graph ->
     *         graph.query("CREATE (:Person {name: 'Alice'})")
     *     }
     * }
     * ```
     */
    object Launcher {
        /**
         * 기본 설정으로 시작된 [FalkorDBServer] 싱글턴 인스턴스입니다.
         * JVM 종료 시 자동으로 정지됩니다.
         */
        val falkordb: FalkorDBServer by lazy {
            FalkorDBServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}
