package io.bluetape4k.testcontainers.spring

import io.bluetape4k.testcontainers.PropertyExportingServer
import org.springframework.test.context.DynamicPropertyRegistry

private const val SYSTEM_PROPERTY_PREFIX = "testcontainers"

/**
 * [PropertyExportingServer]의 연결 정보를 Spring [DynamicPropertyRegistry]에 등록합니다.
 *
 * `propertyKeys()`만 등록 시점에 읽고, 각 값은 Spring registry supplier가 실제
 * 프로퍼티를 해석할 때 [PropertyExportingServer.properties]에서 가져옵니다. 따라서
 * 이 함수는 컨테이너를 시작하거나 중지하지 않고 JVM system property도 변경하지
 * 않습니다.
 *
 * 키는 core의 시스템 프로퍼티 계약과 같은
 * `testcontainers.{propertyNamespace}.{key}` 형식을 사용합니다. 선언된 키가
 * [PropertyExportingServer.properties]에 없으면 supplier 평가 시
 * [IllegalStateException]으로 실패합니다. 같은 키의 중복 등록은 사전 검사하거나
 * 덮어쓰지 않고 Spring registry의 등록 순서와 우선순위 semantics에 위임합니다.
 *
 * ```kotlin
 * import io.bluetape4k.testcontainers.storage.RedisServer
 *
 * companion object {
 *     @DynamicPropertySource
 *     @JvmStatic
 *     fun registerProperties(registry: DynamicPropertyRegistry) {
 *         RedisServer.Launcher.redis.registerDynamicProperties(registry)
 *     }
 * }
 * ```
 */
public fun PropertyExportingServer.registerDynamicProperties(
    registry: DynamicPropertyRegistry,
) {
    propertyKeys().forEach { key ->
        val fullKey = "$SYSTEM_PROPERTY_PREFIX.$propertyNamespace.$key"
        registry.add(fullKey) {
            properties()[key]
                ?: error(
                    "PropertyExportingServer '$propertyNamespace' did not provide property '$key'",
                )
        }
    }
}
