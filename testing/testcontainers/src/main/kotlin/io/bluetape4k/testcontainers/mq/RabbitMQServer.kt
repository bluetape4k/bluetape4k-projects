package io.bluetape4k.testcontainers.mq

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.utility.DockerImageName


/**
 * RabbitMQ 테스트 서버 컨테이너를 실행하고 AMQP/관리 포트 정보를 제공합니다.
 *
 * ## 동작/계약
 * - AMQP/AMQPS/관리 HTTP/HTTPS 포트를 모두 노출합니다.
 * - `useDefaultPort=true`이면 각 포트의 호스트 고정 바인딩을 시도합니다.
 * - 인스턴스 생성만으로는 시작되지 않으며 `start()` 호출 시 추가 시스템 프로퍼티를 기록합니다.
 *
 * ```kotlin
 * val server = RabbitMQServer()
 * server.start()
 * // server.amqpPort > 0
 * ```
 *
 * 참고: [rabbitmq docker official images](https://hub.docker.com/_/rabbitmq/tags)
 */
class RabbitMQServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): RabbitMQContainer(imageName), GenericServer {

    companion object: KLogging() {
        const val IMAGE = "rabbitmq"
        const val TAG = "3.13"           // NOTE: 4 이상을 실행하면 예외가 발생합니다.

        const val NAME = "rabbitmq"

        const val AMQP_PORT = 5672
        const val AMQPS_PORT = 5671
        const val RABBITMQ_HTTP_PORT = 15672
        const val RABBITMQ_HTTPS_PORT = 15671

        /**
         * 이미지 이름/태그로 [RabbitMQServer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - `image`, `tag`가 blank이면 [IllegalArgumentException]이 발생합니다.
         * - 문자열 인자를 [DockerImageName]으로 변환해 새 인스턴스를 반환합니다.
         * - 컨테이너 시작은 호출자가 `start()`로 수행해야 합니다.
         *
         * ```kotlin
         * val server = RabbitMQServer(image = "rabbitmq", tag = "3.13")
         * // server.url.startsWith("amqp://") == true
         * ```
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): RabbitMQServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            
            val imageName = DockerImageName.parse(image).withTag(tag)
            return RabbitMQServer(imageName, useDefaultPort, reuse)
        }

        /**
         * [DockerImageName]으로 [RabbitMQServer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - 전달한 `dockerImageName`을 그대로 사용해 새 인스턴스를 반환합니다.
         * - 이 함수는 컨테이너를 시작하지 않습니다.
         */
        @JvmStatic
        operator fun invoke(
            dockerImageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): RabbitMQServer {
            return RabbitMQServer(dockerImageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(AMQP_PORT)
    override val url: String get() = "amqp://$host:$port"

    /** AMQP 포트의 매핑 결과입니다. */
    val amqpPort: Int get() = getMappedPort(AMQP_PORT)

    /** AMQPS 포트의 매핑 결과입니다. */
    val amqpsPort: Int get() = getMappedPort(AMQPS_PORT)

    /** RabbitMQ 관리 HTTP 포트의 매핑 결과입니다. */
    val rabbitmqHttpPort: Int get() = getMappedPort(RABBITMQ_HTTP_PORT)

    /** RabbitMQ 관리 HTTPS 포트의 매핑 결과입니다. */
    val rabbitmqHttpsPort: Int get() = getMappedPort(RABBITMQ_HTTPS_PORT)

    init {
        addExposedPorts(AMQP_PORT, AMQPS_PORT, RABBITMQ_HTTP_PORT, RABBITMQ_HTTPS_PORT)
        withReuse(reuse)

        // wait strategy 는 RabbitMQContainer 생성자에서 설정합니다.

        if (useDefaultPort) {
            // 위에 addExposedPorts 를 등록했으면, 따로 지정하지 않으면 그 값들을 사용합니다.
            exposeCustomPorts(AMQP_PORT, AMQPS_PORT, RABBITMQ_HTTP_PORT, RABBITMQ_HTTPS_PORT)
        }
    }

    override fun start() {
        super.start()

        val props = mapOf(
            "amqp.port" to amqpPort,
            "amqps.port" to amqpsPort,
            "rabbitmq.http.port" to rabbitmqHttpPort,
            "rabbitmq.https.port" to rabbitmqHttpsPort
        )
        writeToSystemProperties(NAME, props)
    }

    /**
     * 테스트 전역에서 재사용할 RabbitMQ 서버 싱글턴을 제공합니다.
     *
     * ## 동작/계약
     * - 첫 접근 시 서버를 시작하고 [ShutdownQueue]에 종료 훅을 등록합니다.
     * - 이후 접근에서는 동일 인스턴스를 반환합니다.
     */
    object Launcher {
        val rabbitMQ by lazy {
            RabbitMQServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}
