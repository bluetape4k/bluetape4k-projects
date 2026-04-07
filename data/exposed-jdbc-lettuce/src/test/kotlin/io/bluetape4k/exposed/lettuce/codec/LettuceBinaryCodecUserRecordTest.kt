package io.bluetape4k.exposed.lettuce.codec

import io.bluetape4k.exposed.lettuce.domain.UserSchema
import io.bluetape4k.exposed.lettuce.domain.UserSchema.UserRecord
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * [LettuceBinaryCodec]으로 Exposed 도메인 data class([UserRecord])를 직렬화/역직렬화하는 테스트.
 *
 * NOTE: Exposed Entity(DAO 방식)는 직렬화 불가이지만,
 *       [UserRecord] 같은 data class는 [java.io.Serializable]을 구현하여 정상 동작한다.
 */
class LettuceBinaryCodecUserRecordTest {
    companion object: KLogging()

    @Suppress("unused")
    private fun getLettuceBinaryCodecs(): List<LettuceBinaryCodec<UserRecord>> =
        listOf(
            LettuceBinaryCodecs.jdk(),
            LettuceBinaryCodecs.kryo(),
            LettuceBinaryCodecs.fory(),
            LettuceBinaryCodecs.gzipJdk(),
            LettuceBinaryCodecs.gzipKryo(),
            LettuceBinaryCodecs.gzipFory(),
            LettuceBinaryCodecs.deflateJdk(),
            LettuceBinaryCodecs.deflateKryo(),
            LettuceBinaryCodecs.deflateFory(),
            LettuceBinaryCodecs.lz4Jdk(),
            LettuceBinaryCodecs.lz4Kryo(),
            LettuceBinaryCodecs.lz4Fory(),
            LettuceBinaryCodecs.snappyJdk(),
            LettuceBinaryCodecs.snappyKryo(),
            LettuceBinaryCodecs.snappyFory(),
            LettuceBinaryCodecs.zstdJdk(),
            LettuceBinaryCodecs.zstdKryo(),
            LettuceBinaryCodecs.zstdFory()
        )

    @ParameterizedTest(name = "codec={0}")
    @MethodSource("getLettuceBinaryCodecs")
    fun `UserRecord data class를 codec으로 encode 후 decode하면 원본과 같다`(codec: LettuceBinaryCodec<UserRecord>) {
        val origin = UserSchema.newUserRecord()

        val encoded = codec.encodeValue(origin)
        val decoded = codec.decodeValue(encoded)

        decoded.shouldNotBeNull()
        decoded shouldBeEqualTo origin
    }
}
