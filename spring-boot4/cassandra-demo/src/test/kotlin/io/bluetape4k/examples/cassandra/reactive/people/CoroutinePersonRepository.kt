package io.bluetape4k.examples.cassandra.reactive.people

import kotlinx.coroutines.flow.Flow
import org.springframework.data.cassandra.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import reactor.core.publisher.Mono

interface CoroutinePersonRepository: CoroutineCrudRepository<Person, String> {

    fun findByLastname(lastname: String): Flow<Person>

    @Query("SELECT * FROM coroutine_persons WHERE firstname = ?0 AND lastname = ?1")
    suspend fun findByFirstnameAndLastname(firstname: String, lastname: String): Person?

    // NOTE: lastname을 제공하는 supplier를 suspend 함수로 하는 것은 지원하지 않느다. Mono 를 써야 한다
    fun findByLastname(lastname: Mono<String>): Flow<Person>

    // NOTE: firstname 을 제공하는 supplier를 suspend 함수로 하는 것은 지원하지 않는다. Mono 를 써야 한다
    suspend fun findByFirstnameAndLastname(firstname: Mono<String>, lastname: String): Person?

}
