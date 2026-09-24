package io.bluetape4k.examples.cassandra

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadmeCoroutineRepositoryContractTest {

    @Test
    fun `README coroutine repository example matches tested Flow and suspend signatures`() {
        readmeTexts().forEach { (filename, text) ->
            forbiddenFragments.forEach { fragment ->
                assertFalse(
                    text.contains(fragment),
                    "$filename must not document stale coroutine repository signature: $fragment",
                )
            }

            requiredFragments.forEach { fragment ->
                assertTrue(
                    text.contains(fragment),
                    "$filename must document tested coroutine repository signature: $fragment",
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
                    path.resolve("spring-boot/cassandra-demo").resolve(filename),
                    path.resolve(filename),
                )
            }
            .firstOrNull(Files::isRegularFile)
            ?: error("Cannot find $filename from $cwd")
    }

    private companion object {
        val forbiddenFragments = listOf(
            "CoroutineCrudRepository<Person, UUID>",
            "suspend fun findByLastName(lastName: String): Flow<Person>",
            "findByLastName",
        )

        val requiredFragments = listOf(
            "CoroutineCrudRepository<Person, String>",
            "fun findByLastname(lastname: String): Flow<Person>",
            "suspend fun findByFirstnameAndLastname(firstname: String, lastname: String): Person?",
        )
    }
}
