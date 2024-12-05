package io.bluetape4k.testcontainers.llm

import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.restassured.RestAssured.given
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test
import org.testcontainers.utility.DockerImageName

class OllamaServerTest {

    companion object: KLogging()

    @Test
    fun `launch ollama server`() {
        OllamaServer().use { ollama ->
            ollama.start()

            val version = getOllamaVersion(ollama.url)
            version shouldBeEqualTo OllamaServer.TAG
        }
    }

    @Test
    fun `download model and commit to image`() {
        val newImageName = "tc-ollama-allminilm-" + Base58.randomString(4).lowercase()
        OllamaServer().use { ollama ->
            ollama.start()

            // pull model
            ollama.execInContainer("ollama", "pull", "all-minilm")

            val modelName = getOllimaModels(ollama.url).first()
            modelName shouldContain "all-minilm"

            ollama.commitToImage(newImageName)
        }

        // substitute
        val imageName = DockerImageName.parse(newImageName).asCompatibleSubstituteFor(OllamaServer.IMAGE)
        OllamaServer(imageName).use { ollama ->
            ollama.start()

            val modelName = getOllimaModels(ollama.url).first()
            modelName shouldContain "all-minilm"
        }
    }

    private fun getOllamaVersion(endpoint: String): String {
        log.debug { "Get Ollama version from $endpoint" }
        return given()
            .baseUri(endpoint)
            .get("/api/version")
            .jsonPath()
            .getString("version")
            .also { version ->
                log.debug { "Ollama version: $version" }
            }
    }

    private fun getOllimaModels(endpoint: String): List<String> {
        return given()
            .baseUri(endpoint)
            .get("/api/tags")
            .jsonPath()
            .getList<String>("models.name")
            .also { models ->
                log.debug { "Ollama models: ${models.joinToString()}" }
            }
    }
}
