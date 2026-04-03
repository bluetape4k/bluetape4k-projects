package io.bluetape4k.testcontainers.mq

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import io.nats.client.Connection
import io.nats.client.Nats
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

/**
 * NATS 테스트 서버 컨테이너를 실행하고 연결 정보를 제공합니다.
 *
 * ## 동작/계약
 * - `-js` 옵션으로 JetStream을 활성화한 상태로 컨테이너를 구성합니다.
 * - `4222/6222/8222` 포트를 노출하며 `useDefaultPort=true`이면 호스트 고정 바인딩을 시도합니다.
 * - 인스턴스 생성만으로는 시작되지 않고 `start()` 호출 후 시스템 프로퍼티를 기록합니다.
 *
 * ```kotlin
 * val server = NatsServer()
 * server.start()
 * // server.natsPort > 0
 * ```
 *
 * 참고: [Nats official images](https://hub.docker.com/_/nats?tab=description&page=1&ordering=last_updated)
 */
class NatsServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<NatsServer>(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        const val IMAGE = "nats"
        const val TAG = "2.12"
        const val NAME = "nats"

        const val NATS_PORT = 4222
        const val NATS_CLUSTER_PORT = 6222
        const val NATS_MONITOR_PORT = 8222

        /**
         * [DockerImageName]으로 [NatsServer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - 전달한 `imageName`을 그대로 사용해 새 인스턴스를 반환합니다.
         * - 이 함수는 컨테이너를 시작하지 않습니다.
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): NatsServer {
            return NatsServer(imageName, useDefaultPort, reuse)
        }

        /**
         * 이미지 이름/태그로 [NatsServer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - `image`, `tag`가 blank이면 [IllegalArgumentException]이 발생합니다.
         * - 문자열 인자를 [DockerImageName]으로 변환해 새 인스턴스를 반환합니다.
         * - 컨테이너 시작은 호출자가 `start()`로 수행해야 합니다.
         *
         * ```kotlin
         * val server = NatsServer(image = "nats", tag = "2.10")
         * // server.url.startsWith("nats://") == true
         * ```
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): NatsServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(NATS_PORT)
    override val url: String get() = "$NAME://$host:$port"

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf(
        "host", "port", "url",
        "cluster-port", "monitor-port",
    )

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
        "cluster-port" to clusterPort.toString(),
        "monitor-port" to monitorPort.toString(),
    )

    /** NATS 클라이언트 접속 포트의 매핑 결과입니다. */
    val natsPort: Int get() = getMappedPort(NATS_PORT)

    /** NATS 클러스터 내부 통신 포트의 매핑 결과입니다. */
    val clusterPort: Int get() = getMappedPort(NATS_CLUSTER_PORT)

    /** 모니터링 API 포트의 매핑 결과입니다. */
    val monitorPort: Int get() = getMappedPort(NATS_MONITOR_PORT)

    init {
        addExposedPorts(NATS_PORT, NATS_CLUSTER_PORT, NATS_MONITOR_PORT)
        withReuse(reuse)

        // JetStream 을 사용하기 위해서 지정
        // 참고; [Nats Commandline Options](https://hub.docker.com/_/nats)
        withCommand("-js")

        if (useDefaultPort) {
            // 위에 addExposedPorts 를 등록했으면, 따로 지정하지 않으면 그 값들을 사용합니다.
            exposeCustomPorts(NATS_PORT, NATS_CLUSTER_PORT, NATS_MONITOR_PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    /**
     * 테스트 전역에서 재사용할 NATS 서버 싱글턴을 제공합니다.
     *
     * ## 동작/계약
     * - 첫 접근 시 서버를 시작하고 [ShutdownQueue]에 종료 훅을 등록합니다.
     * - 이후 접근에서는 동일 인스턴스를 반환합니다.
     */
    object Launcher {
        val nats: NatsServer by lazy {
            NatsServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}

/**
 * NATS 연결을 열어 블록을 실행하고 자동으로 연결을 닫습니다.
 *
 * ## 동작/계약
 * - `Nats.connect(url)`로 연결한 뒤 `use` 블록에서만 연결을 사용합니다.
 * - 블록 완료/예외 여부와 상관없이 연결은 닫힙니다.
 *
 * ```kotlin
 * val pong = withNats(server.url) { status() }
 * // pong != null
 * ```
 */
inline fun <T> withNats(url: String, block: Connection.() -> T): T {
    return Nats.connect(url).use { connection: Connection ->
        block(connection)
    }
}
