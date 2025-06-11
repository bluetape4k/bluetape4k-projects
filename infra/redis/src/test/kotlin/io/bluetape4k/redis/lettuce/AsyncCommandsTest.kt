package io.bluetape4k.redis.lettuce

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.future.await
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.RepeatedTest

class AsyncCommandsTest: AbstractLettuceTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 3
        private const val ITEM_SIZE = 500
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `bulk put asynchroneously`() = runSuspendIO {
        val keyName = randomName()

        val futures = List(ITEM_SIZE) { index ->
            asyncCommands.hset(keyName, index.toString(), index)
        }
        val list = futures.awaitAll()
        list shouldHaveSize ITEM_SIZE

        asyncCommands.hlen(keyName).await().toInt() shouldBeEqualTo ITEM_SIZE
        asyncCommands.del(keyName).await() shouldBeEqualTo 1L
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `sequence for collection of RedisFuture`() {
        val keyName = randomName()

        val futures = List(ITEM_SIZE) { index ->
            asyncCommands.hset(keyName, index.toString(), index)
        }.sequence()

        val list = futures.get()
        list shouldHaveSize ITEM_SIZE

        asyncCommands.hlen(keyName).get().toInt() shouldBeEqualTo ITEM_SIZE
        asyncCommands.del(keyName).get() shouldBeEqualTo 1L
    }
}
