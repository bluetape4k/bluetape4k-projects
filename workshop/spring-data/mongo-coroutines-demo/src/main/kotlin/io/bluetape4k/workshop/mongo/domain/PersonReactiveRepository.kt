package io.bluetape4k.workshop.mongo.domain

import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.Tailable
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface PersonReactiveRepository: ReactiveCrudRepository<Person, String> {

    fun findPersonByFirstname(firstname: String): Mono<Person>

    fun findFirstByFirstname(firstname: String): Mono<Person>

    fun findAllByFirstname(firstname: String): Flux<Person>

    @Query("{ 'firstname': ?0, 'lastname': ?1 }")
    fun findByFirstnameAndLastname(firstname: String, lastname: String): Mono<Person>

    fun findByLastname(lastname: String): Flux<Person>

    /**
     * Derived query selecting by [lastname]. [lastname] uses deferred resolution that does not require
     * blocking to obtain the parameter value.
     *
     * @param lastname
     * @return
     */
    fun findByLastname(lastname: Mono<String>): Flux<Person>

    /**
     * Derived query selecting by [firstname] and [lastname]. [firstname] uses deferred resolution that
     * does not require blocking to obtain the parameter value.
     *
     * @param firstname
     * @param lastname
     * @return
     */
    fun findByFirstnameAndLastname(firstname: Mono<String>, lastname: String): Mono<Person>

    /**
     * Use a tailable cursor to emit a stream of entities as new entities are written to the capped collection.
     *
     * 참고: [MongoDB Tailable cursors](https://www.mongodb.com/docs/manual/core/tailable-cursors/)
     */
    @Tailable
    fun findWithTailableCursorBy(): Flux<Person>
}
