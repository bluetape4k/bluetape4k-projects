package io.bluetape4k.avro.compat.issue754.java;

import io.bluetape4k.avro.AvroReflectSerializer;

public final class LegacyAvroCaller {
    private LegacyAvroCaller() {
    }

    public static Object deserializeNull(AvroReflectSerializer serializer) {
        return serializer.deserialize(null, Object.class);
    }
}
