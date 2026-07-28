package io.bluetape4k.examples.cassandra.kotlin

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface PersonRepository: CoroutineCrudRepository<Person, String> {

    suspend fun findOneOrNoneByFirstname(firstname: String): Person?

    suspend fun findNullableByFirstname(firstname: String): Person?

    fun findByFirstname(firstname: String): Flow<Person>

    /**
     * 결과가 필요한 query method입니다. 결과를 찾지 못하면 [org.springframework.dao.EmptyResultDataAccessException]을 던집니다.
     * NOTE: suspend 메소드일 때에는 발생하지 않습니다.
     */
    suspend fun findOneByFirstname(firstname: String): Person?
}
