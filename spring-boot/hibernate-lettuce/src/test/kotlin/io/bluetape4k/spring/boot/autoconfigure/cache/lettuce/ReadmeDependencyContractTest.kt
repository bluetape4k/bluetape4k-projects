package io.bluetape4k.spring.boot.autoconfigure.cache.lettuce

import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

class ReadmeDependencyContractTest {

    @Test
    fun `README dependency examples use consumer Gradle coordinates`() {
        readmeTexts().forEach { (filename, text) ->
            forbiddenFragments.forEach { fragment ->
                text.contains(fragment).shouldBeFalse()
            }

            requiredFragments.forEach { fragment ->
                text.contains(fragment).shouldBeTrue()
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
                    path.resolve("spring-boot/hibernate-lettuce").resolve(filename),
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
            "micrometer_core",
        )

        val requiredFragments = listOf(
            "org.springframework.boot:spring-boot-dependencies",
            "io.github.bluetape4k:bluetape4k-spring-boot-hibernate-lettuce",
            "org.springframework.boot:spring-boot-starter-data-jpa",
            "org.springframework.boot:spring-boot-starter-actuator",
            "io.micrometer:micrometer-core",
            "org.springframework.boot:spring-boot-hibernate",
        )
    }
}
