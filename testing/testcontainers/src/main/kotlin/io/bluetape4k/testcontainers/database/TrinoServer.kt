package io.bluetape4k.testcontainers.database

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.TrinoContainer
import org.testcontainers.utility.DockerImageName

/**
 * Docker를 이용하여 [Trino](https://trino.io/) 분산 SQL 쿼리 엔진 서버를 실행합니다.
 *
 * Trino는 대용량 데이터를 빠르게 처리하는 분산 SQL 쿼리 엔진으로,
 * 다양한 데이터 소스(Hive, TPCH, TPC-DS, Memory 등)에 연결할 수 있습니다.
 *
 * ```kotlin
 * val trino = TrinoServer()
 * trino.start()
 *
 * val conn = DriverManager.getConnection(
 *     "jdbc:trino://${trino.host}:${trino.port}/memory",
 *     "test",
 *     null
 * )
 * ```
 *
 * 참고: [Trino Docker Hub](https://hub.docker.com/r/trinodb/trino)
 *
 * @param imageName         Docker 이미지 이름
 * @param useDefaultPort    기본 포트 사용 여부
 * @param reuse             컨테이너 재사용 여부
 */
class TrinoServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): TrinoContainer(imageName), JdbcServer, PropertyExportingServer {

    companion object: KLogging() {
        const val IMAGE = "trinodb/trino"
        const val TAG = "475"
        const val NAME = "trino"
        const val PORT = 8080

        /**
         * [TrinoServer]를 생성합니다.
         *
         * @param image         Docker 이미지 이름 (기본: `trinodb/trino`)
         * @param tag           Docker 이미지 태그 (기본: `475`)
         * @param useDefaultPort 기본 포트를 사용할지 여부 (기본: `false`)
         * @param reuse         컨테이너 재사용 여부 (기본: `true`)
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): TrinoServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }

        /**
         * [TrinoServer]를 생성합니다.
         *
         * @param imageName     Docker 이미지 이름
         * @param useDefaultPort 기본 포트를 사용할지 여부 (기본: `false`)
         * @param reuse         컨테이너 재사용 여부 (기본: `true`)
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): TrinoServer {
            return TrinoServer(imageName, useDefaultPort, reuse)
        }
    }

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf(
        "jdbc.url", "username"
    )

    override fun properties(): Map<String, String> = buildMap {
        put("jdbc.url", "jdbc:trino://$host:$port/memory")
        username?.let { put("username", it) }
    }

    override val port: Int get() = getMappedPort(PORT)
    override val url: String get() = "http://$host:$port"

    init {
        addExposedPorts(PORT)
        withReuse(reuse)

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    /**
     * 테스트에서 재사용할 Trino 서버 싱글턴을 제공합니다.
     */
    object Launcher {
        val trino: TrinoServer by lazy {
            TrinoServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}
