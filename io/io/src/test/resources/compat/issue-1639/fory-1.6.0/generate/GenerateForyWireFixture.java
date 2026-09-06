import io.bluetape4k.io.serializer.ForyWireCompatibilityFixtures;
import org.apache.fory.Fory;
import org.apache.fory.config.CompatibleMode;
import org.apache.fory.config.Language;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GenerateForyWireFixture {

    private GenerateForyWireFixture() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: GenerateForyWireFixture <output>");
        }

        Fory fory = Fory.builder()
                .withLanguage(Language.JAVA)
                .withCompatibleMode(CompatibleMode.COMPATIBLE)
                .withAsyncCompilation(true)
                .withRefTracking(true)
                .withRefCopy(true)
                .withCodegen(true)
                .withStringCompressed(true)
                .requireClassRegistration(false)
                .build();

        Files.write(Path.of(args[0]), fory.serialize(ForyWireCompatibilityFixtures.sample()));
    }
}
