package io.bluetape4k.json.compat.issue754.java;

import io.bluetape4k.json.JsonSerializer;

public final class LegacyJsonCaller {
    private LegacyJsonCaller() {
    }

    public static Object deserializeNull(JsonSerializer serializer) {
        return serializer.deserialize(null, Object.class);
    }
}
