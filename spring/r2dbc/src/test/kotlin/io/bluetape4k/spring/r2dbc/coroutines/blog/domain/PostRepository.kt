package io.bluetape4k.spring.r2dbc.coroutines.blog.domain

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring.r2dbc.coroutines.awaitCountAll
import io.bluetape4k.spring.r2dbc.coroutines.awaitDeleteAll
import io.bluetape4k.spring.r2dbc.coroutines.awaitFindFirstById
import io.bluetape4k.spring.r2dbc.coroutines.awaitFindFirstByIdOrNull
import io.bluetape4k.spring.r2dbc.coroutines.awaitFindOneById
import io.bluetape4k.spring.r2dbc.coroutines.awaitFindOneByIdOrNull
import io.bluetape4k.spring.r2dbc.coroutines.awaitInsert
import io.bluetape4k.spring.r2dbc.coroutines.awaitSelectAll
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter
import org.springframework.data.r2dbc.core.R2dbcEntityOperations
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository

@Repository
class PostRepository(
    private val client: DatabaseClient,
    private val operations: R2dbcEntityOperations,
    private val mappingR2dbcConverter: MappingR2dbcConverter,
) {
    companion object: KLoggingChannel()

    suspend fun count(): Long {
        return operations.awaitCountAll<Post>()
    }

    fun findAll(): Flow<Post> {
        return operations.awaitSelectAll()
    }

    suspend fun findOneById(postId: Long): Post {
        return operations.awaitFindOneById(postId)
    }

    suspend fun findOneByIdOrNull(postId: Long): Post? {
        return operations.awaitFindOneByIdOrNull(postId)
    }

    suspend fun findFirstById(postId: Long): Post {
        return operations.awaitFindFirstById(postId, Post::id.name)
    }

    suspend fun findFirstByIdOrNull(postId: Long): Post? {
        return operations.awaitFindFirstByIdOrNull(postId, Post::id.name)
    }

    suspend fun deleteAll(): Long {
        return operations.awaitDeleteAll<Post>()
    }

    suspend fun save(post: Post): Post {
        return operations.awaitInsert(post)
    }

    suspend fun init() {
        save(Post(title = "My first post title", content = "Content of my first post"))
        save(Post(title = "My second post title", content = "Content of my second post"))
    }
}
