package io.bluetape4k.spring.r2dbc.coroutines.blog.test

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring.r2dbc.coroutines.blog.domain.Comment
import io.bluetape4k.spring.r2dbc.coroutines.blog.domain.Post
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AbstractR2dbcBlogApplicationTest {

    companion object: KLoggingChannel() {
        @JvmStatic
        val faker = Fakers.faker
    }

    @LocalServerPort
    private val port: Int = 0

    protected val client: WebTestClient by lazy {
        WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
    }

    protected fun createPost(): Post =
        Post(
            title = faker.book().title(),
            content = Fakers.fixedString(255)
        )

    protected fun createComment(postId: Long): Comment =
        Comment(
            postId = postId,
            content = Fakers.fixedString(255)
        )
}
