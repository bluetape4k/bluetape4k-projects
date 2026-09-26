package io.bluetape4k.examples.cache.lettuce

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadmeDependencyContractTest {

    @Test
    fun `README dependency examples use consumer Gradle coordinates`() {
        readmeTexts().forEach { (filename, text) ->
            forbiddenFragments.forEach { fragment ->
                assertFalse(
                    text.contains(fragment),
                    "$filename must not expose bluetape4k-internal dependency helper: $fragment",
                )
            }

            requiredFragments.forEach { fragment ->
                assertTrue(
                    text.contains(fragment),
                    "$filename must document consumer dependency coordinate: $fragment",
                )
            }
        }
    }

    private fun readmeTexts(): Map<String, String> =
        listOf("README.md", "README.ko.md").associateWith { filename ->
            findReadme(filename).readText()
        }

    private fun findReadme(filename: String): Path {
        val cwd = Path.of("").toAbsolutePath()
        return generateSequence(cwd) { it.parent }
            .flatMap { path ->
                sequenceOf(
                    path.resolve("spring-boot/hibernate-lettuce-demo").resolve(filename),
                    path.resolve(filename),
                )
            }
            .firstOrNull(Files::isRegularFile)
            ?: error("Cannot find $filename from $cwd")
    }

    private companion object {
        val forbiddenFragments = listOf(
            "Libs.",
            "libs.",
            "springBootStarter",
            "springBoot(\"hibernate\")",
            "h2_database",
            "testcontainers_junit5",
        )

        val requiredFragments = listOf(
            "org.springframework.boot:spring-boot-dependencies",
            "io.github.bluetape4k:bluetape4k-spring-boot-hibernate-lettuce",
            "org.springframework.boot:spring-boot-starter-web",
            "org.springframework.boot:spring-boot-starter-data-jpa",
            "org.springframework.boot:spring-boot-starter-actuator",
            "com.h2database:h2",
            "org.springframework.boot:spring-boot-starter-test",
        )
    }
}
