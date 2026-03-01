package io.bluetape4k.exposed.r2dbc.redisson.codecs

import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.UserTable
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.toUserRecord
import io.bluetape4k.exposed.r2dbc.tests.AbstractExposedR2dbcTest
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.RedissonCodecs
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.flushCache
import org.jetbrains.exposed.v1.r2dbc.insertAndGetId
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.redisson.client.codec.ByteArrayCodec
import org.redisson.client.codec.Codec
import org.redisson.client.codec.StringCodec
import org.redisson.client.handler.State
import org.redisson.codec.CborJacksonCodec

/**
 * NOTE: Exposed Entity 는 Redis 의 Codec 으로는 변환이 불가능하다.
 * NOTE: 그래서 ExposedRedisRepository 는 Entity 가 아닌 data class를 캐시하도록 한다.
 */
@Disabled("Exposed Entity 는 직렬화가 불가합니다")
class ExposedEntityRedissonCodecTest: AbstractExposedR2dbcTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 10
        private const val METHOD_SOURCE = "getRedissonBinaryCodecs"
    }

    private fun getRedissonBinaryCodecs() = listOf(
        CborJacksonCodec(),
        ByteArrayCodec(),
        StringCodec(),

        RedissonCodecs.Default,

        RedissonCodecs.Kryo5,
        RedissonCodecs.Fory,
        RedissonCodecs.Jdk,

        RedissonCodecs.Kryo5Composite,
        RedissonCodecs.ForyComposite,
        RedissonCodecs.JdkComposite,

        RedissonCodecs.SnappyKryo5,
        RedissonCodecs.SnappyFory,
        RedissonCodecs.SnappyJdk,

        RedissonCodecs.SnappyKryo5Composite,
        RedissonCodecs.SnappyForyComposite,
        RedissonCodecs.SnappyJdkComposite,

        RedissonCodecs.LZ4Kryo5,
        RedissonCodecs.LZ4Fory,
        RedissonCodecs.LZ4Jdk,

        RedissonCodecs.LZ4Kryo5Composite,
        RedissonCodecs.LZ4ForyComposite,
        RedissonCodecs.LZ4JdkComposite,

        RedissonCodecs.ZstdKryo5,
        RedissonCodecs.ZstdFory,
        RedissonCodecs.ZstdJdk,

        RedissonCodecs.ZstdKryo5Composite,
        RedissonCodecs.ZstdForyComposite,
        RedissonCodecs.ZstdJdkComposite,

        RedissonCodecs.GzipKryo5,
        RedissonCodecs.GzipFory,
        RedissonCodecs.GzipJdk,

        RedissonCodecs.GzipKryo5Composite,
        RedissonCodecs.GzipForyComposite,
        RedissonCodecs.GzipJdkComposite,
    )

    @ParameterizedTest(name = "codec={0}")
    @MethodSource(METHOD_SOURCE)
    fun `codec for kotlin data class`(codec: Codec) = runTest {
        withTables(TestDB.H2, UserTable) {
            val entityId = UserTable.insertAndGetId {
                it[UserTable.firstName] = Fakers.randomString(1, 10)
                it[UserTable.lastName] = Fakers.randomString(1, 10)
                it[UserTable.email] = Fakers.faker.internet().emailAddress()
            }
            flushCache()

            val record = UserTable.selectAll()
                .where { UserTable.id eq entityId }
                .single()
                .toUserRecord()

            codec.verifyCodec(record)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <ID: Any, T: HasIdentifier<ID>> Codec.verifyCodec(origin: T) {
        val buf = valueEncoder.encode(origin)
        val actual = valueDecoder.decode(buf, State()) as? T
        actual!!.id shouldBeEqualTo origin.id
        // actual shouldBeEqualTo origin
    }
}
