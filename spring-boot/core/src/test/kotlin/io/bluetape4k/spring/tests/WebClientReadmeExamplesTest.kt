package io.bluetape4k.spring.tests

import io.bluetape4k.assertions.shouldNotBeNull
import kotlinx.coroutines.reactive.asFlow
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class WebClientReadmeExamplesTest {

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

    private data class ReadmeUser(
        val name: String,
    )
}
