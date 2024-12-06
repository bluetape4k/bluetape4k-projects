package io.bluetape4k.quarkus.tests.resources

import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.quarkus.redis.datasource.ReactiveRedisDataSource
import io.quarkus.redis.datasource.RedisDataSource
import io.quarkus.test.junit.QuarkusTest
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.inject.Inject
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

@QuarkusTest
class RedisClientTest {

    companion object: KLogging()

    @Inject
    internal lateinit var redisDataSource: RedisDataSource

    @Inject
    internal lateinit var reactiveRedisDataSource: ReactiveRedisDataSource

    @Test
    fun `access to redis by key`() {
        val key = Fakers.randomUuid().encodeBase62()
        val value = Fakers.randomString()

        val commands = redisDataSource.value(String::class.java)
        commands.set(key, value)

        val saved = commands.get(key).toString()
        saved shouldBeEqualTo value
    }

    @Test
    fun `access to redis by key as reactive`() = runTest {
        val key = Fakers.randomUuid().encodeBase62()
        val value = Fakers.randomString()

        val commands = reactiveRedisDataSource.value(String::class.java)
        commands.set(key, value).awaitSuspending()

        val saved = commands.get(key).awaitSuspending().toString()
        saved shouldBeEqualTo value
    }
}
