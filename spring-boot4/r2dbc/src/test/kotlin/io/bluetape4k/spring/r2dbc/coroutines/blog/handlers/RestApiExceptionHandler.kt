package io.bluetape4k.spring.r2dbc.coroutines.blog.handlers

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.warn
import io.bluetape4k.spring.r2dbc.coroutines.blog.exceptions.PostNotFoundException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class RestApiExceptionHandler {

    companion object: KLoggingChannel()

    @ExceptionHandler(PostNotFoundException::class)
    fun handle(ex: PostNotFoundException): ResponseEntity<Void> {
        log.warn(ex) { "Post[${ex.postId}] not found." }
        return ResponseEntity.notFound().build()
    }
}
