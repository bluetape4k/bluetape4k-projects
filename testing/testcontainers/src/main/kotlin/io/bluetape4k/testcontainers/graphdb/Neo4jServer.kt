package io.bluetape4k.testcontainers.graphdb

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.graphdb.Neo4jServer.Companion.IMAGE
import io.bluetape4k.testcontainers.graphdb.Neo4jServer.Companion.TAG
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.neo4j.Neo4jContainer
import org.testcontainers.utility.DockerImageName

/**
 * [Neo4j](https://neo4j.com/) 그래프 데이터베이스를 testcontainers로 실행합니다.
 *
 * 참고: [Neo4j Docker 이미지](https://hub.docker.com/_/neo4j)
 *
 * 기본적으로 인증 없이 시작되며, Bolt(7687) 및 HTTP(7474) 포트를 노출합니다.
 *
 * **사용 예시:**
 * ```kotlin
 * val driver = GraphDatabase.driver(Neo4jServer.Launcher.neo4j.boltUrl)
 * driver.verifyConnectivity()
 * ```
 */
class Neo4jServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean = false,
    reuse: Boolean = true,
): Neo4jContainer(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        /** Neo4j Docker 이미지 이름 */
        const val IMAGE = "neo4j"

        /** Neo4j Docker 이미지 태그 */
        const val TAG = "5"

        /** 시스템 프로퍼티 등록에 사용할 서버 이름 */
        const val NAME = "neo4j"

        /** Neo4j HTTP 포트 */
        const val HTTP_PORT = 7474

        /** Neo4j Bolt 포트 */
        const val BOLT_PORT = 7687

        /**
         * [DockerImageName]을 직접 지정하여 [Neo4jServer] 인스턴스를 생성합니다.
         *
         * @param imageName Docker 이미지 이름
         * @param useDefaultPort 고정 포트 바인딩 여부
         * @param reuse 컨테이너 재사용 여부
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): Neo4jServer {
            return Neo4jServer(imageName, useDefaultPort, reuse)
        }

        /**
         * 이미지 이름과 태그를 문자열로 지정하여 [Neo4jServer] 인스턴스를 생성합니다.
         *
         * @param image Docker 이미지 이름 (기본값: [IMAGE])
         * @param tag Docker 이미지 태그 (기본값: [TAG])
         * @param useDefaultPort 고정 포트 바인딩 여부
         * @param reuse 컨테이너 재사용 여부
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): Neo4jServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return Neo4jServer(imageName, useDefaultPort, reuse)
        }
    }

    /**
     * 매핑된 Bolt 포트를 반환합니다.
     */
    override val port: Int get() = getMappedPort(BOLT_PORT)

    /**
     * Bolt 프로토콜 URL을 반환합니다. (예: `bolt://localhost:7687`)
     */
    override val url: String get() = "bolt://$host:$port"

    /**
     * Neo4j Bolt 프로토콜 URL을 반환합니다.
     *
     * ```kotlin
     * val server = Neo4jServer()
     * val boltUrl = server.getBoltUrlString()
     * // boltUrl == "bolt://${server.host}:${server.getMappedPort(7687)}"
     * ```
     */
    fun getBoltUrlString(): String = "bolt://$host:${getMappedPort(BOLT_PORT)}"

    /**
     * Neo4j HTTP 프로토콜 URL을 반환합니다.
     *
     * ```kotlin
     * val server = Neo4jServer()
     * val httpUrl = server.getHttpUrlString()
     * // httpUrl == "http://${server.host}:${server.getMappedPort(7474)}"
     * ```
     */
    fun getHttpUrlString(): String = "http://$host:${getMappedPort(HTTP_PORT)}"

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> =
        setOf("host", "port", "url", "bolt-port", "http-port", "bolt-url", "http-url")

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
        "bolt-port" to getMappedPort(BOLT_PORT).toString(),
        "http-port" to getMappedPort(HTTP_PORT).toString(),
        "bolt-url" to boltUrl,
        "http-url" to httpUrl,
    )

    init {
        addExposedPorts(HTTP_PORT, BOLT_PORT)
        withoutAuthentication()
        withReuse(reuse)

        if (useDefaultPort) {
            exposeCustomPorts(HTTP_PORT, BOLT_PORT)
        }
    }

    /**
     * Neo4j 서버를 시작하고 시스템 프로퍼티에 접속 정보를 등록합니다.
     *
     * 등록되는 시스템 프로퍼티:
     * - `testcontainers.neo4j.host`
     * - `testcontainers.neo4j.port` (Bolt 포트)
     * - `testcontainers.neo4j.url` (Bolt URL)
     * - `testcontainers.neo4j.bolt-port`
     * - `testcontainers.neo4j.http-port`
     * - `testcontainers.neo4j.bolt-url`
     * - `testcontainers.neo4j.http-url`
     */
    override fun start() {
        super.start()

        writeToSystemProperties()
    }

    /**
     * 테스트 전반에서 재사용할 Neo4j 서버 싱글턴을 제공합니다.
     */
    object Launcher {
        /**
         * 싱글턴 Neo4j 서버 인스턴스.
         * JVM 종료 시 자동으로 컨테이너가 중지됩니다.
         */
        val neo4j: Neo4jServer by lazy {
            Neo4jServer().apply {
                start()
                ShutdownQueue.register(this as AutoCloseable)
            }
        }
    }
}
