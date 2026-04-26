package io.bluetape4k.testcontainers.aws

import io.bluetape4k.logging.KLogging
import io.bluetape4k.utils.ShutdownQueue
import org.elasticmq.rest.sqs.SQSRestServer
import org.elasticmq.rest.sqs.SQSRestServerBuilder
import java.net.URI
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * [ElasticMQ](https://github.com/softwaremill/elasticmq) 내장 SQS 에뮬레이터 서버.
 *
 * Docker 컨테이너 없이 JVM 내에서 직접 실행되는 AWS SQS 호환 서버입니다.
 * `org.elasticmq:elasticmq-rest-sqs_2.13` 라이브러리를 사용합니다.
 *
 * ```kotlin
 * val server = ElasticMqServer()
 * server.start()
 * // SQS endpoint: server.endpoint
 * server.stop()
 * ```
 *
 * 싱글턴으로 사용하려면 [Launcher]를 활용하세요:
 *
 * ```kotlin
 * val server = ElasticMqServer.Launcher.elasticMq
 * // server는 이미 시작된 상태이며, JVM 종료 시 자동으로 중지됩니다.
 * ```
 *
 * @param port        서버 바인딩 포트 (기본: 9324)
 * @param bindAddress 바인딩 주소 (기본: "localhost")
 */
class ElasticMqServer(
    val port: Int = DEFAULT_PORT,
    val bindAddress: String = DEFAULT_BIND_ADDRESS,
) : AutoCloseable {
    companion object : KLogging() {
        const val DEFAULT_PORT = 9324
        const val DEFAULT_BIND_ADDRESS = "localhost"
        const val ACCESS_KEY = "x"
        const val SECRET_KEY = "x"
        const val REGION_NAME = "us-east-1"
    }

    /** SQS 엔드포인트 URI */
    val endpoint: URI get() = URI.create("http://$bindAddress:$port")

    /** SQS Access Key (ElasticMQ 기본값) */
    val accessKey: String = ACCESS_KEY

    /** SQS Secret Key (ElasticMQ 기본값) */
    val secretKey: String = SECRET_KEY

    /** AWS 리전명 */
    val regionName: String = REGION_NAME

    private var server: SQSRestServer? = null
    private val lock = ReentrantLock()

    /**
     * ElasticMQ SQS 서버를 시작합니다.
     *
     * 이미 시작된 경우에는 아무 동작도 하지 않습니다. Thread-safe합니다.
     */
    fun start() = lock.withLock {
        if (server != null) return@withLock
        // Scala ElasticMQ 라이브러리가 KLogging의 람다 오버로드와 충돌하므로 문자열 형식 사용
        log.debug("Starting ElasticMQ SQS server on $bindAddress:$port")
        server = SQSRestServerBuilder
            .withPort(port)
            .withInterface(bindAddress)
            .start()
        log.info("ElasticMQ SQS server started. Endpoint: $endpoint")
    }

    /**
     * ElasticMQ SQS 서버를 중지합니다.
     *
     * 이미 중지된 경우에는 아무 동작도 하지 않습니다 (double-stop safe). Thread-safe합니다.
     */
    fun stop() = lock.withLock {
        server?.stopAndWait()
        server = null
        log.info("ElasticMQ SQS server stopped.")
    }

    override fun close() = stop()

    /**
     * 서버가 현재 실행 중인지 여부를 반환합니다.
     */
    val isRunning: Boolean get() = lock.withLock { server != null }

    /**
     * 테스트에서 재사용할 [ElasticMqServer] 싱글턴을 제공합니다.
     *
     * ```kotlin
     * // 테스트 코드에서 싱글턴 서버 사용
     * val sqsEndpoint = ElasticMqServer.Launcher.elasticMq.endpoint
     * ```
     */
    object Launcher {
        val elasticMq: ElasticMqServer by lazy {
            ElasticMqServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}
