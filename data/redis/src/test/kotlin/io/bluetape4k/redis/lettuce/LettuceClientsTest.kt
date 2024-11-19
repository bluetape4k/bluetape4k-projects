package io.bluetape4k.redis.lettuce

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
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
}
