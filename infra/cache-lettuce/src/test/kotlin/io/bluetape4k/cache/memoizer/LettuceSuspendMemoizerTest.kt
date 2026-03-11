package io.bluetape4k.cache.memoizer

import io.bluetape4k.cache.RedisServers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.codec.LettuceIntCodec
import io.bluetape4k.redis.lettuce.codec.LettuceLongCodec
import io.bluetape4k.redis.lettuce.map.LettuceSuspendMap
import io.lettuce.core.codec.RedisCodec

class LettuceSuspendMemoizerTest: AbstractSuspendMemoizerTest() {

    companion object: KLogging() {
        private fun <V: Any> newMap(
            codec: RedisCodec<String, V>,
            name: String,
        ): LettuceSuspendMap<V> = LettuceSuspendMap(
            LettuceClients.connect(RedisServers.redisClient, codec),
            name
        )
    }

    private val heavyMap = newMap(LettuceIntCodec, "memoizer:lettuce:suspend:heavy")

    override val heavyFunc: suspend (Int) -> Int = heavyMap.suspendMemoizer { x ->
        Thread.sleep(100)
        x * x
    }

    override val factorial: SuspendFactorialProvider = object: SuspendFactorialProvider {
        override val cachedCalc: suspend (Long) -> Long =
            newMap(LettuceLongCodec, "memoizer:lettuce:suspend:factorial")
                .suspendMemoizer { calc(it) }
    }

    override val fibonacci: SuspendFibonacciProvider = object: SuspendFibonacciProvider {
        override val cachedCalc: suspend (Long) -> Long =
            newMap(LettuceLongCodec, "memoizer:lettuce:suspend:fibonacci")
                .suspendMemoizer { calc(it) }
    }
}
