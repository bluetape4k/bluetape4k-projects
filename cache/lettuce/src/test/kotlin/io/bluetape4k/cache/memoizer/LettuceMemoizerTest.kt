package io.bluetape4k.cache.memoizer

import io.bluetape4k.cache.RedisServers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.codec.LettuceIntCodec
import io.bluetape4k.redis.lettuce.codec.LettuceLongCodec
import io.bluetape4k.redis.lettuce.map.LettuceMap
class LettuceMemoizerTest: AbstractMemoizerTest() {

    companion object: KLogging() {
        private val intConnection by lazy { LettuceClients.connect(RedisServers.redisClient, LettuceIntCodec) }
        private val longConnection by lazy { LettuceClients.connect(RedisServers.redisClient, LettuceLongCodec) }
    }

    private val heavyMap = LettuceMap<Int>(intConnection, "memoizer:lettuce:heavy").apply { clear() }

    override val heavyFunc: (Int) -> Int = heavyMap.memoizer { x ->
        Thread.sleep(100)
        x * x
    }

    override val factorial: FactorialProvider = object: FactorialProvider {
        override val cachedCalc: (Long) -> Long =
            LettuceMap<Long>(longConnection, "memoizer:lettuce:factorial")
                .memoizer { calc(it) }
    }

    override val fibonacci: FibonacciProvider = object: FibonacciProvider {
        override val cachedCalc: (Long) -> Long =
            LettuceMap<Long>(longConnection, "memoizer:lettuce:fibonacci")
                .memoizer { calc(it) }
    }
}
