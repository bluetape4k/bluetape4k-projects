package io.bluetape4k.redis.lettuce

import io.bluetape4k.redis.AbstractRedisTest
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.lettuce.core.api.sync.RedisCommands
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll

@OptIn(ExperimentalLettuceCoroutinesApi::class)
abstract class AbstractLettuceTest: AbstractRedisTest() {

    protected lateinit var client: RedisClient

    protected lateinit var commands: RedisCommands<String, Any>
    protected lateinit var asyncCommands: RedisAsyncCommands<String, Any>
    protected lateinit var suspendCommands: RedisCoroutinesCommands<String, Any>

    @BeforeAll
    open fun beforeAll() {
        client = LettuceClients.clientOf(redis.host, redis.port)

        commands = LettuceClients.commands(client, LettuceBinaryCodecs.lz4Fury())
        asyncCommands = LettuceClients.asyncCommands(client, LettuceBinaryCodecs.lz4Fury())
        suspendCommands = LettuceClients.coroutinesCommands(client, LettuceBinaryCodecs.lz4Fury())

    }

    @AfterAll
    open fun afterAll() {
        client.shutdown()
    }
}
