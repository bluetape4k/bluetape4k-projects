package io.bluetape4k.spring.r2dbc.coroutines.blog.test.domain

import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.r2dbc.coroutines.blog.domain.CommentRepository
import io.bluetape4k.spring.r2dbc.coroutines.blog.test.AbstractR2dbcBlogApplicationTest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class CommentRepositoryTest(
    @Autowired private val commentRepository: CommentRepository,
): AbstractR2dbcBlogApplicationTest() {

    companion object: KLogging()

    @Test
    fun `find comments by post id`() = runTest {
        val comments = commentRepository.findAllByPostId(1L).toList()
        comments.shouldNotBeEmpty()
        comments.size shouldBeGreaterOrEqualTo 2
    }

    @Test
    fun `find comments by non-existing post id`() = runTest {
        val comments = commentRepository.findAllByPostId(-1L).toList()
        comments.shouldBeEmpty()
    }

    @Test
    fun `count of comments by post id`() = runTest {
        val count = commentRepository.countByPostId(1L)
        count shouldBeGreaterOrEqualTo 2L
    }

    @Test
    fun `count of comments by non-existing post id`() = runTest {
        commentRepository.countByPostId(-1L) shouldBeEqualTo 0L
    }

    @Test
    fun `insert new comment`() = runTest {
        val oldCommentSize = commentRepository.countByPostId(2L)

        val newComment = createComment(2L)
        val savedComment = commentRepository.save(newComment)
        savedComment.shouldNotBeNull()
        savedComment.id.shouldNotBeNull()

        val newCommentSize = commentRepository.countByPostId(2L)
        newCommentSize shouldBeEqualTo oldCommentSize + 1
    }
}
