package io.bluetape4k.redis.lettuce

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.LettuceTestUtils.commands
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE

class LettuceClientsTest: AbstractLettuceTest() {

    companion object: KLogging()

    @Test
    fun `connect to redis server`() {
        val result = commands.ping()
        result shouldBeEqualTo "PONG"
    }

    @RepeatedTest(10)
    fun `connect to redis server repeatly`() {
        val commands = LettuceClients.commands(client, LettuceBinaryCodecs.default())
        commands.ping() shouldBeEqualTo "PONG"
    }

    @Test
    fun `reuse same connection for same client and codec`() {
        val conn1 = LettuceClients.connect(client)
        val conn2 = LettuceClients.connect(client)
        (conn1 === conn2).shouldBeTrue()

        val codec = LettuceBinaryCodecs.default<Any>()
        val typedConn1 = LettuceClients.connect(client, codec)
        val typedConn2 = LettuceClients.connect(client, codec)
        (typedConn1 === typedConn2).shouldBeTrue()
    }

    @Test
    fun `connect to redis server in multi-threading`() {
        MultithreadingTester()
            .workers(8)
            .rounds(4)
            .add {
                val commands = LettuceClients.commands(client, LettuceBinaryCodecs.default())
                commands.ping() shouldBeEqualTo "PONG"
            }
            .run()
    }

    @EnabledOnJre(JRE.JAVA_21, JRE.JAVA_25)
    @Test
    fun `connect to redis server in virtual threads`() {
        StructuredTaskScopeTester()
            .rounds(32)
            .add {
                val asyncCommands = LettuceClients.asyncCommands(client, LettuceBinaryCodecs.default())
                asyncCommands.ping().get() shouldBeEqualTo "PONG"
            }
            .run()
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    @Test
    fun `connect to redis server in coroutines`() = runSuspendIO {
        SuspendedJobTester()
            .rounds(32)
            .add {
                val coroutinesCommand = LettuceClients.coroutinesCommands(client, LettuceBinaryCodecs.default())
                coroutinesCommand.ping() shouldBeEqualTo "PONG"
            }
            .run()
    }
}
