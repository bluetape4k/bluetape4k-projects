package io.bluetape4k.spring.r2dbc.coroutines.blog

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import io.bluetape4k.spring.r2dbc.coroutines.blog.domain.CommentRepository
import io.bluetape4k.spring.r2dbc.coroutines.blog.domain.PostRepository
import kotlinx.coroutines.runBlocking
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class DatabaseInitializer(
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
) {

    companion object: KLoggingChannel()

    /**
     * Spring Boot Application이 준비되면 호출되는 Event Listener
     */
    @EventListener(value = [ApplicationReadyEvent::class])
    fun init() {
        log.info { "Insert new two posts ... " }

        // Transactional 하게 2개의 Post와 4개의 Comment를 저장합니다.
        runBlocking {
            postRepository.init()
            commentRepository.init()
        }

        log.info { "Done insert new two posts and four comments ..." }
    }
}
