package io.bluetape4k.workshop.mongo.reactive

import io.bluetape4k.logging.KLogging
import io.bluetape4k.workshop.mongo.domain.Person
import io.bluetape4k.workshop.mongo.domain.PersonReactiveRepository
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.concurrent.ConcurrentLinkedQueue

class PersonReactiveRepositoryTest @Autowired constructor(
    private val repository: PersonReactiveRepository,
    private val operations: ReactiveMongoOperations,
): AbstractReactiveMongoTest(operations) {

    companion object: KLogging()

    @Test
    fun `insert and count`() {
        val prevCount = repository.count().block()!!

        val saveAndCount = repository.count()
            .doOnNext(System.out::println)
            .thenMany(
                repository.saveAll(Flux.just(newPerson(), newPerson()))
            )
            .last()
            .flatMap { repository.count() }
            .doOnNext(System.out::println)

        StepVerifier.create(saveAndCount).expectNext(prevCount + 2).verifyComplete()
    }

    @Test
    fun `perform conversion before result processing`() {
        StepVerifier.create(repository.findAll().doOnNext(System.out::println))
            .expectNextCount(4L)
            .verifyComplete()
    }

    @Test
    fun `stream data with tailable cursor`() {
        val prevCount = repository.count().block()!!.toInt()

        val queue = ConcurrentLinkedQueue<Person>()

        val disposable = repository.findWithTailableCursorBy()
            .doOnNext { println(it) }
            .doOnNext(queue::add)
            .doOnComplete { println("Complete") }
            .doOnTerminate { println("Terminated") }
            .subscribe()

        Thread.sleep(100)

        StepVerifier.create(repository.save(newPerson()))
            .expectNextCount(1)
            .verifyComplete()

        Thread.sleep(100)

        StepVerifier.create(repository.save(newPerson()))
            .expectNextCount(1)
            .verifyComplete()

        Thread.sleep(100)

        // dispose 된 이후로 추가된 Person은 queue에 추가되지 않는다
        disposable.dispose()

        StepVerifier.create(repository.save(newPerson()))
            .expectNextCount(1)
            .verifyComplete()

        Thread.sleep(100)

        queue.size shouldBeEqualTo prevCount + 2
    }

    @Test
    fun `query data with query derivation`() {
        StepVerifier.create(repository.findByLastname("White")).expectNextCount(2).verifyComplete()
    }

    @Test
    fun `query data with string query`() {
        StepVerifier.create(repository.findByFirstnameAndLastname("Walter", "White")).expectNextCount(1)
            .verifyComplete()
    }

    @Test
    fun `query data with deferred query deviation`() {
        StepVerifier.create(repository.findByLastname(Mono.just("White"))).expectNextCount(2).verifyComplete()
    }

    @Test
    fun `query data with mixed diferred query deviation`() {
        val person = repository.findByFirstnameAndLastname(Mono.just("Walter"), "White")
        StepVerifier.create(person).expectNextCount(1).verifyComplete()
    }
}
