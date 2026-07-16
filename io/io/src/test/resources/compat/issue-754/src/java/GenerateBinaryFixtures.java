package io.bluetape4k.io.serializer.compat.issue754.java;

import io.bluetape4k.io.serializer.BinarySerializer;
import io.bluetape4k.io.serializer.ForyBinarySerializer;
import io.bluetape4k.io.serializer.JdkBinarySerializer;
import io.bluetape4k.io.serializer.KryoBinarySerializer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GenerateBinaryFixtures {
    private GenerateBinaryFixtures() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("expected fixture output directory");
        }
        Path output = Path.of(args[0]);
        Files.createDirectories(output);
        LegacyBinaryCaller.SimpleData data = new LegacyBinaryCaller.SimpleData(754L, "issue-754", 12);

        write(output.resolve("jdk-simple-data.bin"), new JdkBinarySerializer(), data);
        write(output.resolve("kryo-default-simple-data.bin"), new KryoBinarySerializer(), data);
        write(output.resolve("kryo-fast-simple-data.bin"), KryoBinarySerializer.fast(), data);
        write(output.resolve("fory-default-simple-data.bin"), new ForyBinarySerializer(), data);
        write(output.resolve("fory-fast-simple-data.bin"), ForyBinarySerializer.fast(), data);
    }

    private static void write(Path path, BinarySerializer serializer, LegacyBinaryCaller.SimpleData data)
        throws Exception {
        byte[] bytes = serializer.serialize(data);
        if (bytes.length == 0) {
            throw new IllegalStateException("empty fixture: " + path);
        }
        Files.write(path, bytes);
        Object restored = serializer.deserialize(bytes);
        if (!data.equals(restored)) {
            throw new IllegalStateException("fixture round trip failed: " + path);
        }
    }
}
