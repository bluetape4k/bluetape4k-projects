package io.bluetape4k.redis.lettuce

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

class LettuceClientsTest: AbstractLettuceTest() {

    companion object: KLogging()

    @Test
    fun `connect to redis server`() {
        val result = commands.ping()
        result shouldBeEqualTo "PONG"
    }

    @RepeatedTest(10)
    fun `connect to redis server repeatly`() {
        val commands = LettuceClients.commands(client, LettuceBinaryCodecs.Default)
        commands.ping() shouldBeEqualTo "PONG"
    }

    @Test
    fun `connect to redis server in multi-threading`() {
        MultithreadingTester()
            .numThreads(16)
            .roundsPerThread(2)
            .add {
                val commands = LettuceClients.commands(client, LettuceBinaryCodecs.Default)
                commands.ping() shouldBeEqualTo "PONG"
            }
            .run()
    }

    @Test
    fun `connect to redis server in virtual threads`() {
        StructuredTaskScopeTester()
            .roundsPerTask(32)
            .add {
                val asyncCommands = LettuceClients.asyncCommands(client, LettuceBinaryCodecs.Default)
                asyncCommands.ping().get() shouldBeEqualTo "PONG"
            }
            .run()
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    @Test
    fun `connect to redis server in coroutines`() = runSuspendIO {
        SuspendedJobTester()
            .numThreads(16)
            .roundsPerJob(32)
            .add {
                val coroutinesCommand = LettuceClients.coroutinesCommands(client, LettuceBinaryCodecs.Default)
                coroutinesCommand.ping() shouldBeEqualTo "PONG"
            }
            .run()
    }
}
