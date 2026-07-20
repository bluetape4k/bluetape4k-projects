package io.bluetape4k.redis.lettuce.codec;

import io.bluetape4k.io.serializer.JdkBinarySerializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public final class LettuceBinaryCodecJavaCompatibilityFixture {

    private LettuceBinaryCodecJavaCompatibilityFixture() {}

    public static void compileExistingUsage() {
        LettuceBinaryCodec<String> codec =
            new LettuceBinaryCodec<>(new JdkBinarySerializer());
        ByteBuf target = Unpooled.buffer();
        try {
            codec.encodeValue("value", target);
        } finally {
            target.release();
        }
    }
}
