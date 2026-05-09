package io.bluetape4k.cache.memoizer

import io.bluetape4k.cache.RedisServers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.codec.LettuceIntCodec
import io.bluetape4k.redis.lettuce.codec.LettuceLongCodec
import io.bluetape4k.redis.lettuce.map.LettuceSuspendMap
class LettuceSuspendMemoizerTest: AbstractSuspendMemoizerTest() {

    companion object: KLogging() {
        private val intConnection by lazy { LettuceClients.connect(RedisServers.redisClient, LettuceIntCodec) }
        private val longConnection by lazy { LettuceClients.connect(RedisServers.redisClient, LettuceLongCodec) }
    }

    private val heavyMap = LettuceSuspendMap<Int>(intConnection, "memoizer:lettuce:suspend:heavy")

    override val heavyFunc: suspend (Int) -> Int = heavyMap.suspendMemoizer { x ->
        Thread.sleep(100)
        x * x
    }

    override val factorial: SuspendFactorialProvider = object: SuspendFactorialProvider {
        override val cachedCalc: suspend (Long) -> Long =
            LettuceSuspendMap<Long>(longConnection, "memoizer:lettuce:suspend:factorial")
                .suspendMemoizer { calc(it) }
    }

    override val fibonacci: SuspendFibonacciProvider = object: SuspendFibonacciProvider {
        override val cachedCalc: suspend (Long) -> Long =
            LettuceSuspendMap<Long>(longConnection, "memoizer:lettuce:suspend:fibonacci")
                .suspendMemoizer { calc(it) }
    }
}
