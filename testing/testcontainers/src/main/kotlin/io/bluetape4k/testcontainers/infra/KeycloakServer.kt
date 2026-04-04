package io.bluetape4k.testcontainers.infra

import dasniko.testcontainers.keycloak.KeycloakContainer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.utility.DockerImageName

/**
 * [Keycloak](https://www.keycloak.org/)을 testcontainers를 이용하여 실행합니다.
 *
 * Keycloak 17+ (Quarkus 기반)부터는 context path가 `/auth`가 아닌 `/`입니다.
 *
 * 참고:
 * - [Keycloak docker image](https://quay.io/repository/keycloak/keycloak)
 * - [testcontainers-keycloak](https://github.com/dasniko/testcontainers-keycloak)
 */
class KeycloakServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean = false,
    reuse: Boolean = true,
): KeycloakContainer(imageName.toString()), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        const val IMAGE = "quay.io/keycloak/keycloak"
        const val TAG = "26.2"
        const val NAME = "keycloak"
        const val PORT = 8080

        /**
         * [DockerImageName]으로 [KeycloakServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val image = DockerImageName.parse("quay.io/keycloak/keycloak").withTag("26.2")
         * val server = KeycloakServer(image)
         * // server.isRunning == false
         * ```
         *
         * @param imageName      Docker 이미지 이름
         * @param useDefaultPort `true`면 8080 포트를 고정 바인딩합니다.
         * @param reuse          컨테이너 재사용 여부입니다.
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): KeycloakServer {
            return KeycloakServer(imageName, useDefaultPort, reuse)
        }

        /**
         * 이미지 이름/태그로 [KeycloakServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val server = KeycloakServer(image = "quay.io/keycloak/keycloak", tag = "26.2")
         * // server.url.startsWith("http://") == true (시작 후)
         * ```
         *
         * @param image          Docker 이미지 이름, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param tag            Docker 이미지 태그, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param useDefaultPort `true`면 8080 포트를 고정 바인딩합니다.
         * @param reuse          컨테이너 재사용 여부입니다.
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): KeycloakServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return KeycloakServer(imageName, useDefaultPort, reuse)
        }
    }

    /** 컨테이너의 매핑된 HTTP 포트를 반환합니다. */
    override val port: Int get() = getMappedPort(PORT)

    /** Keycloak 서버의 기본 URL (예: `http://localhost:32768`)을 반환합니다. */
    override val url: String get() = "http://$host:$port"

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> =
        setOf("host", "port", "url", "auth-url", "admin-username", "admin-password")

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
        "auth-url" to getAuthServerUrl(),
        "admin-username" to getAdminUsername(),
        "admin-password" to getAdminPassword(),
    )

    /**
     * Keycloak 인증 서버 URL을 반환합니다.
     *
     * Keycloak 17+ (Quarkus 기반)에서는 context path가 `/`이며, `/auth`가 아닙니다.
     */
    // Note: Don't create val properties that delegate to parent getters to avoid JVM signature conflicts
    // Use the parent methods directly when needed

    init {
        withReuse(reuse)
    }

    override fun start() {
        super.start()

        writeToSystemProperties()
    }

    /**
     * 테스트에서 재사용할 Keycloak 서버 싱글턴을 제공합니다.
     */
    object Launcher {
        val keycloak: KeycloakServer by lazy {
            KeycloakServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}
