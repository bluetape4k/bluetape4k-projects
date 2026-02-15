package io.bluetape4k.feign.spring

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.http.HttpbinServer
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest(
    classes = [HttpbinApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class HttpbinClientTest(
    @param:Autowired private val jsonPlaceClient: HttpbinClient,
) {

    companion object: KLogging() {
        @JvmStatic
        private val httpbinServer by lazy { HttpbinServer.Launcher.httpbin }

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("test.feign.httpbin-url") { httpbinServer.url }
        }
    }

    @Test
    fun `context loading`() {
        jsonPlaceClient.shouldNotBeNull()
    }

    @Test
    fun `get all posts`() {
        val response = jsonPlaceClient.posts()
        log.debug { "posts response: $response" }
        response.method shouldBeEqualTo "GET"
        response.url.shouldNotBeNull().shouldContain("/anything/posts")
    }

    @Test
    fun `get post's comments`() {
        val comments1 = jsonPlaceClient.getPostComments(1)
        comments1.method shouldBeEqualTo "GET"
        comments1.url.shouldNotBeNull().shouldContain("/anything/post/1/comments")

        val comments2 = jsonPlaceClient.getPostComments(2)
        comments2.method shouldBeEqualTo "GET"
        comments2.url.shouldNotBeNull().shouldContain("/anything/post/2/comments")
    }
}
