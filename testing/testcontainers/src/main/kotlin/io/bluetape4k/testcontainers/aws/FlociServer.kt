package io.bluetape4k.testcontainers.aws

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.net.URI

/**
 * [Floci](https://github.com/floci-io/floci) AWS 에뮬레이터 서버.
 *
 * GraalVM Native Image로 빌드된 경량 AWS 에뮬레이터입니다.
 * LocalStack Community edition archived(2026-03-23) 이후의 대안으로 활용할 수 있습니다.
 *
 * Floci는 Maven 아티팩트를 제공하지 않으므로 Testcontainers의 [GenericContainer]를
 * 직접 래핑하여 사용합니다.
 *
 * > ⚠️ **주의**: Floci는 모든 AWS 서비스를 항상 활성화합니다.
 * > [withServices]를 호출해도 서비스 선택이 적용되지 않습니다 (no-op).
 *
 * > ⚠️ **@Deprecated**: floci는 아직 초기 단계입니다.
 * > 안정 버전이 확인될 때까지 [LocalStackServer]를 사용하는 것을 권장합니다.
 *
 * ```kotlin
 * // 기본 설정으로 Floci 서버 시작
 * val server = FlociServer()
 * server.start()
 *
 * // 에뮬레이터 엔드포인트와 자격 증명 정보를 SDK 타입에 의존하지 않고 사용
 * val endpoint: URI = server.awsEndpoint
 * val accessKey: String = server.awsAccessKey
 * val secretKey: String = server.awsSecretKey
 * val region: String = server.regionName
 * ```
 *
 * 참고: [Floci GitHub](https://github.com/floci-io/floci) · [Docker image](https://hub.docker.com/r/floci/floci/tags)
 *
 * @param imageName      Docker 이미지 이름 ([DockerImageName])
 * @param useDefaultPort Default 포트(4566)를 고정 바인딩할지 여부
 * @param reuse          컨테이너 재사용 여부
 */
@Deprecated(
    message = "floci는 아직 초기 단계입니다. 안정 버전이 확인될 때까지 LocalStackServer를 사용하는 것을 권장합니다.",
    level = DeprecationLevel.WARNING
)
class FlociServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<FlociServer>(imageName), GenericServer, PropertyExportingServer, AwsEmulatorServer {

