package io.bluetape4k.grpc.examples.helloworld

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GreeterServiceTest {
    companion object: KLogging() {
        private const val PROCESS_NAME = "greeter.service"
    }

    private val server = GreeterServer(PROCESS_NAME)
    private val client = GreeterClient(PROCESS_NAME)

    @BeforeAll
    fun setup() {
        server.start()
    }

    @AfterAll
    fun cleanup() {
        client.close()
        server.close()
    }

    @Test
    fun `서버가 시작된 후 isRunning이 true여야 한다`() {
        server.isRunning.shouldBeTrue()
    }

    @Test
    fun `서버가 종료된 후 isShutdown이 true여야 한다`() {
        val tempServer = GreeterServer("greeter.service.shutdown.test")
        tempServer.start()
        tempServer.isRunning.shouldBeTrue()
        tempServer.stop()
        tempServer.isShutdown.shouldBeTrue()
        tempServer.isRunning.shouldBeFalse()
    }

    @Test
    fun `일반 이름으로 sayHello 호출 시 인사 메시지를 반환해야 한다`() =
        runSuspendTest {
            val message = client.sayHello("Debop")
            message shouldBeEqualTo "Hello Debop"
        }

    @Test
    fun `빈 문자열 이름으로 sayHello 호출 시 빈 이름 인사를 반환해야 한다`() =
        runSuspendTest {
            val message = client.sayHello("")
            message shouldBeEqualTo "Hello "
        }

    @Test
    fun `공백 이름으로 sayHello 호출 시 공백을 포함한 인사를 반환해야 한다`() =
        runSuspendTest {
            val message = client.sayHello("  ")
            message shouldBeEqualTo "Hello   "
        }

    @Test
    fun `여러 번 연속 호출해도 정상 동작해야 한다`() =
        runSuspendTest {
            val names = listOf("Alice", "Bob", "Charlie")
            names.forEach { name ->
                val message = client.sayHello(name)
                message shouldBeEqualTo "Hello $name"
            }
        }

    @Test
    fun `응답 메시지가 비어있지 않아야 한다`() =
        runSuspendTest {
            val message = client.sayHello("World")
            message.shouldNotBeEmpty()
        }

    @Test
    fun `close를 여러 번 호출해도 예외가 발생하지 않아야 한다`() {
        val tempClient = GreeterClient("greeter.service")
        tempClient.close()
        // 두 번째 close는 채널이 이미 shutdown되어 있으므로 아무 동작도 하지 않아야 한다
        tempClient.close()
    }

    @Test
    fun `GreeterServer는 빈 이름으로 생성 시 예외를 던져야 한다`() {
        assertThrows<IllegalArgumentException> {
            GreeterServer("")
        }
    }

    @Test
    fun `GreeterClient는 빈 이름으로 생성 시 예외를 던져야 한다`() {
        assertThrows<IllegalArgumentException> {
            GreeterClient("")
        }
    }
}
