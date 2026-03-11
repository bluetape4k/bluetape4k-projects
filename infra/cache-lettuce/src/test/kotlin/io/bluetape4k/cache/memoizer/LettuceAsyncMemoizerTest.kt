package io.bluetape4k.cache.memoizer

import io.bluetape4k.cache.RedisServers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.codec.LettuceIntCodec
import io.bluetape4k.redis.lettuce.codec.LettuceLongCodec
import io.bluetape4k.redis.lettuce.map.LettuceMap
import io.lettuce.core.codec.RedisCodec
import java.util.concurrent.CompletableFuture

class LettuceAsyncMemoizerTest: AbstractAsyncMemoizerTest() {

    companion object: KLogging() {
        private fun <V: Any> newMap(
            codec: RedisCodec<String, V>,
            name: String,
        ): LettuceMap<V> = LettuceMap(
            LettuceClients.connect(RedisServers.redisClient, codec),
            name
        )
    }

    private val heavyMap = newMap(LettuceIntCodec, "memoizer:lettuce:async:heavy").apply { clear() }

    override val heavyFunc: (Int) -> CompletableFuture<Int> = heavyMap.asyncMemoizer { x ->
        CompletableFuture.supplyAsync {
            Thread.sleep(100)
            x * x
        }
    }

    override val factorial: AsyncFactorialProvider = object: AsyncFactorialProvider {
        override val cachedCalc: (Long) -> CompletableFuture<Long> =
            newMap(LettuceLongCodec, "memoizer:lettuce:async:factorial")
                .asyncMemoizer { calc(it) }
    }

    override val fibonacci: AsyncFibonacciProvider = object: AsyncFibonacciProvider {
        override val cachedCalc: (Long) -> CompletableFuture<Long> =
            newMap(LettuceLongCodec, "memoizer:lettuce:async:fibonacci")
                .asyncMemoizer { calc(it) }
    }
}
