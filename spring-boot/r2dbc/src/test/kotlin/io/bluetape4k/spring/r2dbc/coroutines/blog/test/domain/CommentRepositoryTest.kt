package io.bluetape4k.spring.r2dbc.coroutines.blog.test.domain

import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.coroutines.flow.extensions.log
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.r2dbc.coroutines.blog.domain.CommentRepository
import io.bluetape4k.spring.r2dbc.coroutines.blog.test.AbstractR2dbcBlogApplicationTest
import kotlinx.coroutines.flow.toList
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class CommentRepositoryTest(
    @param:Autowired private val commentRepository: CommentRepository,
): AbstractR2dbcBlogApplicationTest() {

    companion object: KLoggingChannel()

    @Test
    fun `find comments by post id`() = runSuspendIO {
        val comments = commentRepository.findAllByPostId(1L)
            .log("comment")
            .toList()

        comments.shouldNotBeEmpty()
        comments.size shouldBeGreaterOrEqualTo 2
    }

    @Test
    fun `find comments by non-existing post id`() = runSuspendIO {
        val comments = commentRepository.findAllByPostId(-1L)
            .log("comment")
            .toList()
        comments.shouldBeEmpty()
    }

    @Test
    fun `count of comments by post id`() = runSuspendIO {
        val count = commentRepository.countByPostId(1L)
        count shouldBeGreaterOrEqualTo 2L
    }

    @Test
    fun `count of comments by non-existing post id`() = runSuspendIO {
        commentRepository.countByPostId(-1L) shouldBeEqualTo 0L
    }

    @Test
    fun `insert new comment`() = runSuspendIO {
        val oldCommentSize = commentRepository.countByPostId(2L)

        val newComment = createComment(2L)
        val savedComment = commentRepository.save(newComment)

        log.debug { "savedComment=$savedComment" }
        savedComment.shouldNotBeNull()
        savedComment.id.shouldNotBeNull()

        val newCommentSize = commentRepository.countByPostId(2L)
        newCommentSize shouldBeEqualTo oldCommentSize + 1
    }
}
