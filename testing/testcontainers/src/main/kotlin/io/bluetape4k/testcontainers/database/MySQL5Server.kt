package io.bluetape4k.testcontainers.database

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

/**
 * Docker를 이용하여 MySQL 5.7 서버를 실행합니다.
 *
 * * 참고: [MySQL 5.x Docker Image](https://hub.docker.com/_/mysql/tags?page=&page_size=&ordering=&name=5.)
 * * 참고: [MySQL 5.7 Does Not Have an Official Docker Image on ARM/M1 Mac](https://betterprogramming.pub/mysql-5-7-does-not-have-an-official-docker-image-on-arm-m1-mac-e55cbe093d4c)
 *
 * @constructor
 *
 * @param imageName         Docker image name
 * @param useDefaultPort    default port 사용 여부
 * @param reuse             Container 재사용 여부
 * @param username          Database 사용자 이름
 * @param password          Database 사용자 비밀번호
 * @param configuration     설정
 */
class MySQL5Server private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
    username: String,
    password: String,
    configuration: String,
): MySQLContainer<MySQL5Server>(imageName), JdbcServer {

    companion object: KLogging() {
        const val IMAGE = "biarms/mysql" // "mysql"
        const val TAG = "5" // "5.7"      // https://hub.docker.com/_/mysql/tags?page=&page_size=&ordering=&name=5.
        const val NAME = "mysql"
        const val PORT: Int = 3306
        const val USERNAME = "test"
        const val PASSWORD = "test"
        const val DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver"

        /**
         * [MySQL5Server]를 생성합니다.
         *
         * @param image             docker image (기본: `mysql`)
         * @param tag               docker image tag (기본: `5.7`)
         * @param useDefaultPort    기본 포트를 사용할지 여부 (기본: `true`)
         * @param reuse             재사용 여부 (기본: `true`)
         * @param username          사용자 이름 (기본: `test`)
         * @param password          비밀번호 (기본: `test`)
         * @param configuration      설정 (기본: `""`)
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
        ): MySQL5Server {
            val imageName = DockerImageName.parse(image)
                .withTag(tag)
                .asCompatibleSubstituteFor("mysql")
            
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
        ): MySQL5Server {
            return MySQL5Server(imageName, useDefaultPort, reuse, username, password, configuration)
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
        setWaitStrategy(Wait.forListeningPort())

        withCreateContainerCmdModifier { cmd ->
            cmd.withPlatform("linux/arm64")  // for Apple Silicon
        }

        // 로컬 테스트용이므로, 비밀번호가 없어도 실행할 수 있도록 한다
        withEnv("ALLOW_EMPTY_PASSWORD", "yes")

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties(NAME, buildJdbcProperties())
    }

    object Launcher {
        val mysql: MySQL5Server by lazy {
            MySQL5Server().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}
