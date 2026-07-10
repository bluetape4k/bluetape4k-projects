package io.bluetape4k.testcontainers.aws

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.net.URI
import java.time.Duration

/**
 * [MiniStack](https://ministack.org) AWS 에뮬레이터 서버.
 *
 * MIT 라이선스 기반의 경량 AWS 에뮬레이터로, 31개 이상의 AWS 서비스를 지원합니다.
 * LocalStack Community Edition 아카이브(2026-03-23) 이후의 주력 대안입니다.
 *
 * - Docker 이미지: `ministackorg/ministack:1.3.14` (~270MB, ~30MB RAM, ~2초 기동)
 * - 헬스 엔드포인트: `/_ministack/health`
 * - KMS 전 기능 지원 (DisableKey/EnableKey/Grant 포함)
 * - DynamoDB GSI Pagination 지원
 *
 * MiniStack은 모든 AWS 서비스를 항상 활성화합니다.
 * [withServices]를 호출해도 서비스 선택이 적용되지 않습니다 (no-op).
 *
 * ```kotlin
 * // 기본 설정으로 MiniStack 서버 시작
 * val server = MiniStackServer()
 * server.start()
 *
 * // 에뮬레이터 엔드포인트와 자격 증명 정보를 SDK 타입에 의존하지 않고 사용
 * val endpoint: URI = server.awsEndpoint
 * val accessKey: String = server.awsAccessKey
 * val secretKey: String = server.awsSecretKey
 * val region: String = server.regionName
 * ```
 *
 * 참고: [MiniStack 공식 사이트](https://ministack.org) · [Docker image](https://hub.docker.com/r/ministackorg/ministack)
 *
 * @param imageName      Docker 이미지 이름 ([DockerImageName])
 * @param useDefaultPort 기본 포트(4566)를 고정 바인딩할지 여부
 * @param reuse          컨테이너 재사용 여부
 */
class MiniStackServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<MiniStackServer>(imageName), GenericServer, PropertyExportingServer, AwsEmulatorServer {

    companion object: KLogging() {
        /** MiniStack Docker 이미지 이름 (Repository) */
        const val IMAGE = "ministackorg/ministack"

        /** MiniStack Docker 이미지 기본 태그 */
        const val TAG = "1.3.14"

        /** PropertyExportingServer 네임스페이스 및 컨테이너 식별자 */
        const val NAME = "ministack"

        /** MiniStack이 노출하는 단일 AWS 에뮬레이터 포트 */
        const val PORT = 4566

        /** 기본 access key (MiniStack은 임의의 값을 허용함) */
        const val DEFAULT_ACCESS_KEY = "test"

        /** 기본 secret key (MiniStack은 임의의 값을 허용함) */
        const val DEFAULT_SECRET_KEY = "test"

        /** 기본 region 이름 */
        const val DEFAULT_REGION = "us-east-1"

        /**
         * 이미지 이름/태그로 [MiniStackServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val server = MiniStackServer(image = "ministackorg/ministack", tag = "1.3.14")
         * server.start()
         * // server.url.startsWith("http://") == true
         * ```
         *
         * @param image          Docker 이미지 이름. blank이면 [IllegalArgumentException]이 발생합니다.
         * @param tag            Docker 이미지 태그. blank이면 [IllegalArgumentException]이 발생합니다.
         * @param useDefaultPort `true`면 4566 포트를 고정 바인딩합니다.
         *                       ⚠️ CI 환경에서 여러 테스트를 병렬 실행하는 경우 포트 충돌이 발생할 수 있습니다.
         *                       병렬 테스트에서는 `false`(기본값)를 사용하여 랜덤 포트를 할당받으세요.
         * @param reuse          컨테이너 재사용 여부
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = false,
        ): MiniStackServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }

        /**
         * [DockerImageName]으로 [MiniStackServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val image = DockerImageName.parse("ministackorg/ministack").withTag("1.3.14")
         * val server = MiniStackServer(image)
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
            reuse: Boolean = false,
        ): MiniStackServer {
            return MiniStackServer(imageName, useDefaultPort, reuse)
        }
    }

    /** 컨테이너의 매핑된 MiniStack 포트(4566)를 반환합니다. */
    override val port: Int get() = getMappedPort(PORT)

    /** MiniStack 서버의 기본 URL (예: `http://localhost:32768`)을 반환합니다. */
    override val url: String get() = "http://$host:$port"

    /**
     * AWS 에뮬레이터 엔드포인트 URI.
     *
     * AWS SDK 클라이언트의 `endpointOverride`로 사용합니다.
     */
    override val awsEndpoint: URI get() = URI.create("http://$host:$port")

    /** 기본 AWS access key (`"test"`). MiniStack은 임의의 값을 허용합니다. */
    override val awsAccessKey: String = DEFAULT_ACCESS_KEY

    /** 기본 AWS secret key (`"test"`). MiniStack은 임의의 값을 허용합니다. */
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
     * ⚠️ 컨테이너가 시작된 이후에만 호출해야 합니다.
     * 시작 전 호출 시 [host]/[port] 평가 과정에서 `IllegalStateException`이 발생합니다.
     * 시작 여부와 무관하게 키 목록만 필요한 경우 [propertyKeys]를 사용하세요.
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

        // MiniStack 공식 헬스 엔드포인트 사용
        setWaitStrategy(
            Wait.forHttp("/_ministack/health")
                .forStatusCode(200)
                .withStartupTimeout(Duration.ofSeconds(60))
        )

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    /**
     * 활성화할 AWS 서비스 목록을 지정합니다.
     *
     * > ⚠️ MiniStack은 모든 AWS 서비스를 항상 활성화하므로 이 메서드는 **no-op**입니다.
     * > 호출해도 서비스 선택이 적용되지 않으며, 동일한 [MiniStackServer] 인스턴스를 그대로 반환합니다.
     *
     * @param services (무시됨) 활성화할 서비스 이름 목록
     * @return 메서드 체이닝을 위한 현재 [MiniStackServer] 인스턴스
     */
    override fun withServices(vararg services: String): MiniStackServer {
        if (services.isNotEmpty()) {
            log.warn { "withServices(${services.toList()}) is a no-op: MiniStack enables all AWS services by default. Service selection is ignored." }
        }
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
     * 테스트에서 재사용할 [MiniStackServer] 싱글턴을 제공합니다.
     *
     * 첫 접근 시 컨테이너를 시작하고 JVM 종료 시 자동 정리하도록 [ShutdownQueue]에 등록합니다.
     */
    object Launcher {
        /**
         * 지연 초기화되는 [MiniStackServer] 싱글턴.
         *
         * ```kotlin
         * val miniStack = MiniStackServer.Launcher.miniStack
         * val endpoint = miniStack.awsEndpoint
         * ```
         */
        val miniStack: MiniStackServer by lazy {
            MiniStackServer.log.debug { "Starting MiniStack singleton container..." }
            MiniStackServer().apply {
                start()
                ShutdownQueue.register(this)
                MiniStackServer.log.debug { "MiniStack singleton started. endpoint=$awsEndpoint" }
            }
        }
    }
}
