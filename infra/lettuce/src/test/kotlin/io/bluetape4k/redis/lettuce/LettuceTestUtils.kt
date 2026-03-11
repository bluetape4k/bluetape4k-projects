package io.bluetape4k.redis.lettuce

import io.bluetape4k.LibraryName
import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import io.bluetape4k.testcontainers.storage.RedisServer
import io.bluetape4k.utils.ShutdownQueue
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.lettuce.core.api.sync.RedisCommands

@OptIn(ExperimentalLettuceCoroutinesApi::class)
object LettuceTestUtils: KLogging() {

    @JvmStatic
    val redis: RedisServer by lazy { RedisServer.Launcher.redis }

    val client: RedisClient by lazy {
        LettuceClients.clientOf(redis.host, redis.port)
            .apply {
                ShutdownQueue.register { this.shutdown() }
            }
    }

    val commands: RedisCommands<String, Any> by lazy {
        LettuceClients.commands(client, LettuceBinaryCodecs.lz4Fory())
    }
    val asyncCommands: RedisAsyncCommands<String, Any> by lazy {
        LettuceClients.asyncCommands(client, LettuceBinaryCodecs.lz4Fory())
    }
    val coroutinesCommands: RedisCoroutinesCommands<String, Any> by lazy {
        LettuceClients.coroutinesCommands(client, LettuceBinaryCodecs.lz4Fory())
    }

    @JvmStatic
    val faker = Fakers.faker

    @JvmStatic
    fun randomName(): String =
        "${LibraryName}:${Base58.randomString(8)}"

    @JvmStatic
    fun randomString(size: Int = 2048): String =
        Fakers.fixedString(size)
}
