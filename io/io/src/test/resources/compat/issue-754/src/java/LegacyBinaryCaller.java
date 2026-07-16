package io.bluetape4k.io.serializer.compat.issue754.java;

import io.bluetape4k.io.serializer.BinarySerializer;
import java.io.Serializable;
import java.util.Objects;

public final class LegacyBinaryCaller {
    private LegacyBinaryCaller() {
    }

    public static Object deserializeNull(BinarySerializer serializer) {
        return serializer.deserialize(null);
    }

    public static final class SimpleData implements Serializable {
        private static final long serialVersionUID = 754L;

        public long id;
        public String name;
        public int age;

        public SimpleData() {
        }

        public SimpleData(long id, String name, int age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof SimpleData that)) {
                return false;
            }
            return id == that.id && age == that.age && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, age);
        }
    }
}
