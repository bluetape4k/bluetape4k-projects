package io.bluetape4k.testcontainers.database

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

/**
 * Docker를 이용하여 MariaDB 서버를 실행합니다.
 *
 * * 참고: [MariaDB Docker Image](https://hub.docker.com/_/mariadb/tags)
 *
 * @param imageName         Docker image name
 * @param useDefaultPort    default port 사용 여부
 * @param reuse             Container 재사용 여부
 * @param username          Database 사용자 이름
 * @param password          Database 사용자 비밀번호
 * @param configuration     설정
 */
class MariaDBServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
    username: String,
    password: String,
    configuration: String,
): MariaDBContainer<MariaDBServer>(imageName), JdbcServer {

    companion object: KLogging() {
        const val IMAGE = "mariadb"
        const val TAG = "11"
        const val NAME = "mariadb"
        const val PORT: Int = 3306
        const val USERNAME = "test"
        const val PASSWORD = "test"
        const val DRIVER_CLASS_NAME = "org.mariadb.jdbc.Driver"

        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
            username: String = USERNAME,
            password: String = PASSWORD,
            configuration: String = "",
        ): MariaDBServer {
            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse, username, password, configuration)
        }

        /**
         * [MySQL5Server]를 생성합니다.
         *
         * @param imageName         docker image name
         * @param useDefaultPort    기본 포트를 사용할지 여부 (기본: `true`)
         * @param reuse             재사용 여부 (기본: `true`)
         * @param username          사용자 이름 (기본: `test`)
         * @param password          비밀번호 (기본: `test`)
         * @param configuration      설정 (기본: `""`)
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
            username: String = USERNAME,
            password: String = PASSWORD,
            configuration: String = "",
        ): MariaDBServer {
            return MariaDBServer(imageName, useDefaultPort, reuse, username, password, configuration)
        }
    }

    override fun getDriverClassName(): String = DRIVER_CLASS_NAME
    override val port: Int get() = getMappedPort(PORT)
    override val url: String get() = jdbcUrl

    init {
        if (configuration.isNotBlank()) {
            withConfigurationOverride(configuration)
        }
        addExposedPorts(PORT)
        
        withUsername(username)
        withPassword(password)
        withReuse(reuse)

        // 로컬 테스트용이므로, 비밀번호가 없어도 실행할 수 있도록 한다
        withEnv("MYSQL_ALLOW_EMPTY_PASSWORD", "yes")

        withCreateContainerCmdModifier { cmd ->
            cmd.withPlatform("linux/arm64")  // for Apple Silicon
        }

        // For Debugging
        // withLogConsumer(Slf4jLogConsumer(log))

        setWaitStrategy(Wait.forListeningPort())

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties(NAME, buildJdbcProperties())
    }

    object Launcher {
        val mariadb: MariaDBServer by lazy {
            MariaDBServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}
