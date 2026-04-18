package io.bluetape4k.testcontainers.graphdb

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.graphdb.MemgraphServer.Companion.IMAGE
import io.bluetape4k.testcontainers.graphdb.MemgraphServer.Companion.TAG
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

/**
 * [Memgraph](https://memgraph.com/) 그래프 데이터베이스를 Testcontainers로 실행합니다.
 *
 * Memgraph는 Cypher 쿼리 언어를 지원하는 인메모리 그래프 데이터베이스로,
 * Neo4j Java Driver를 통해 Bolt 프로토콜로 접속할 수 있습니다.
 *
 * 참고: [Memgraph Docker Hub](https://hub.docker.com/r/memgraph/memgraph)
 *
 * ```kotlin
 * val memgraph = MemgraphServer().apply { start() }
 * val driver = GraphDatabase.driver(memgraph.boltUrl, AuthTokens.none())
 * driver.session().use { session ->
 *     val result = session.run("RETURN 1 AS n")
 *     println(result.single()["n"].asInt())
 * }
 * ```
 *
 * @param imageName      Docker 이미지 이름 ([DockerImageName])
 * @param useDefaultPort 기본 포트를 호스트 포트로 고정할지 여부
 * @param reuse          컨테이너 재사용 여부
 */
class MemgraphServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean = false,
    reuse: Boolean = true,
): GenericContainer<MemgraphServer>(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        /** Memgraph 공식 Docker 이미지 이름 */
        const val IMAGE = "memgraph/memgraph"

        /** 기본으로 사용하는 Memgraph 이미지 태그 — Memgraph 3.9.0 최신 안정 버전 */
        const val TAG = "3.9.0"

        /** 시스템 프로퍼티 접두사에 사용되는 서버 이름 */
        const val NAME = "memgraph"

        /** Bolt 프로토콜 기본 포트 */
        const val BOLT_PORT = 7687

        /** Memgraph 로그 서버 포트 */
        const val LOG_PORT = 7444

        /**
         * [DockerImageName]을 직접 지정하여 [MemgraphServer] 인스턴스를 생성합니다.
         *
         * @param imageName      Docker 이미지 이름
         * @param useDefaultPort 기본 포트를 호스트에 고정할지 여부
         * @param reuse          컨테이너 재사용 여부
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): MemgraphServer {
            return MemgraphServer(imageName, useDefaultPort, reuse)
        }

        /**
         * 이미지명과 태그를 문자열로 지정하여 [MemgraphServer] 인스턴스를 생성합니다.
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
            reuse: Boolean = true,
        ): MemgraphServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            val imageName = DockerImageName.parse(image).withTag(tag)
            return MemgraphServer(imageName, useDefaultPort, reuse)
        }
    }

    /** 호스트에 매핑된 Bolt 포트 번호 */
    override val port: Int get() = getMappedPort(BOLT_PORT)

    /** Bolt 프로토콜 URL (`bolt://host:port` 형식) */
    override val url: String get() = "bolt://$host:$port"

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf("host", "port", "url", "bolt-port", "log-port", "bolt-url")

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
        "bolt-port" to getMappedPort(BOLT_PORT).toString(),
        "log-port" to getMappedPort(LOG_PORT).toString(),
        "bolt-url" to boltUrl,
    )

    /**
     * Neo4j Java Driver 등에서 사용할 수 있는 Bolt 연결 URL입니다.
     *
     * ```kotlin
     * val driver = GraphDatabase.driver(memgraph.boltUrl, AuthTokens.none())
     * ```
     */
    val boltUrl: String get() = "bolt://$host:${getMappedPort(BOLT_PORT)}"

    init {
        addExposedPorts(BOLT_PORT, LOG_PORT)
        withReuse(reuse)
        addEnv("MEMGRAPH", "--telemetry-enabled=false")
        withCommand("--telemetry-enabled=false")
        waitingFor(Wait.forListeningPort())

        if (useDefaultPort) {
            exposeCustomPorts(BOLT_PORT, LOG_PORT)
        }
    }

    /**
     * Memgraph 서버를 시작하고 시스템 프로퍼티에 연결 정보를 등록합니다.
     *
     * 등록되는 시스템 프로퍼티:
     * - `testcontainers.memgraph.host`
     * - `testcontainers.memgraph.port`
     * - `testcontainers.memgraph.url`
     * - `testcontainers.memgraph.bolt-port`
     * - `testcontainers.memgraph.log-port`
     * - `testcontainers.memgraph.bolt-url`
     */
    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    /**
     * 테스트에서 재사용할 Memgraph 서버 싱글턴을 제공합니다.
     *
     * ```kotlin
     * val driver = GraphDatabase.driver(
     *     MemgraphServer.Launcher.memgraph.boltUrl,
     *     AuthTokens.none()
     * )
     * ```
     */
    object Launcher {
        /**
         * 기본 설정으로 시작된 [MemgraphServer] 싱글턴 인스턴스입니다.
         * JVM 종료 시 자동으로 정지됩니다.
         */
        val memgraph: MemgraphServer by lazy {
            MemgraphServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}
