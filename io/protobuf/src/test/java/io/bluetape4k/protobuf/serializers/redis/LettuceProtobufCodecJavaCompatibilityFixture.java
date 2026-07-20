package io.bluetape4k.protobuf.serializers.redis;

import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec;
import io.netty.buffer.ByteBuf;

public final class LettuceProtobufCodecJavaCompatibilityFixture {

    private LettuceProtobufCodecJavaCompatibilityFixture() {}

    public static void compileExistingUsage(Object value, ByteBuf target) {
        LettuceBinaryCodec<Object> strict =
            LettuceProtobufCodecs.INSTANCE.protobuf();
        LettuceBinaryCodec<Object> trusted =
            LettuceProtobufCodecs.INSTANCE.trustedInternalProtobuf();

        strict.encodeValue(value, target);
        trusted.encodeValue(value, null);
    }
}
