package io.bluetape4k.testcontainers

/**
 * 시스템 프로퍼티 export 계약을 정의하는 인터페이스.
 *
 * 모든 testcontainers 서버가 이 인터페이스를 구현하여 일관된 키 명명 규칙과
 * 프로퍼티 등록/해제 기능을 제공합니다.
 *
 * 시스템 프로퍼티 키는 `testcontainers.{propertyNamespace}.{key}` 형식으로 생성됩니다.
 *
 * ```kotlin
 * class MyServer : GenericContainer<MyServer>("image"), PropertyExportingServer {
 *     override val propertyNamespace: String = "my-server"
 *     override fun propertyKeys(): Set<String> = setOf("host", "port", "url")
 *     override fun properties(): Map<String, String> = mapOf(
 *         "host" to host,
 *         "port" to firstMappedPort.toString(),
 *         "url" to "$host:$firstMappedPort",
 *     )
 * }
 * ```
 */
interface PropertyExportingServer {

    /**
     * 이 서버의 프로퍼티 네임스페이스.
     *
     * 시스템 프로퍼티 키는 `testcontainers.{propertyNamespace}.{key}` 형식입니다.
     * 예: `propertyNamespace = "redis"` → `testcontainers.redis.host`
     */
    val propertyNamespace: String

    /**
     * 이 서버가 export하는 프로퍼티 키 목록.
     *
     * `start()` 호출 전에도 사용 가능합니다 (키 이름만 반환, 값 없음).
     * contract test에서 키 명명 규칙 검증에 활용합니다.
     *
     * @return 프로퍼티 키 이름의 [Set] (네임스페이스 접두사 제외)
     */
    fun propertyKeys(): Set<String>

    /**
     * 이 서버의 연결 정보를 키-값 맵으로 반환합니다.
     *
     * @return 프로퍼티 키-값 [Map] (네임스페이스 접두사 제외)
     * @throws IllegalStateException `start()` 호출 전에 사용 시 발생
     */
    fun properties(): Map<String, String>

    /**
     * 이 서버의 연결 정보를 시스템 프로퍼티로 등록하고,
     * 등록 해제를 위한 [AutoCloseable]을 반환합니다.
     *
     * `close()` 시 이전 프로퍼티 값으로 복원합니다.
     *
     * 사용 예시:
     * ```kotlin
     * val registration = server.registerSystemProperties()
     * // ... 테스트 실행
     * registration.close()  // 또는 use { ... } 블록
     * ```
     *
     * @return 프로퍼티 복원을 위한 [AutoCloseable]
     */
    fun registerSystemProperties(): AutoCloseable {
        val prefix = "$SERVER_PREFIX.$propertyNamespace"
        val previousValues = mutableMapOf<String, String?>()
        val props = properties()

        props.forEach { (key, value) ->
            val fullKey = "$prefix.$key"
            previousValues[fullKey] = System.getProperty(fullKey)
            System.setProperty(fullKey, value)
        }

        return AutoCloseable {
            previousValues.forEach { (fullKey, prevValue) ->
                if (prevValue == null) {
                    System.clearProperty(fullKey)
                } else {
                    System.setProperty(fullKey, prevValue)
                }
            }
        }
    }

    /**
     * 이 서버의 연결 정보를 시스템 프로퍼티로 등록합니다.
     *
     * 해제가 필요하지 않은 singleton 서버에 적합합니다.
     * 해제가 필요한 경우 [registerSystemProperties]를 사용하세요.
     */
    fun writeToSystemProperties() {
        val prefix = "$SERVER_PREFIX.$propertyNamespace"
        properties().forEach { (key, value) ->
            System.setProperty("$prefix.$key", value)
        }
    }
}

/**
 * 비-JDBC 서버의 키 명명 변경 시 구 키도 함께 등록하기 위한 유틸리티입니다.
 *
 * [mapping]의 새 키가 현재 맵에 있으면 구 키도 같은 값으로 추가합니다.
 *
 * 사용 예시 (KafkaServer):
 * ```kotlin
 * override fun properties() = mapOf(
 *     "bootstrap.servers" to bootstrapServers,
 *     "bound.port.numbers" to boundPortNumbers.joinToString(",")
 * ).withCompatKeys(mapOf(
 *     "bootstrap.servers" to "bootstrapServers",
 *     "bound.port.numbers" to "boundPortNumbers"
 * ))
 * ```
 *
 * @param mapping 새 키 → 구 키 매핑. 새 키가 맵에 있으면 구 키도 같은 값으로 추가
 * @return 구 키가 추가된 새 맵
 */
fun Map<String, String>.withCompatKeys(mapping: Map<String, String>): Map<String, String> {
    val result = this.toMutableMap()
    mapping.forEach { (newKey, oldKey) ->
        result[newKey]?.let { result[oldKey] = it }
    }
    return result
}
