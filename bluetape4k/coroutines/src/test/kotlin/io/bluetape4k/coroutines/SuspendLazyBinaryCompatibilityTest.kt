package io.bluetape4k.coroutines

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import javax.tools.ToolProvider
import kotlin.time.Duration.Companion.seconds

class SuspendLazyBinaryCompatibilityTest {

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `previous implementation inherits timeout interface defaults`(@TempDir tempDir: Path) {
        val outputDir = compileLegacyImplementation(tempDir)

        URLClassLoader(arrayOf(outputDir.toUri().toURL()), SuspendLazy::class.java.classLoader).use { loader ->
            Class.forName("io.bluetape4k.coroutines.SuspendLazy", false, loader) shouldBeEqualTo SuspendLazy::class.java
            val interfacePath = Path.of(SuspendLazy::class.java.protectionDomain.codeSource.location.toURI())
            interfacePath.startsWith(outputDir).shouldBeFalse()
            val legacyClass = Class.forName("io.bluetape4k.coroutines.LegacySuspendLazy", true, loader)
            val legacy = legacyClass.getDeclaredConstructor().newInstance() as SuspendLazy<Int>

            runBlocking {
                legacy.getUntil(1.seconds) shouldBeEqualTo 42
                legacy.getUntilOrNull(1.seconds) shouldBeEqualTo 42
                legacy.cancel()
            }
        }
    }

    private fun compileLegacyImplementation(tempDir: Path): Path {
        val compiler = requireNotNull(ToolProvider.getSystemJavaCompiler()) {
            "A JDK compiler is required for the binary compatibility fixture."
        }
        val sourceDir = Files.createDirectories(tempDir.resolve("src"))
        val outputDir = Files.createDirectories(tempDir.resolve("classes"))
        val sources = writeLegacySources(sourceDir)

        val result = compiler.run(
            null,
            null,
            null,
            "-proc:none",
            "-d",
            outputDir.toString(),
            *sources.map(Path::toString).toTypedArray(),
        )
        result shouldBeEqualTo 0

        Files.delete(outputDir.resolve("kotlin/coroutines/Continuation.class"))
        Files.delete(outputDir.resolve("io/bluetape4k/coroutines/SuspendLazy.class"))
        return outputDir
    }

    private fun writeLegacySources(sourceDir: Path): List<Path> {
        val continuationSource = sourceDir.resolve("Continuation.java")
        val interfaceSource = sourceDir.resolve("SuspendLazy.java")
        val implementationSource = sourceDir.resolve("LegacySuspendLazy.java")

        Files.writeString(
            continuationSource,
            """
            package kotlin.coroutines;
            public interface Continuation<T> {}
            """.trimIndent(),
        )
        Files.writeString(
            interfaceSource,
            """
            package io.bluetape4k.coroutines;
            import kotlin.coroutines.Continuation;
            public interface SuspendLazy<T> {
                Object invoke(Continuation<? super T> continuation);
            }
            """.trimIndent(),
        )
        Files.writeString(
            implementationSource,
            """
            package io.bluetape4k.coroutines;
            import kotlin.coroutines.Continuation;
            public final class LegacySuspendLazy implements SuspendLazy<Integer> {
                @Override
                public Object invoke(Continuation<? super Integer> continuation) {
                    return Integer.valueOf(42);
                }
            }
            """.trimIndent(),
        )
        return listOf(continuationSource, interfaceSource, implementationSource)
    }
}
