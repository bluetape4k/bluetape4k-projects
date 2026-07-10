package io.bluetape4k.testcontainers

import io.bluetape4k.assertions.shouldBeEmpty
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.readText

class ContainerReusePolicyTest {

    @Test
    fun `server wrappers do not enable container reuse by default`() {
        val sourceRoot = Path.of("src/main/kotlin")
        val violations = Files.walk(sourceRoot).use { paths ->
            paths
                .filter { it.extension == "kt" }
                .filter { source ->
                    val text = source.readText()
                    REUSABLE_DEFAULT.containsMatchIn(text) ||
                        REUSABLE_NAMED_ARGUMENT.containsMatchIn(text) ||
                        IMPLICIT_REUSE.containsMatchIn(text)
                }
                .map(sourceRoot::relativize)
                .toList()
        }

        violations.shouldBeEmpty()
    }

    private companion object {
        val REUSABLE_DEFAULT = Regex("reuse\\s*:\\s*Boolean\\s*=\\s*true")
        val REUSABLE_NAMED_ARGUMENT = Regex("(?m)^\\s*reuse\\s*=\\s*true")
        val IMPLICIT_REUSE = Regex("withReuse\\(true\\)")
    }
}
