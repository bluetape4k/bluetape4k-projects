package io.bluetape4k.testcontainers.database

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.mariadb.MariaDBContainer
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
): MariaDBContainer(imageName), JdbcServer, PropertyExportingServer {

    companion object: KLogging() {
        const val IMAGE = "mariadb"
        const val TAG = "12"
        const val NAME = "mariadb"
        const val PORT: Int = 3306
        const val USERNAME = "test"
        const val PASSWORD = "test"
        const val DRIVER_CLASS_NAME = "org.mariadb.jdbc.Driver"

        /**
         * [MariaDBServer]를 생성합니다.
         *
         * ```kotlin
         * val server = MariaDBServer(image = "mariadb", tag = "12")
         * // server.url.startsWith("jdbc:mariadb://") == true (시작 후)
         * ```
         *
         * @param image             docker image (기본: `mariadb`)
         * @param tag               docker image tag (기본: `12`)
         * @param useDefaultPort    기본 포트를 사용할지 여부 (기본: `false`)
         * @param reuse             재사용 여부 (기본: `true`)
         * @param username          사용자 이름 (기본: `test`)
         * @param password          비밀번호 (기본: `test`)
         * @param configuration     설정 (기본: `""`)
         */
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
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse, username, password, configuration)
        }

        /**
         * [MariaDBServer]를 생성합니다.
         *
         * ```kotlin
         * val image = DockerImageName.parse("mariadb").withTag("12")
         * val server = MariaDBServer(image)
         * // server.isRunning == false
         * ```
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

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf(
        "jdbc-url", "driver-class-name", "username", "password", "database-name",
    )

    override fun properties(): Map<String, String> = buildKebabJdbcProperties()

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
            val arch = System.getProperty("os.arch")
            val platform = if (arch == "aarch64") "linux/arm64" else "linux/amd64"
            cmd.withPlatform(platform)
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
        writeToSystemProperties()
    }

    /**
     * 테스트에서 재사용할 MariaDB 서버 싱글턴을 제공합니다.
     */
    object Launcher {
        val mariadb: MariaDBServer by lazy {
            MariaDBServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}
