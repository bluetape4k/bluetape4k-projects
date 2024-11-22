package io.bluetape4k.workshop.mongo.domain

import kotlinx.coroutines.flow.Flow
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.Tailable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface PersonCoroutineRepository: CoroutineCrudRepository<Person, String> {

    suspend fun findPersonByFirstname(firstname: String): Person?

    suspend fun findFirstByFirstname(firstname: String): Person?

    fun findAllByFirstname(firstname: String): Flow<Person>

    @Query("{ 'firstname': ?0, 'lastname': ?1 }")
    suspend fun findByFirstnameAndLastname(firstname: String, lastname: String): Person?

    fun findByLastname(lastname: String): Flow<Person>

    // NOTE: 인자로 Mono<String>을 받는 것은 동작하지만 `suspend () -> String` 은 동작하지 않는다
    //
    fun findByLastname(lastname: Mono<String>): Flow<Person>

    /**
     * Derived query selecting by [firstname] and [lastname]. [firstname] uses deferred resolution that
     * does not require blocking to obtain the parameter value.
     * NOTE: 인자로 Mono<String>을 받는 것은 동작하지만 `suspend () -> String` 은 동작하지 않는다
     */
    suspend fun findByFirstnameAndLastname(firstname: Mono<String>, lastname: String): Person?

    /**
     * Use a tailable cursor to emit a stream of entities as new entities are written to the capped collection.
     *
     * 참고: [MongoDB Tailable cursors](https://www.mongodb.com/docs/manual/core/tailable-cursors/)
     */
    @Tailable
    fun findWithTailableCursorBy(): Flux<Person>
}
