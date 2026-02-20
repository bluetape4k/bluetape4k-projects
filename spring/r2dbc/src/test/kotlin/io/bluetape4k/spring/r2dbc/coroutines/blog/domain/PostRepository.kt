package io.bluetape4k.spring.r2dbc.coroutines.blog.domain

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring.r2dbc.coroutines.countAllSuspending
import io.bluetape4k.spring.r2dbc.coroutines.deleteAllSuspending
import io.bluetape4k.spring.r2dbc.coroutines.findFirstByIdSuspending
import io.bluetape4k.spring.r2dbc.coroutines.findFirstByIdOrNullSuspending
import io.bluetape4k.spring.r2dbc.coroutines.findOneByIdSuspending
import io.bluetape4k.spring.r2dbc.coroutines.findOneByIdOrNullSuspending
import io.bluetape4k.spring.r2dbc.coroutines.insertSuspending
import io.bluetape4k.spring.r2dbc.coroutines.selectAllSuspending
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.core.R2dbcEntityOperations
import org.springframework.stereotype.Repository

@Repository
class PostRepository(
//    private val client: DatabaseClient,
    private val operations: R2dbcEntityOperations,
//    private val mappingR2dbcConverter: MappingR2dbcConverter,
) {
    companion object: KLoggingChannel()

    suspend fun count(): Long {
        return operations.countAllSuspending<Post>()
    }

    fun findAll(): Flow<Post> {
        return operations.selectAllSuspending()
    }

    suspend fun findOneById(postId: Long): Post {
        return operations.findOneByIdSuspending(postId)
    }

    suspend fun findOneByIdOrNull(postId: Long): Post? {
        return operations.findOneByIdOrNullSuspending(postId)
    }

    suspend fun findFirstById(postId: Long): Post {
        return operations.findFirstByIdSuspending(postId, Post::id.name)
    }

    suspend fun findFirstByIdOrNull(postId: Long): Post? {
        return operations.findFirstByIdOrNullSuspending(postId, Post::id.name)
    }

    suspend fun deleteAll(): Long {
        return operations.deleteAllSuspending<Post>()
    }

    suspend fun save(post: Post): Post {
        return operations.insertSuspending(post)
    }

    suspend fun init() {
        save(Post(title = "My first post title", content = "Content of my first post"))
        save(Post(title = "My second post title", content = "Content of my second post"))
    }
}
