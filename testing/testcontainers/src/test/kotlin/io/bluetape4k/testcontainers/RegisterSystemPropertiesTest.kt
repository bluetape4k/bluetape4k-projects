package io.bluetape4k.testcontainers

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotThrow
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [PropertyExportingServer.registerSystemProperties] 메서드에 대한 단위 테스트.
 *
 * Docker 없이 순수 JVM에서 실행되며, Mock 구현체를 사용합니다.
 */
class RegisterSystemPropertiesTest {

    companion object : KLogging() {
        private const val NAMESPACE = "mock"
        private val FULL_PREFIX = "$SERVER_PREFIX.$NAMESPACE"
    }

    /**
     * 테스트용 Mock [PropertyExportingServer] 구현체.
     * Docker 컨테이너 없이 순수 JVM에서 실행됩니다.
     */
    private inner class MockServer(
        override val propertyNamespace: String = NAMESPACE,
    ) : PropertyExportingServer {
        override fun propertyKeys() = setOf("host", "port", "url")
        override fun properties() = mapOf(
            "host" to "localhost",
            "port" to "5432",
            "url" to "http://localhost:5432",
        )
    }

    @BeforeEach
    fun cleanupBefore() {
        System.clearProperty("$FULL_PREFIX.host")
        System.clearProperty("$FULL_PREFIX.port")
        System.clearProperty("$FULL_PREFIX.url")
    }

    @AfterEach
    fun cleanupAfter() {
        System.clearProperty("$FULL_PREFIX.host")
        System.clearProperty("$FULL_PREFIX.port")
        System.clearProperty("$FULL_PREFIX.url")
    }

    @Test
    fun `registerSystemProperties 는 프로퍼티를 시스템에 등록한다`() {
        val server = MockServer()
        val registration = server.registerSystemProperties()

        System.getProperty("$FULL_PREFIX.host") shouldBeEqualTo "localhost"
        System.getProperty("$FULL_PREFIX.port") shouldBeEqualTo "5432"
        System.getProperty("$FULL_PREFIX.url") shouldBeEqualTo "http://localhost:5432"

        registration.close()
    }

    @Test
    fun `close 후 존재하지 않던 프로퍼티는 null 로 복원된다`() {
        // 사전 조건: 프로퍼티가 없는 상태
        System.getProperty("$FULL_PREFIX.host").shouldBeNull()

        val server = MockServer()
        val registration = server.registerSystemProperties()

        // 등록 확인
        System.getProperty("$FULL_PREFIX.host") shouldBeEqualTo "localhost"

        // 해제 후 null로 복원
        registration.close()
        System.getProperty("$FULL_PREFIX.host").shouldBeNull()
        System.getProperty("$FULL_PREFIX.port").shouldBeNull()
        System.getProperty("$FULL_PREFIX.url").shouldBeNull()
    }

    @Test
    fun `close 후 기존 값이 있던 프로퍼티는 이전 값으로 복원된다`() {
        // 사전 조건: 기존 값 설정
        System.setProperty("$FULL_PREFIX.host", "previous-host")
        System.setProperty("$FULL_PREFIX.port", "9999")

        val server = MockServer()
        val registration = server.registerSystemProperties()

        // 등록 후 새 값 확인
        System.getProperty("$FULL_PREFIX.host") shouldBeEqualTo "localhost"
        System.getProperty("$FULL_PREFIX.port") shouldBeEqualTo "5432"

        // 해제 후 이전 값으로 복원
        registration.close()
        System.getProperty("$FULL_PREFIX.host") shouldBeEqualTo "previous-host"
        System.getProperty("$FULL_PREFIX.port") shouldBeEqualTo "9999"
        System.getProperty("$FULL_PREFIX.url").shouldBeNull()
    }

    @Test
    fun `close 를 두 번 호출해도 예외가 발생하지 않는다 (idempotent)`() {
        val server = MockServer()
        val registration = server.registerSystemProperties()

        val closeAction = { registration.close() }

        closeAction shouldNotThrow Exception::class
        closeAction shouldNotThrow Exception::class
    }

    @Test
    fun `use 블록 패턴으로 사용하면 블록 종료 후 프로퍼티가 복원된다`() {
        System.getProperty("$FULL_PREFIX.host").shouldBeNull()

        MockServer().registerSystemProperties().use {
            System.getProperty("$FULL_PREFIX.host") shouldBeEqualTo "localhost"
        }

        // use 블록 종료 후 복원 확인
        System.getProperty("$FULL_PREFIX.host").shouldBeNull()
    }

    @Test
    fun `BeforeEach AfterEach 패턴으로 사용할 수 있다`() {
        // @BeforeEach 에서 등록, @AfterEach 에서 해제하는 패턴 시뮬레이션
        val server = MockServer()
        lateinit var registration: AutoCloseable

        // @BeforeEach 역할
        registration = server.registerSystemProperties()
        System.getProperty("$FULL_PREFIX.host") shouldBeEqualTo "localhost"

        // 테스트 로직
        System.getProperty("$FULL_PREFIX.port") shouldBeEqualTo "5432"

        // @AfterEach 역할
        registration.close()
        System.getProperty("$FULL_PREFIX.host").shouldBeNull()
    }

    @Test
    fun `네임스페이스가 다른 두 서버는 서로 간섭하지 않는다`() {
        val server1 = MockServer(propertyNamespace = "mock-1")
        val server2 = object : PropertyExportingServer {
            override val propertyNamespace = "mock-2"
            override fun propertyKeys() = setOf("host")
            override fun properties() = mapOf("host" to "remote-host")
        }

        val reg1 = server1.registerSystemProperties()
        val reg2 = server2.registerSystemProperties()

        System.getProperty("$SERVER_PREFIX.mock-1.host") shouldBeEqualTo "localhost"
        System.getProperty("$SERVER_PREFIX.mock-2.host") shouldBeEqualTo "remote-host"

        reg1.close()
        System.getProperty("$SERVER_PREFIX.mock-1.host").shouldBeNull()
        System.getProperty("$SERVER_PREFIX.mock-2.host") shouldBeEqualTo "remote-host"

        reg2.close()
        System.getProperty("$SERVER_PREFIX.mock-2.host").shouldBeNull()

        // 정리
        System.clearProperty("$SERVER_PREFIX.mock-1.host")
        System.clearProperty("$SERVER_PREFIX.mock-2.host")
    }
}
