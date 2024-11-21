package io.bluetape4k.examples.cassandra.kotlin

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface PersonRepository: CoroutineCrudRepository<Person, String> {

    suspend fun findOneOrNoneByFirstname(firstname: String): Person?

    suspend fun findNullableByFirstname(firstname: String): Person?

    fun findByFirstname(firstname: String): Flow<Person>

    /**
     * Query method requiring a result. Throws [org.springframework.dao.EmptyResultDataAccessException] if no result is found.
     * NOTE: suspend 메소드일 때에는 발생하지 않습니다.
     */
    suspend fun findOneByFirstname(firstname: String): Person?
}
