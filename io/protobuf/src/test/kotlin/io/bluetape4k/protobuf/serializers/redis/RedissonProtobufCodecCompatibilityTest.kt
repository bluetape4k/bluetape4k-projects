package io.bluetape4k.protobuf.serializers.redis

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.redis.redisson.codec.RedissonCodecs
import org.junit.jupiter.api.Test
import org.redisson.client.handler.State

class RedissonProtobufCodecCompatibilityTest {

    @Test
    fun `기본 trusted fallback은 기존 Kryo5 payload를 읽는다`() {
        val legacyPayload = RedissonCodecs.Kryo5.valueEncoder.encode("legacy")
        try {
            RedissonProtobufCodec.trustedInternal()
                .valueDecoder
                .decode(legacyPayload, State()) shouldBeEqualTo "legacy"
        } finally {
            legacyPayload.release()
        }
    }
}
