package io.bluetape4k.io.readme;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.bluetape4k.io.AtomicFileSupport;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AtomicFileSupportJavaContractTest {

    @TempDir
    Path tempDir;

    @Test
    void javaCallerUsesAtomicFileSupportFacade() throws IOException {
        Path target = tempDir.resolve("java.bin");
        byte[] payload = "java-caller".getBytes();

        long written = write(target, payload);

        assertEquals(payload.length, written);
        assertArrayEquals(payload, Files.readAllBytes(target));
    }

    private static long write(Path destination, byte[] payload) throws IOException {
        return AtomicFileSupport.writeAtomically(destination, output -> {
            try {
                output.write(payload);
            } catch (IOException failure) {
                throw new UncheckedIOException(failure);
            }
            return Unit.INSTANCE;
        });
    }

    @Test
    void facadeDeclaresTheExpectedDescriptorAndIOException() throws NoSuchMethodException {
        Method method = AtomicFileSupport.class.getMethod(
            "writeAtomically",
            Path.class,
            Function1.class
        );

        assertEquals(long.class, method.getReturnType());
        assertArrayEquals(new Class<?>[] {IOException.class}, method.getExceptionTypes());
    }
}
