package io.bluetape4k.spring.tests

import io.bluetape4k.assertions.shouldNotBeNull
import kotlinx.coroutines.reactive.asFlow
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.assertTrue

class WebClientReadmeExamplesTest {

    @Test
    fun `README WebClient examples do not call retrieve twice`() {
        readmeTexts().forEach { (fileName, text) ->
            val matches =
                DOUBLE_RETRIEVE_PATTERN
                    .findAll(text)
                    .map { it.value.replace(Regex("\\s+"), " ") }
                    .toList()

            assertTrue(
                actual = matches.isEmpty(),
                message = "$fileName should not chain .retrieve() after httpGet/httpPost: $matches",
            )
        }
    }

    @Test
    fun `README WebClient examples compile against ResponseSpec helpers`() {
        val webClient = WebClient.create("https://api.example.com")
        val newUser = ReadmeUser("debop")

        val response =
            webClient
                .httpGet("/users")
                .bodyToFlux(ReadmeUser::class.java)
                .asFlow()
        val created =
            webClient
                .httpPost("/users", newUser)
                .bodyToMono(ReadmeUser::class.java)

        response.shouldNotBeNull()
        created.shouldNotBeNull()
    }

    private fun readmeTexts(): Map<String, String> =
        listOf("README.md", "README.ko.md").associateWith { fileName ->
            modulePath(fileName).readText()
        }

    private fun modulePath(fileName: String): Path {
        val cwd = Path.of("").toAbsolutePath()

        return generateSequence(cwd) { it.parent }
            .map { it.resolve("spring-boot/core/$fileName") }
            .firstOrNull(Files::exists)
            ?: error("Cannot locate spring-boot/core/$fileName from $cwd")
    }

    private data class ReadmeUser(
        val name: String,
    )

    companion object {
        private val DOUBLE_RETRIEVE_PATTERN =
            Regex("""http(?:Get|Post)\([^)]*\)\s*\.\s*retrieve\(\)""")
    }
}
