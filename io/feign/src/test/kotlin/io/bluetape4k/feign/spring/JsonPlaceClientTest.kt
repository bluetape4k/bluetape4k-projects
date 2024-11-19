package io.bluetape4k.feign.spring

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    classes = [JsonPlaceApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class JsonPlaceClientTest(
    @Autowired private val jsonPlaceClient: JsonPlaceClient,
) {

    companion object: KLogging()

    @Test
    fun `context loading`() {
        jsonPlaceClient.shouldNotBeNull()
    }

    @Test
    fun `get all posts`() {
        val posts = jsonPlaceClient.posts()
        posts.forEach {
            log.trace { "post: $it" }
        }
        posts.shouldNotBeEmpty()
    }

    @Test
    fun `get post's comments`() {
        val comments1 = jsonPlaceClient.getPostComments(1)
        comments1.shouldNotBeEmpty()

        val comments2 = jsonPlaceClient.getPostComments(2)
        comments2.shouldNotBeEmpty()
    }
}
