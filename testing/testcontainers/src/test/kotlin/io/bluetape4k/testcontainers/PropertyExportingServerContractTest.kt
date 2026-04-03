package io.bluetape4k.testcontainers

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldNotThrow
import org.junit.jupiter.api.Test

/**
 * [PropertyExportingServer] 계약 테스트.
 *
 * 모든 서버 구현체가 계약을 올바르게 따르는지 검증합니다.
 * Docker 없이 실행 가능한 정적 검증만 포함합니다.
 */
class PropertyExportingServerContractTest {

    companion object : KLogging()

    /**
     * 테스트용 Mock [PropertyExportingServer] 구현체.
     * Docker 컨테이너 없이 순수 JVM에서 실행됩니다.
     */
    private inner class MockServer(
        override val propertyNamespace: String,
        private val keys: Set<String>,
        private val props: Map<String, String>,
    ) : PropertyExportingServer {
        override fun propertyKeys(): Set<String> = keys
        override fun properties(): Map<String, String> = props
    }

    /**
     * [PropertyExportingServer.propertyNamespace]는 소문자와 점 또는 하이픈만 포함해야 합니다.
     * camelCase나 대문자는 허용되지 않습니다.
     */
    @Test
    fun `propertyNamespace 는 소문자와 점 또는 하이픈만 포함해야 한다`() {
        val validNamespaces = listOf(
            "redis",
            "my-server",
            "database.postgres",
            "kafka-broker",
            "aws.localstack",
        )
        val invalidNamespaces = listOf(
            "Redis",
            "myServer",
            "MY_SERVER",
            "dataBase",
        )

        validNamespaces.forEach { ns ->
            ns.matches(Regex("[a-z][a-z0-9.\\-]*")).shouldBeTrue()
        }

        invalidNamespaces.forEach { ns ->
            ns.matches(Regex("[a-z][a-z0-9.\\-]*")).shouldBeFalse()
        }
    }

    /**
     * [PropertyExportingServer.propertyKeys]는 dot-separated lowercase 형식이어야 합니다.
     * compat 키(camelCase 형식의 구 키)는 검증에서 제외합니다.
     */
    @Test
    fun `propertyKeys 는 dot-separated lowercase 형식이어야 한다`() {
        val server = MockServer(
            propertyNamespace = "test",
            keys = setOf("host", "port", "bootstrap.servers", "bound.port.numbers"),
            props = mapOf(
                "host" to "localhost",
                "port" to "5432",
                "bootstrap.servers" to "localhost:9093",
                "bound.port.numbers" to "9093",
            ),
        )

        // camelCase 패턴 판별 함수: camelCase이면 compat 키로 간주하여 검증 제외
        fun isCamelCase(key: String): Boolean = key.any { it.isUpperCase() }

        val nonCompatKeys = server.propertyKeys().filterNot { isCamelCase(it) }

        // 비-compat 키는 모두 dot-separated lowercase 형식이어야 합니다
        nonCompatKeys.forEach { key ->
            key.matches(Regex("[a-z][a-z0-9.\\-]*")).shouldBeTrue()
        }
    }

    /**
     * [PropertyExportingServer.propertyKeys]는 start() 호출 없이도 예외 없이 실행 가능합니다.
     */
    @Test
    fun `propertyKeys 는 start 없이도 호출 가능하다`() {
        val server = MockServer(
            propertyNamespace = "test",
            keys = setOf("host", "port", "url"),
            props = emptyMap(),
        )

        val callPropertyKeys = { server.propertyKeys() }
        callPropertyKeys shouldNotThrow Exception::class
    }

    /**
     * [PropertyExportingServer.registerSystemProperties]는 [AutoCloseable]을 반환합니다.
     * close() 호출 시 예외가 발생하지 않아야 합니다.
     */
    @Test
    fun `registerSystemProperties 는 AutoCloseable 을 반환한다`() {
        val server = MockServer(
            propertyNamespace = "test-contract",
            keys = setOf("host", "port"),
            props = mapOf("host" to "localhost", "port" to "5432"),
        )

        val registration = server.registerSystemProperties()

        registration.shouldNotBeNull()

        val closeAction = { registration.close() }
        closeAction shouldNotThrow Exception::class

        // 정리
        System.clearProperty("$SERVER_PREFIX.test-contract.host")
        System.clearProperty("$SERVER_PREFIX.test-contract.port")
    }

    /**
     * [PropertyExportingServer.writeToSystemProperties]는
     * `testcontainers.{namespace}.{key}` 형식으로 시스템 프로퍼티를 등록합니다.
     */
    @Test
    fun `writeToSystemProperties 는 올바른 키 형식으로 등록한다`() {
        val namespace = "contract-write-test"
        val server = MockServer(
            propertyNamespace = namespace,
            keys = setOf("host", "port"),
            props = mapOf("host" to "myhost", "port" to "1234"),
        )

        server.writeToSystemProperties()

        try {
            System.getProperty("$SERVER_PREFIX.$namespace.host") shouldBeEqualTo "myhost"
            System.getProperty("$SERVER_PREFIX.$namespace.port") shouldBeEqualTo "1234"
        } finally {
            System.clearProperty("$SERVER_PREFIX.$namespace.host")
            System.clearProperty("$SERVER_PREFIX.$namespace.port")
        }
    }

