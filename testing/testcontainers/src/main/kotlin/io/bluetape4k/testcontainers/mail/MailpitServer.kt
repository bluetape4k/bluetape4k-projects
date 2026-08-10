package io.bluetape4k.testcontainers.mail

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

/**
 * [Mailpit](https://github.com/axllent/mailpit) SMTP 메일 캐처 서버.
 *
 * 테스트 환경에서 이메일 발송을 캡처하고 검증하기 위한 경량 SMTP 서버입니다.
 * AWS SES API는 지원하지 않으며 SMTP 프로토콜만 지원합니다.
 *
 * - SMTP 포트: 1025
 * - Web UI 포트: 8025 (메일 목록 확인)
 *
 * 참고: [Mailpit Docker image](https://hub.docker.com/r/axllent/mailpit/tags)
 *
 * @param imageName      Docker 이미지 이름 ([DockerImageName])
 * @param useDefaultPort Default 포트를 사용할지 여부
 * @param reuse          컨테이너 재사용 여부
 */
class MailpitServer private constructor(
    imageName: DockerImageName,
    private val useDefaultPort: Boolean = false,
    private val reuse: Boolean = false,
): GenericContainer<MailpitServer>(imageName), GenericServer, PropertyExportingServer {

    /**
     * [MailpitServer] 인스턴스 생성을 위한 팩토리 메서드와 상수를 제공합니다.
     */
    companion object: KLogging() {
        const val IMAGE = "axllent/mailpit"
        const val TAG = "v1.30.7"
        const val NAME = "mailpit"
        const val SMTP_PORT = 1025
        const val UI_PORT = 8025

        /**
         * 이미지 이름/태그로 [MailpitServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val server = MailpitServer(image = "axllent/mailpit", tag = TAG)
         * // server.url.startsWith("smtp://") == true (시작 후)
         * ```
         *
         * @param image          Docker 이미지 이름, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param tag            Docker 이미지 태그, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param useDefaultPort `true`면 1025/8025 포트를 고정 바인딩합니다.
         * @param reuse          컨테이너 재사용 여부입니다.
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = false,
        ): MailpitServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return MailpitServer(imageName, useDefaultPort, reuse)
        }

        /**
         * [DockerImageName]으로 [MailpitServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val image = DockerImageName.parse("axllent/mailpit").withTag(TAG)
         * val server = MailpitServer(image)
         * // server.isRunning == false
         * ```
         *
         * @param imageName      Docker 이미지 이름
         * @param useDefaultPort `true`면 1025/8025 포트를 고정 바인딩합니다.
         * @param reuse          컨테이너 재사용 여부입니다.
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = false,
        ): MailpitServer {
            return MailpitServer(imageName, useDefaultPort, reuse)
        }
    }

    /** 컨테이너의 매핑된 SMTP 포트를 반환합니다. */
    override val port: Int get() = getMappedPort(SMTP_PORT)

    /** 컨테이너의 매핑된 Web UI 포트를 반환합니다. */
    val uiPort: Int get() = getMappedPort(UI_PORT)

    /** Mailpit SMTP 서버 URL (예: `smtp://localhost:1025`)을 반환합니다. */
    override val url: String get() = "smtp://$host:$port"

    /** Mailpit Web UI URL (예: `http://localhost:8025`)을 반환합니다. */
    val uiUrl: String get() = "http://$host:$uiPort"

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> =
        setOf("host", "smtp-port", "ui-port", "smtp-url", "ui-url")

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "smtp-port" to port.toString(),
        "ui-port" to uiPort.toString(),
        "smtp-url" to url,
        "ui-url" to uiUrl,
    )

    init {
        addExposedPorts(SMTP_PORT, UI_PORT)
        withReuse(reuse)
        if (useDefaultPort) {
            exposeCustomPorts(SMTP_PORT, UI_PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    /**
     * 테스트에서 재사용할 Mailpit 서버 싱글턴을 제공합니다.
     */
    object Launcher {
        val mailpit: MailpitServer by lazy {
            MailpitServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}