    companion object: KLogging() {
        /** Floci Docker 이미지 이름 (Repository) */
        const val IMAGE = "floci/floci"

        /** Floci Docker 이미지 기본 태그 */
        const val TAG = "1.5.8"

        /** PropertyExportingServer 네임스페이스 및 컨테이너 식별자 */
        const val NAME = "floci"

        /** Floci가 노출하는 단일 AWS 에뮬레이터 포트 */
        const val PORT = 4566

        /** 기본 access key (Floci는 임의의 값을 허용함) */
        const val DEFAULT_ACCESS_KEY = "test"

        /** 기본 secret key (Floci는 임의의 값을 허용함) */
        const val DEFAULT_SECRET_KEY = "test"

        /** 기본 region 이름 */
        const val DEFAULT_REGION = "us-east-1"

        /**
         * 이미지 이름/태그로 [FlociServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val server = FlociServer(image = "floci/floci", tag = "1.5.7")
         * server.start()
         * // server.url.startsWith("http://") == true
         * ```
         *
         * @param image          Docker 이미지 이름. blank이면 [IllegalArgumentException]이 발생합니다.
         * @param tag            Docker 이미지 태그. blank이면 [IllegalArgumentException]이 발생합니다.
         * @param useDefaultPort `true`면 4566 포트를 고정 바인딩합니다.
         * @param reuse          컨테이너 재사용 여부
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): FlociServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }

        /**
         * [DockerImageName]으로 [FlociServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val image = DockerImageName.parse("floci/floci").withTag("1.5.7")
         * val server = FlociServer(image)
         * // server.isRunning == false (아직 start 전)
         * ```
         *
         * @param imageName      Docker 이미지 이름
         * @param useDefaultPort `true`면 4566 포트를 고정 바인딩합니다.
         * @param reuse          컨테이너 재사용 여부
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): FlociServer {
            return FlociServer(imageName, useDefaultPort, reuse)
        }
    }

    /** 컨테이너의 매핑된 Floci 포트(4566)를 반환합니다. */
    override val port: Int get() = getMappedPort(PORT)

    /** Floci 서버의 기본 URL (예: `http://localhost:32768`)을 반환합니다. */
    override val url: String get() = "http://$host:$port"

    /**
     * AWS 에뮬레이터 엔드포인트 URI.
     *
     * AWS SDK 클라이언트의 `endpointOverride`로 사용합니다.
     */
    override val awsEndpoint: URI get() = URI.create("http://$host:$port")

    /** 기본 AWS access key (`"test"`). Floci는 임의의 값을 허용합니다. */
    override val awsAccessKey: String = DEFAULT_ACCESS_KEY

    /** 기본 AWS secret key (`"test"`). Floci는 임의의 값을 허용합니다. */
    override val awsSecretKey: String = DEFAULT_SECRET_KEY

    /** 기본 AWS region 이름 (`"us-east-1"`). */
    override val regionName: String = DEFAULT_REGION

    /** PropertyExportingServer 네임스페이스 ([NAME]). */
    override val propertyNamespace: String = NAME

    /**
     * 시스템 프로퍼티로 export할 키 목록을 반환합니다.
     *
     * 모든 키는 kebab-case 소문자를 사용합니다.
     * `host`, `port`, `url`, `aws-endpoint`, `aws-access-key`, `aws-secret-key`, `region`을 노출합니다.
     */
    override fun propertyKeys(): Set<String> =
        setOf("host", "port", "url", "aws-endpoint", "aws-access-key", "aws-secret-key", "region")

    /**
     * 시스템 프로퍼티로 export할 key/value 맵을 반환합니다.
     *
     * 컨테이너가 시작된 이후에 호출되어야 [host], [port]가 유효합니다.
     */
    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
        "aws-endpoint" to awsEndpoint.toString(),
        "aws-access-key" to awsAccessKey,
        "aws-secret-key" to awsSecretKey,
        "region" to regionName,
    )

    init {
        addExposedPorts(PORT)
        withReuse(reuse)

        // Floci는 공식 health endpoint를 제공하지 않으므로 listening port를 대기 전략으로 사용
        setWaitStrategy(Wait.forListeningPort())

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    /**
     * 활성화할 AWS 서비스 목록을 지정합니다.
     *
     * > ⚠️ Floci는 모든 AWS 서비스를 항상 활성화하므로 이 메서드는 **no-op**입니다.
     * > 호출해도 서비스 선택이 적용되지 않으며, 동일한 [FlociServer] 인스턴스를 그대로 반환합니다.
     *
     * @param services (무시됨) 활성화할 서비스 이름 목록
     * @return 메서드 체이닝을 위한 현재 [FlociServer] 인스턴스
     */
    override fun withServices(vararg services: String): FlociServer {
        log.debug { "Floci enables all services by default. withServices(${services.toList()}) is a no-op." }
        return this
    }

    /**
     * 컨테이너를 시작하고, 시스템 프로퍼티로 [properties]를 export합니다.
     */
    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    /**
     * 테스트에서 재사용할 [FlociServer] 싱글턴을 제공합니다.
     *
     * 첫 접근 시 컨테이너를 시작하고 JVM 종료 시 자동 정리하도록 [ShutdownQueue]에 등록합니다.
     */
    object Launcher {
        /**
         * 지연 초기화되는 [FlociServer] 싱글턴.
         *
         * ```kotlin
         * val floci = FlociServer.Launcher.floci
         * val endpoint = floci.endpoint
         * ```
         */
        val floci: FlociServer by lazy {
            FlociServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}
