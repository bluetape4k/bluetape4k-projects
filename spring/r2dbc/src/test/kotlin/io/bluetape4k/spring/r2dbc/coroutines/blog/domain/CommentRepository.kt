package io.bluetape4k.spring.r2dbc.coroutines.blog.domain

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring.r2dbc.coroutines.awaitCount
import io.bluetape4k.spring.r2dbc.coroutines.awaitInsert
import io.bluetape4k.spring.r2dbc.coroutines.awaitSelect
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.core.R2dbcEntityOperations
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.data.relational.core.query.isEqual
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository

@Repository
class CommentRepository(
    private val client: DatabaseClient,
    private val operations: R2dbcEntityOperations,
) {
    companion object: KLoggingChannel()

    suspend fun save(comment: Comment): Comment {
        return operations.awaitInsert(comment)
    }

    suspend fun countByPostId(postId: Long): Long {
        val query = Query.query(Criteria.where(Comment::postId.name).isEqual(postId))
        return operations.awaitCount<Comment>(query)
    }

    fun findAllByPostId(postId: Long): Flow<Comment> {
        val query = Query.query(Criteria.where(Comment::postId.name).isEqual(postId))
        return operations.awaitSelect(query)
    }

    suspend fun init() {
        save(Comment(postId = 1, content = "Content 1 of post 1"))
        save(Comment(postId = 1, content = "Content 2 of post 1"))
        save(Comment(postId = 2, content = "Content 1 of post 2"))
        save(Comment(postId = 2, content = "Content 2 of post 2"))
    }
}
