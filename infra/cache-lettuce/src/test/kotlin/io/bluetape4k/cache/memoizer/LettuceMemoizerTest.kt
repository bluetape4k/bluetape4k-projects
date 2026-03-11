package io.bluetape4k.cache.memoizer

import io.bluetape4k.cache.RedisServers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.codec.LettuceIntCodec
import io.bluetape4k.redis.lettuce.codec.LettuceLongCodec
import io.bluetape4k.redis.lettuce.map.LettuceMap
import io.lettuce.core.codec.RedisCodec

class LettuceMemoizerTest: AbstractMemoizerTest() {

    companion object: KLogging() {
        private fun <V: Any> newMap(
            name: String,
            codec: RedisCodec<String, V>,
        ): LettuceMap<V> = LettuceMap(
            LettuceClients.connect(RedisServers.redisClient, codec),
            name
        )
    }

    private val heavyMap = newMap("memoizer:lettuce:heavy", LettuceIntCodec).apply { clear() }

    override val heavyFunc: (Int) -> Int = heavyMap.memoizer { x ->
        Thread.sleep(100)
        x * x
    }

    override val factorial: FactorialProvider = object: FactorialProvider {
        override val cachedCalc: (Long) -> Long =
            newMap("memoizer:lettuce:factorial", LettuceLongCodec)
                .memoizer { calc(it) }
    }

    override val fibonacci: FibonacciProvider = object: FibonacciProvider {
        override val cachedCalc: (Long) -> Long =
            newMap("memoizer:lettuce:fibonacci", LettuceLongCodec)
                .memoizer { calc(it) }
    }
}
