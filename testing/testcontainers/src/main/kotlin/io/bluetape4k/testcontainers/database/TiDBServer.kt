package io.bluetape4k.testcontainers.database

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.tidb.TiDBContainer
import org.testcontainers.utility.DockerImageName

/**
 * 분산형 RDBMS인 PingCAP 의 [TiDB](https://www.pingcap.com/tidb/) 테스트용으로 사용할 수 있는 컨테이너를 제공한다.
 * MySQL과 호환됩니다.
 *
 * - 참고: [TiDB](https://www.pingcap.com/tidb/)
 * - 참고: [TiDB Docker](https://hub.docker.com/r/pingcap/tidb)
 */
class TiDBServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): TiDBContainer(imageName), JdbcServer {

    companion object: KLogging() {
        const val IMAGE = "pingcap/tidb"
        const val TAG = "v8.3.0"
        const val NAME = "tidb"

        const val TIDB_PORT = 4000
        const val REST_API_PORT = 10080

        const val DEFAULT_USERNAME = "root"
        const val DEFAULT_PASSWORD = ""
        const val DEFAULT_DATABASE_NAME = "test"

        const val DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver"

        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): TiDBServer {
            return TiDBServer(imageName, useDefaultPort, reuse)
        }

        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): TiDBServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(TIDB_PORT)
    override val url: String get() = super.getJdbcUrl()
    override fun getDriverClassName(): String = DRIVER_CLASS_NAME

    val tidbPort: Int get() = port
    val restApiPort: Int get() = getMappedPort(REST_API_PORT)

    init {
        addExposedPorts(TIDB_PORT, REST_API_PORT)

        // NOTE: 현 버전에서는 username, password, database name을 설정을 지원하지 않습니다.
        withReuse(reuse)

        if (useDefaultPort) {
            exposeCustomPorts(TIDB_PORT, REST_API_PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties(NAME, buildJdbcProperties())
    }

    object Launcher {
        val tidb: TiDBServer by lazy {
            TiDBServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}
