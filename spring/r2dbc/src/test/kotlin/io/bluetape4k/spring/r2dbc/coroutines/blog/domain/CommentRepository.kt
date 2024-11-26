package io.bluetape4k.spring.r2dbc.coroutines.blog.domain

import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.r2dbc.coroutines.coCount
import io.bluetape4k.spring.r2dbc.coroutines.coInsert
import io.bluetape4k.spring.r2dbc.coroutines.coSelect
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
    companion object: KLogging()

    suspend fun save(comment: Comment): Comment {
        return operations.coInsert(comment)
    }

    suspend fun countByPostId(postId: Long): Long {
        val query = Query.query(Criteria.where(Comment::postId.name).isEqual(postId))
        return operations.coCount<Comment>(query)
    }

    fun findAllByPostId(postId: Long): Flow<Comment> {
        val query = Query.query(Criteria.where(Comment::postId.name).isEqual(postId))
        return operations.coSelect(query)
    }

    suspend fun init() {
        save(Comment(postId = 1, content = "Content 1 of post 1"))
        save(Comment(postId = 1, content = "Content 2 of post 1"))
        save(Comment(postId = 2, content = "Content 1 of post 2"))
        save(Comment(postId = 2, content = "Content 2 of post 2"))
    }
}
