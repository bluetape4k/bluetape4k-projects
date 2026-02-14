package io.bluetape4k.testcontainers

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.info
import org.testcontainers.containers.ContainerState

/**
 * Testcontainers를 사용하는 Server의 기본 정보를 제공합니다.
 */
interface GenericServer: ContainerState {
    /**
     * Server의 기본 포트를 제공합니다.
     */
    val port: Int get() = firstMappedPort

    /**
     * Server의 URL 정보를 제공합니다.
     */
    val url: String get() = "$host:$port"
}

private val log by lazy { KotlinLogging.logger {} }

internal const val SERVER_PREFIX = "testcontainers"

/**
 * 테스트를 위한 Server의 기본 정보를 System Property로 등록하여 Application 환경설정에서 사용할 수 있도록 합니다.
 *
 * ```properties
 * spring.redis.host = ${testcontainers.redis.host}
 * spring.redis.port = ${testcontainers.redis.port}
 * spring.redis.url = ${testcontainers.redis.url}
 * ```
 */
fun <T: GenericServer> T.writeToSystemProperties(name: String, extraProps: Map<String, Any?> = emptyMap()) {
    require(name.isNotBlank()) { "Server name must not be blank." }
    log.info { "Setup Server properties ..." }

    System.setProperty("$SERVER_PREFIX.$name.host", this.host)
    System.setProperty("$SERVER_PREFIX.$name.port", this.port.toString())
    System.setProperty("$SERVER_PREFIX.$name.url", this.url)

    extraProps.forEach { (key, value) ->
        value?.run {
            System.setProperty("$SERVER_PREFIX.$name.$key", this.toString())
        }
    }

    log.info {
        buildString {
            appendLine()
            appendLine("Start $name Server:")
            appendLine("\t$SERVER_PREFIX.$name.host=$host")
            appendLine("\t$SERVER_PREFIX.$name.port=$port")
            appendLine("\t$SERVER_PREFIX.$name.url=$url")

            extraProps.forEach { (key, value) ->
                value?.let {
                    appendLine("\t$SERVER_PREFIX.$name.$key=$it")
                }
            }
        }
    }
}
