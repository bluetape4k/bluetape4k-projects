package io.bluetape4k.testcontainers.mq

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.redpanda.RedpandaContainer
import org.testcontainers.utility.DockerImageName


/**
 * Redpanda 테스트 서버 컨테이너를 실행하고 브로커/관리 포트 정보를 제공합니다.
 *
 * ## 동작/계약
 * - Kafka 브로커, Admin API, Schema Registry, REST Proxy 포트를 모두 노출합니다.
 * - `useDefaultPort=true`이면 각 포트의 호스트 고정 바인딩을 시도합니다.
 * - 인스턴스 생성만으로는 시작되지 않으며 `start()` 호출 시 추가 시스템 프로퍼티를 기록합니다.
 *
 * ```kotlin
 * val server = RedpandaServer()
 * server.start()
 * // server.schemaRegistryPort > 0
 * ```
 *
 * - 참고: [Redpanda Official Site](https://redpanda.com/)
 * - 참고: [Redpanda Docker Hub](https://hub.docker.com/r/redpandadata/redpanda)
 */
class RedpandaServer private constructor(
    dockerImageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): RedpandaContainer(dockerImageName), GenericServer {

    companion object: KLogging() {
        const val IMAGE = "docker.redpanda.com/redpandadata/redpanda"
        const val TAG = "v24.3.1"
        const val NAME = "redpanda"

        const val PORT = 9092
        const val ADMIN_PORT = 9644
        const val SCHEMA_REGISTRY_PORT = 8081
        const val REST_PROXY_PORT = 8082

        /**
         * [DockerImageName]으로 [RedpandaServer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - 전달한 `dockerImageName`을 그대로 사용해 새 인스턴스를 반환합니다.
         * - 컨테이너 시작은 호출자가 `start()`로 수행해야 합니다.
         */
        @JvmStatic
        operator fun invoke(
            dockerImageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): RedpandaServer {
            return RedpandaServer(dockerImageName, useDefaultPort, reuse)
        }

        /**
         * 이미지 이름/태그로 [RedpandaServer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - `image`, `tag`가 blank이면 [IllegalArgumentException]이 발생합니다.
         * - 문자열 인자를 [DockerImageName]으로 변환해 새 인스턴스를 반환합니다.
         * - 이 함수는 컨테이너를 시작하지 않습니다.
         *
         * ```kotlin
         * val server = RedpandaServer(image = RedpandaServer.IMAGE, tag = RedpandaServer.TAG)
         * // server.url.startsWith("redpanda://") == true
         * ```
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): RedpandaServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(PORT)
    override val url: String get() = "redpanda://$host:$port"
    /** Admin API 포트의 매핑 결과입니다. */
    val adminPort: Int get() = getMappedPort(ADMIN_PORT)

    /** Schema Registry 포트의 매핑 결과입니다. */
    val schemaRegistryPort: Int get() = getMappedPort(SCHEMA_REGISTRY_PORT)

    /** REST Proxy 포트의 매핑 결과입니다. */
    val restProxyPort: Int get() = getMappedPort(REST_PROXY_PORT)

    init {
        addExposedPorts(PORT, ADMIN_PORT, SCHEMA_REGISTRY_PORT, REST_PROXY_PORT)
        withReuse(reuse)

        if (useDefaultPort) {
            exposeCustomPorts(PORT, ADMIN_PORT, SCHEMA_REGISTRY_PORT, REST_PROXY_PORT)
        }
    }

    override fun start() {
        super.start()

        val extraProps = mapOf(
            "admin.port" to adminPort,
            "schema.registry.port" to schemaRegistryPort,
            "rest.proxy.port" to restProxyPort,
        )
        writeToSystemProperties(NAME, extraProps)
    }

    /**
     * 테스트 전역에서 재사용할 Redpanda 서버 싱글턴을 제공합니다.
     *
     * ## 동작/계약
     * - 첫 접근 시 서버를 시작하고 [ShutdownQueue]에 종료 훅을 등록합니다.
     * - 이후 접근에서는 동일 인스턴스를 반환합니다.
     */
    object Launcher {
        val redpanda: RedpandaServer by lazy {
            RedpandaServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}
