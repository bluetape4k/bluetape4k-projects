package io.bluetape4k.spring.r2dbc.coroutines.blog.test.domain

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.r2dbc.coroutines.blog.domain.PostRepository
import io.bluetape4k.spring.r2dbc.coroutines.blog.test.AbstractR2dbcBlogApplicationTest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PostRepositoryTest(
    @param:Autowired private val postRepository: PostRepository,
): AbstractR2dbcBlogApplicationTest() {

    companion object: KLoggingChannel()

    @Test
    fun `context loading`() {
        postRepository.shouldNotBeNull()
    }

    @Test
    fun `find all posts`() = runTest {
        val posts = postRepository.findAll()
            .onEach { post -> log.debug { "post=$post" } }
            .toList()
        posts.shouldNotBeEmpty()
    }

    @Test
    fun `find one post by id`() = runTest {
        val post = postRepository.findOneById(1L)
        post.id shouldBeEqualTo 1L
        log.debug { "post=$post" }
    }

    @Test
    fun `find one post by id - not exists`() = runTest {
        postRepository.findOneByIdOrNull(-1L).shouldBeNull()
    }

    @Test
    fun `find first by id`() = runTest {
        val post = postRepository.findFirstById(1L)
        post.id shouldBeEqualTo 1L
    }

    @Test
    fun `find first by id - not exists`() = runTest {
        postRepository.findFirstByIdOrNull(-1L).shouldBeNull()
    }

    @Test
    fun `insert new post`() = runTest {
        val oldCount = postRepository.count()

        val newPost = createPost()
        val savedPost = postRepository.save(newPost)
        savedPost.id.shouldNotBeNull()

        val newCount = postRepository.count()
        newCount shouldBeEqualTo oldCount + 1
    }
}
