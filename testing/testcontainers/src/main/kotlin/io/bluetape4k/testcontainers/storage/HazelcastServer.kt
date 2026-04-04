package io.bluetape4k.testcontainers.storage

import com.hazelcast.config.rest.RestConfig
import com.hazelcast.spi.properties.HazelcastProperty
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

/**
 * [Hazelcast](https://hazelcast.com/) server container.
 *
 * 참고: [Hazelcast Docker image](https://hub.docker.com/r/hazelcast/hazelcast/tags)
 *
 * ```
 * val hazelcast = HazelcastServer().apply { start() }
 * ```
 *
 * @param imageName      Docker image name ([DockerImageName])
 * @param useDefaultPort Default port 를 사용할지 여부
 * @param reuse          재사용 여부
 */
class HazelcastServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<HazelcastServer>(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        const val IMAGE = "hazelcast/hazelcast"
        const val TAG = "5-slim"
        const val NAME = "hazelcast"
        const val PORT = 5701

        /**
         * [DockerImageName]으로 [HazelcastServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val image = DockerImageName.parse("hazelcast/hazelcast").withTag("5-slim")
         * val server = HazelcastServer(image)
         * // server.isRunning == false
         * ```
         *
         * @param imageName Docker 이미지 이름
         * @param useDefaultPort `true`면 5701 포트를 고정 바인딩합니다.
         * @param reuse 컨테이너 재사용 여부입니다.
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): HazelcastServer {
            return HazelcastServer(imageName, useDefaultPort, reuse)
        }

        /**
         * 이미지 이름/태그로 [HazelcastServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val server = HazelcastServer(image = "hazelcast/hazelcast", tag = "5-slim")
         * // server.url.contains(":5701") || server.port > 0
         * ```
         *
         * @param image Docker 이미지 이름, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param tag   Docker 이미지 태그, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param useDefaultPort `true`면 5701 포트를 고정 바인딩합니다.
         * @param reuse 컨테이너 재사용 여부입니다.
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): HazelcastServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return HazelcastServer(imageName, useDefaultPort, reuse)
        }
    }

    private val enabledFeatures = HashSet<HazelcastProperty>()
    private val customProperties = HashSet<String>()
    private lateinit var config: RestConfig

    override val port: Int get() = getMappedPort(PORT)

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

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    /**
     * HTTP health-check 관련 JVM 속성 활성화를 위한 예약 설정입니다.
     *
     * ## 동작/계약
     * - 현재 버전에서는 TODO로 비활성화되어 실제 설정 변경은 없습니다.
     * - fluent API 체이닝을 위해 자기 자신을 반환합니다.
     */
    fun withHttpHealthCheck() = apply {
        // TODO: deprecated feature 수정 필요
        // enabledFeatures.add(GroupProperty.HTTP_HEALTHCHECK_ENABLED)
    }

    /**
     * REST client 관련 JVM 속성 활성화를 위한 예약 설정입니다.
     *
     * ## 동작/계약
     * - 현재 버전에서는 TODO로 비활성화되어 실제 설정 변경은 없습니다.
     * - fluent API 체이닝을 위해 자기 자신을 반환합니다.
     */
    fun withRESTClient() = apply {
        // TODO: deprecated feature 수정 필요
        // enabledFeatures.add(GroupProperty.REST_CLIENT_ENABLED)
    }

    /**
     * REST API 구성을 저장합니다.
     */
    fun withRestApi(config: RestConfig) {
        this.config = config
    }

    /**
     * Hazelcast JVM 옵션으로 전달할 커스텀 시스템 프로퍼티를 추가합니다.
     */
    fun withCustomProperty(property: String) = apply {
        customProperties.add(property)
    }

    override fun configure() {
        super.configure()

        val javaOpts = enabledFeatures.joinToString(" ") { "-D${it.name}=true" }
        val customProps = customProperties.joinToString(" ") { "-D$it" }

        log.debug { "javaOpts=$javaOpts" }
        withEnv("JAVA_OPTS", "$javaOpts $customProps")
    }

    /**
     * Hazelcast REST API의 기본 URL을 반환합니다.
     *
     * ```kotlin
     * val server = HazelcastServer()
     * val restUrl = server.getRestBaseUrl()
     * // restUrl == "http://${server.host}:${server.port}/hazelcast/rest"
     * ```
     */
    fun getRestBaseUrl(): String = "$url/hazelcast/rest"

    /**
     * 테스트에서 재사용할 Hazelcast 서버 싱글턴을 제공합니다.
     */
    object Launcher {
        val hazelcast: HazelcastServer by lazy {
            HazelcastServer()
                .withRESTClient()
                .withHttpHealthCheck()
                .apply {
                    start()
                    ShutdownQueue.register(this)
                }
        }
    }
}
