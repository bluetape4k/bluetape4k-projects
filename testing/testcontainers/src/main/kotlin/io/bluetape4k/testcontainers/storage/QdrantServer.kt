package io.bluetape4k.testcontainers.storage

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.qdrant.QdrantContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration

/**
 * 공식 [QdrantContainer]의 준비 상태 검사를 사용하는 테스트 서버입니다.
 *
 * 명시적 인스턴스는 `use { start() }`로 종료하고, [Launcher] 인스턴스는 직접 종료하지 않습니다.
 * endpoint는 시작 후에만 조회할 수 있으며, [port]와 [url]은 HTTP 연결 정보입니다.
 * 기본값은 임의 호스트 포트와 비재사용이며 인증 없는 테스트 전용 설정입니다.
 * `start()`가 export한 JVM 속성은 `stop()`에서 자동 복원하지 않습니다.
 */
class QdrantServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): QdrantContainer(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        /** 기본 Docker 이미지입니다. */
        const val IMAGE = "qdrant/qdrant"

        /** 검증에 사용하는 고정 이미지 태그입니다. */
        const val TAG = "v1.19.0"

        /** 시스템 속성 namespace입니다. */
        const val NAME = "qdrant"

        /** 컨테이너 내부 HTTP 포트입니다. */
        const val HTTP_PORT = 6333

        /** 컨테이너 내부 gRPC 포트입니다. */
        const val GRPC_PORT = 6334

        /**
         * 아직 시작하지 않은 서버를 생성합니다.
         *
         * @param image Docker 이미지 이름입니다. 빈 값은 허용하지 않습니다.
         * @param tag 이미지 태그입니다. 빈 값은 허용하지 않습니다.
         * @param useDefaultPort HTTP·gRPC 표준 호스트 포트를 사용할지 여부입니다.
         * @param reuse Testcontainers의 실행 간 재사용을 명시적으로 활성화합니다.
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = false,
        ): QdrantServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            return QdrantServer(DockerImageName.parse(image).withTag(tag), useDefaultPort, reuse)
        }
    }

    /** 시작된 서버의 mapped HTTP 포트입니다. */
    val httpPort: Int get() = getMappedPort(HTTP_PORT)

    override val port: Int get() = httpPort
    override val url: String get() = "http://$host:$httpPort"
    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf("host", "port", "url", "http-port", "grpc-port")

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
        "http-port" to httpPort.toString(),
        "grpc-port" to grpcPort.toString(),
    )

    init {
        exposedPorts = listOf(HTTP_PORT, GRPC_PORT)
        withReuse(reuse)
        withStartupTimeout(Duration.ofMinutes(2))
        if (useDefaultPort) {
            exposeCustomPorts(HTTP_PORT, GRPC_PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties()
        log.debug { "QdrantServer 시작: url=$url, grpcPort=$grpcPort" }
    }

    /** JVM 안에서 공유하며 [ShutdownQueue]가 종료하는 서버입니다. */
    object Launcher {
        /** 호출자가 종료하지 않는 공유 서버 인스턴스입니다. */
        val qdrant: QdrantServer by lazy {
            QdrantServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}
