package io.bluetape4k.testcontainers.storage

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

/**
 * [InfluxDB](https://www.influxdata.com/) 2.x를 Docker container로 실행해주는 클래스입니다.
 *
 * 참고: [InfluxDB Docker image](https://hub.docker.com/_/influxdb/tags)
 *
 * @param imageName        Docker image name ([DockerImageName])
 * @param useDefaultPort   Default port를 사용할지 여부
 * @param reuse            재사용 여부
 * @param organization     InfluxDB 조직 이름 (기본값: DEFAULT_ORG)
 * @param bucket           InfluxDB 버킷 이름 (기본값: DEFAULT_BUCKET)
 * @param adminToken       InfluxDB 관리자 토큰 (기본값: DEFAULT_ADMIN_TOKEN)
 * @param username         InfluxDB 사용자명 (기본값: DEFAULT_USERNAME)
 * @param password         InfluxDB 사용자 비밀번호 (기본값: DEFAULT_PASSWORD)
 */
class InfluxDBServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean = false,
    reuse: Boolean = true,
    val organization: String = DEFAULT_ORG,
    val bucket: String = DEFAULT_BUCKET,
    val adminToken: String = DEFAULT_ADMIN_TOKEN,
    val username: String = DEFAULT_USERNAME,
    val password: String = DEFAULT_PASSWORD,
): GenericContainer<InfluxDBServer>(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        const val IMAGE = "influxdb"
        const val TAG = "2.7"
        const val NAME = "influxdb"
        const val PORT = 8086

        const val DEFAULT_ORG = "bluetape4k"
        const val DEFAULT_BUCKET = "test-bucket"
        const val DEFAULT_ADMIN_TOKEN = "test-token"
        const val DEFAULT_USERNAME = "admin"
        const val DEFAULT_PASSWORD = "password"

        /**
         * 이미지 이름/태그로 [InfluxDBServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val server = InfluxDBServer(image = "influxdb", tag = "2.7")
         * // server.url.startsWith("http://") == true (시작 후)
         * ```
         *
         * @param image        Docker 이미지 이름, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param tag          Docker 이미지 태그, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param useDefaultPort `true`면 8086 포트를 고정 바인딩합니다.
         * @param reuse        컨테이너 재사용 여부입니다.
         * @param organization InfluxDB 조직 이름
         * @param bucket       InfluxDB 버킷 이름
         * @param adminToken   InfluxDB 관리자 토큰
         * @param username     InfluxDB 사용자명
         * @param password     InfluxDB 사용자 비밀번호
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
            organization: String = DEFAULT_ORG,
            bucket: String = DEFAULT_BUCKET,
            adminToken: String = DEFAULT_ADMIN_TOKEN,
            username: String = DEFAULT_USERNAME,
            password: String = DEFAULT_PASSWORD,
        ): InfluxDBServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(
                imageName,
                useDefaultPort,
                reuse,
                organization,
                bucket,
                adminToken,
                username,
                password,
            )
        }

        /**
         * [DockerImageName]으로 [InfluxDBServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val image = DockerImageName.parse("influxdb").withTag("2.7")
         * val server = InfluxDBServer(image)
         * // server.isRunning == false
         * ```
         *
         * @param imageName    Docker 이미지 이름
         * @param useDefaultPort `true`면 8086 포트를 고정 바인딩합니다.
         * @param reuse        컨테이너 재사용 여부입니다.
         * @param organization InfluxDB 조직 이름
         * @param bucket       InfluxDB 버킷 이름
         * @param adminToken   InfluxDB 관리자 토큰
         * @param username     InfluxDB 사용자명
         * @param password     InfluxDB 사용자 비밀번호
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
            organization: String = DEFAULT_ORG,
            bucket: String = DEFAULT_BUCKET,
            adminToken: String = DEFAULT_ADMIN_TOKEN,
            username: String = DEFAULT_USERNAME,
            password: String = DEFAULT_PASSWORD,
        ): InfluxDBServer {
            return InfluxDBServer(
                imageName,
                useDefaultPort,
                reuse,
                organization,
                bucket,
                adminToken,
                username,
                password,
            )
        }
    }

    override val port: Int get() = getMappedPort(PORT)

    /**
     * InfluxDB URL을 반환합니다.
     */
    override val url: String get() = "http://$host:$port"

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf(
        "host", "port", "url",
        "organization", "bucket", "admin-token", "username",
    )

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
        "organization" to organization,
        "bucket" to bucket,
        "admin-token" to adminToken,
        "username" to username,
    )

    init {
        withExposedPorts(PORT)
        withReuse(reuse)
        // InfluxDB 2.x 초기 설정 환경변수
        withEnv("DOCKER_INFLUXDB_INIT_MODE", "setup")
        withEnv("DOCKER_INFLUXDB_INIT_USERNAME", username)
        withEnv("DOCKER_INFLUXDB_INIT_PASSWORD", password)
        withEnv("DOCKER_INFLUXDB_INIT_ORG", organization)
        withEnv("DOCKER_INFLUXDB_INIT_BUCKET", bucket)
        withEnv("DOCKER_INFLUXDB_INIT_ADMIN_TOKEN", adminToken)

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    /**
     * 테스트에서 재사용할 InfluxDB 서버 싱글턴을 제공합니다.
     */
    object Launcher {
        val influxDB: InfluxDBServer by lazy {
            InfluxDBServer().apply {
                start()
                ShutdownQueue.register(this as AutoCloseable)
            }
        }
    }
}
