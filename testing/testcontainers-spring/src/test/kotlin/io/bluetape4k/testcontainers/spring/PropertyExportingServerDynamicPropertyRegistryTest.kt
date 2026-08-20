package io.bluetape4k.testcontainers.spring

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.testcontainers.PropertyExportingServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.function.Supplier

/**
 * [PropertyExportingServer.registerDynamicProperties]의 Spring bridge 계약 테스트.
 *
 * Docker 없이 fake server와 recording registry만 사용해 key mapping, lazy supplier,
 * 예외 전달 및 시스템 프로퍼티 비변경을 검증합니다.
 */
class PropertyExportingServerDynamicPropertyRegistryTest {

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerReadmeStyleProperties(registry: DynamicPropertyRegistry) {
            FakeServer(
                propertyNamespace = "redis",
                keys = setOf("host"),
                values = mapOf("host" to "localhost"),
            ).registerDynamicProperties(registry)
        }
    }

    private val systemPropertyKey = "testcontainers.bridge-contract.host"

    @AfterEach
    fun cleanupSystemProperty() {
        System.clearProperty(systemPropertyKey)
    }

    @Test
    fun `propertyKeys 를 full Spring property key 로 등록한다`() {
        val server = FakeServer(
            propertyNamespace = "redis",
            keys = linkedSetOf("host", "port", "url"),
            values = mapOf(
                "host" to "localhost",
                "port" to "6379",
                "url" to "redis://localhost:6379",
            ),
        )
        val registry = RecordingRegistry()

        server.registerDynamicProperties(registry)

        registry.names shouldBeEqualTo listOf(
            "testcontainers.redis.host",
            "testcontainers.redis.port",
            "testcontainers.redis.url",
        )
        server.propertiesCalls shouldBeEqualTo 0
        registry.value("testcontainers.redis.host") shouldBeEqualTo "localhost"
        registry.value("testcontainers.redis.port") shouldBeEqualTo "6379"
        registry.value("testcontainers.redis.url") shouldBeEqualTo "redis://localhost:6379"
    }

    @Test
    fun `빈 propertyKeys 는 registry 를 변경하지 않는다`() {
        val registry = RecordingRegistry()

        FakeServer(keys = emptySet(), values = emptyMap()).registerDynamicProperties(registry)

        registry.names shouldBeEqualTo emptyList()
    }

    @Test
    fun `supplier 는 등록 시점이 아니라 값 해석 시 properties 를 호출한다`() {
        val server = FakeServer(
            keys = setOf("host"),
            values = mapOf("host" to "before"),
        )
        val registry = RecordingRegistry()

        server.registerDynamicProperties(registry)
        server.propertiesCalls shouldBeEqualTo 0

        server.values = mapOf("host" to "after")
        registry.value("testcontainers.bridge-contract.host") shouldBeEqualTo "after"
        server.propertiesCalls shouldBeEqualTo 1

        server.values = mapOf("host" to "latest")
        registry.value("testcontainers.bridge-contract.host") shouldBeEqualTo "latest"
        server.propertiesCalls shouldBeEqualTo 2
    }

    @Test
    fun `propertyKeys 에 선언된 키가 properties 에 없으면 supplier 평가가 실패한다`() {
        val server = FakeServer(
            keys = setOf("host", "port"),
            values = mapOf("host" to "localhost"),
        )
        val registry = RecordingRegistry()

        server.registerDynamicProperties(registry)

        val error = assertFailsWith<IllegalStateException> {
            registry.value("testcontainers.bridge-contract.port")
        }

        error.message shouldBeEqualTo
            "PropertyExportingServer 'bridge-contract' did not provide property 'port'"
    }

    @Test
    fun `properties 예외는 원래 타입과 메시지로 전달된다`() {
        val server = FakeServer(
            keys = setOf("host"),
            values = emptyMap(),
            propertiesFailure = IllegalArgumentException("server is not running"),
        )
        val registry = RecordingRegistry()

        server.registerDynamicProperties(registry)

        val error = assertFailsWith<IllegalArgumentException> {
            registry.value("testcontainers.bridge-contract.host")
        }

        error.message shouldBeEqualTo "server is not running"
    }

    @Test
    fun `등록 전후 시스템 프로퍼티를 변경하지 않는다`() {
        System.setProperty(systemPropertyKey, "existing")
        val registry = RecordingRegistry()

        FakeServer(
            keys = setOf("host"),
            values = mapOf("host" to "replacement"),
        ).registerDynamicProperties(registry)

        System.getProperty(systemPropertyKey) shouldBeEqualTo "existing"
    }

    @Test
    fun `중복 등록 정책을 별도로 덮어쓰지 않고 registry 호출에 위임한다`() {
        val server = FakeServer(
            keys = setOf("host"),
            values = mapOf("host" to "localhost"),
        )
        val registry = RecordingRegistry()

        server.registerDynamicProperties(registry)
        server.registerDynamicProperties(registry)

        registry.names shouldBeEqualTo listOf(
            "testcontainers.bridge-contract.host",
            "testcontainers.bridge-contract.host",
        )
        registry.valueAt(0) shouldBeEqualTo "localhost"
        registry.valueAt(1) shouldBeEqualTo "localhost"
    }

    private class FakeServer(
        override val propertyNamespace: String = "bridge-contract",
        private val keys: Set<String>,
        var values: Map<String, String>,
        private val propertiesFailure: RuntimeException? = null,
    ): PropertyExportingServer {
        var propertiesCalls: Int = 0
            private set

        override fun propertyKeys(): Set<String> = keys

        override fun properties(): Map<String, String> {
            propertiesCalls++
            propertiesFailure?.let { throw it }
            return values
        }
    }

    private class RecordingRegistry: DynamicPropertyRegistry {
        private val entries = mutableListOf<Pair<String, Supplier<Any>>>()

        val names: List<String>
            get() = entries.map { it.first }

        override fun add(name: String, valueSupplier: Supplier<Any>) {
            entries += name to valueSupplier
        }

        fun value(name: String): Any = entries.single { it.first == name }.second.get()

        fun valueAt(index: Int): Any = entries[index].second.get()
    }
}
