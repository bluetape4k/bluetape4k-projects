package io.bluetape4k.examples.cassandra.reactive.people

import org.springframework.data.cassandra.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface ReactivePersonRepository: ReactiveCrudRepository<Person, String> {

    fun findByLastname(lastname: String): Flux<Person>

    @Query("SELECT * FROM coroutine_persons WHERE firstname = ?0 AND lastname = ?1")
    fun findByFirstnameInAndLastname(firstname: String, lastname: String): Mono<Person>

    fun findByLastname(lastname: Mono<String>): Flux<Person>

    fun findByFirstnameAndLastname(firstname: Mono<String>, lastname: String): Mono<Person>
}
