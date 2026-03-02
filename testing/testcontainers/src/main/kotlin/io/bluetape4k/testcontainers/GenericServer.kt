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
 *
 * ## 동작/계약
 * - `name`이 blank이면 [IllegalArgumentException]이 발생합니다.
 * - `${testcontainers.$name.host|port|url}` 기본 속성을 항상 기록합니다.
 * - `extraProps`의 null 값은 무시하고 non-null 값만 문자열로 기록합니다.
 * - JVM 전역 System Property를 변경하므로 테스트 종료 후 정리가 필요할 수 있습니다.
 *
 * ```kotlin
 * redis.writeToSystemProperties("redis", mapOf("ssl" to false))
 * // testcontainers.redis.host/port/url/ssl 속성이 등록됨
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
