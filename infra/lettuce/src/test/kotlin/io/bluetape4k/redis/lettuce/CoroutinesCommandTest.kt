package io.bluetape4k.redis.lettuce

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.RepeatedTest

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class CoroutinesCommandTest: AbstractLettuceTest() {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 3
        private const val ITEM_SIZE = 500
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `hset in coroutines`() = runSuspendIO {
        val keyName = randomName()

        val list = List(ITEM_SIZE) { index ->
            coroutinesCommands.hset(keyName, index.toString(), index)
        }
        list shouldHaveSize ITEM_SIZE

        coroutinesCommands.hlen(keyName)?.toInt() shouldBeEqualTo ITEM_SIZE
        coroutinesCommands.del(keyName) shouldBeEqualTo 1L
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `hset in coroutines async`() = runSuspendIO {
        val keyName = randomName()

        val list = List(ITEM_SIZE) { index ->
            async(Dispatchers.IO) {
                coroutinesCommands.hset(keyName, index.toString(), index)
            }
        }.awaitAll()

        list shouldHaveSize ITEM_SIZE

        coroutinesCommands.hlen(keyName)?.toInt() shouldBeEqualTo ITEM_SIZE
        coroutinesCommands.del(keyName) shouldBeEqualTo 1L
    }
}