    /**
     * [PropertyExportingServer.registerSystemProperties] 후 close() 하면
     * 등록된 프로퍼티가 null로 복원됩니다.
     */
    @Test
    fun `registerSystemProperties 후 close 하면 프로퍼티가 null 로 복원된다`() {
        val namespace = "contract-restore-test"
        val server = MockServer(
            propertyNamespace = namespace,
            keys = setOf("host"),
            props = mapOf("host" to "temphost"),
        )

        val registration = server.registerSystemProperties()
        System.getProperty("$SERVER_PREFIX.$namespace.host") shouldBeEqualTo "temphost"

        registration.close()
        System.getProperty("$SERVER_PREFIX.$namespace.host").shouldBeNull()
    }

    /**
     * 주요 서버 클래스들이 [PropertyExportingServer]를 구현하는지 검증합니다.
     * Reflection 기반으로 Docker 없이 실행됩니다.
     */
    @Test
    fun `주요 서버 클래스들이 PropertyExportingServer 를 구현한다`() {
        val expectedImplementors = listOf(
            "io.bluetape4k.testcontainers.database.PostgreSQLServer",
            "io.bluetape4k.testcontainers.storage.RedisServer",
            "io.bluetape4k.testcontainers.mq.KafkaServer",
            "io.bluetape4k.testcontainers.aws.LocalStackServer",
            "io.bluetape4k.testcontainers.storage.MongoDBServer",
            "io.bluetape4k.testcontainers.mq.RabbitMQServer",
            "io.bluetape4k.testcontainers.database.MySQL8Server",
            "io.bluetape4k.testcontainers.database.MariaDBServer",
        )

        val contractInterface = PropertyExportingServer::class.java

        expectedImplementors.forEach { className ->
            val clazz = Class.forName(className)
            contractInterface.isAssignableFrom(clazz).shouldBeTrue()
        }
    }

    /**
     * [PropertyExportingServer.propertyKeys]와 [PropertyExportingServer.properties] 키 집합이 일치합니다.
     */
    @Test
    fun `propertyKeys 와 properties 의 키 집합이 일치한다`() {
        val keys = setOf("host", "port", "url")
        val props = mapOf("host" to "localhost", "port" to "5432", "url" to "http://localhost:5432")

        val server = MockServer(
            propertyNamespace = "test",
            keys = keys,
            props = props,
        )

        server.propertyKeys() shouldBeEqualTo props.keys
    }

    /**
     * 빈 프로퍼티를 가진 서버에서 [PropertyExportingServer.registerSystemProperties]를
     * 호출해도 예외가 발생하지 않습니다.
     */
    @Test
    fun `빈 properties 를 가진 서버는 registerSystemProperties 가 정상 동작한다`() {
        val server = MockServer(
            propertyNamespace = "empty-server",
            keys = emptySet(),
            props = emptyMap(),
        )

        val callRegister = { server.registerSystemProperties().close() }
        callRegister shouldNotThrow Exception::class
    }

    /**
     * [PropertyExportingServer.withCompatKeys] 유틸리티는 구 키를 새 키와 함께 맵에 추가합니다.
     */
    @Test
    fun `withCompatKeys 는 구 키를 현재 맵에 추가한다`() {
        val original = mapOf(
            "bootstrap.servers" to "localhost:9093",
            "bound.port.numbers" to "9093",
        )

        val result = original.withCompatKeys(
            mapOf(
                "bootstrap.servers" to "bootstrapServers",
                "bound.port.numbers" to "boundPortNumbers",
            )
        )

        result["bootstrap.servers"] shouldBeEqualTo "localhost:9093"
        result["bootstrapServers"] shouldBeEqualTo "localhost:9093"
        result["bound.port.numbers"] shouldBeEqualTo "9093"
        result["boundPortNumbers"] shouldBeEqualTo "9093"
    }

    /**
     * [PropertyExportingServer.withCompatKeys]에서 존재하지 않는 새 키는 구 키를 추가하지 않습니다.
     */
    @Test
    fun `withCompatKeys 에서 새 키가 없으면 구 키는 추가되지 않는다`() {
        val original = mapOf("host" to "localhost")

        val result = original.withCompatKeys(
            mapOf("bootstrap.servers" to "bootstrapServers")
        )

        result.containsKey("bootstrapServers").shouldBeFalse()
        result["host"] shouldBeEqualTo "localhost"
    }
}
