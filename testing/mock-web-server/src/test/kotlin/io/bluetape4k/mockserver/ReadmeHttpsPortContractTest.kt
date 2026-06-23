package io.bluetape4k.mockserver

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * README HTTPS port documentation must match runtime and container metadata.
 */
class ReadmeHttpsPortContractTest {

    @Test
    fun `README HTTPS port matches application and Jib container ports`() {
        val projectDir = projectDir()
        val applicationYaml = projectDir.resolve("src/main/resources/application.yml").readText()
        val buildGradle = projectDir.resolve("build.gradle.kts").readText()
        val expectedHttpsPort = Regex("https:\\s*\\n\\s*port:\\s*(\\d+)")
            .find(applicationYaml)
            ?.groupValues
            ?.get(1)

        expectedHttpsPort shouldBeEqualTo "8443"
        buildGradle.contains("\"$expectedHttpsPort\"").shouldBeTrue()

        val staleHttpsPortDocs = listOf(
            Regex("""\*\*443\*\*"""),
            Regex("""(?<!\d)443\s*\(HTTPS\)"""),
            Regex("""`443`"""),
            Regex("""-p 443:443"""),
        )

        listOf("README.md", "README.ko.md").forEach { name ->
            val readme = projectDir.resolve(name).readText()

            staleHttpsPortDocs.any { it.containsMatchIn(readme) }.shouldBeFalse()
            readme.contains(expectedHttpsPort!!).shouldBeTrue()
        }
    }

    private fun projectDir(): Path {
        val candidates = listOf(
            Path.of("."),
            Path.of("testing/mock-web-server"),
        ).filter { it.resolve("src/main/resources/application.yml").exists() }

        check(candidates.isNotEmpty()) {
            "mock-web-server project directory was not found from ${Path.of("").toAbsolutePath()}"
        }

        return candidates.first()
    }
}
