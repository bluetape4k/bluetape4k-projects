package io.bluetape4k.protobuf.serializers.redis

import io.bluetape4k.LibraryName
import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.testcontainers.storage.RedisServer
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.lettuce.core.api.sync.RedisCommands
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll

@OptIn(ExperimentalLettuceCoroutinesApi::class)
abstract class AbstractLettuceTest {

    companion object: KLogging() {

        @JvmStatic
        val redis: RedisServer by lazy { RedisServer.Launcher.redis }

        @JvmStatic
        val faker = Fakers.faker

        @JvmStatic
        protected fun randomName(): String =
            "${LibraryName}:${Base58.randomString(8)}"

        @JvmStatic
        protected fun randomString(size: Int = 2048): String =
            Fakers.fixedString(size)

    }

    protected lateinit var client: RedisClient

    protected lateinit var commands: RedisCommands<String, Any>
    protected lateinit var asyncCommands: RedisAsyncCommands<String, Any>
    protected lateinit var coroutinesCommands: RedisCoroutinesCommands<String, Any>

    @BeforeAll
    open fun beforeAll() {
        client = LettuceClients.clientOf(redis.host, redis.port)

        commands = LettuceClients.commands(client, LettuceProtobufCodecs.lz4Protobuf())
        asyncCommands = LettuceClients.asyncCommands(client, LettuceProtobufCodecs.lz4Protobuf())
        coroutinesCommands = LettuceClients.coroutinesCommands(client, LettuceProtobufCodecs.lz4Protobuf())
    }

    @AfterAll
    open fun afterAll() {
        runCatching {
            LettuceClients.shutdown(client)
        }
    }
}
