package io.bluetape4k.examples.cassandra.reactive.people

import io.bluetape4k.examples.cassandra.AbstractCassandraTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

@SpringBootTest(classes = [PersonConfiguration::class])
class ReactivePersonRepositoryTest(
    @Autowired private val repository: ReactivePersonRepository,
): AbstractCassandraTest() {

    companion object: KLoggingChannel()

    @BeforeEach
    fun setup() {
        val deleteAndInsert = repository.deleteAll()
            .thenMany(
                repository.saveAll(
                    Flux.just(
                        Person("Debop", "Bae", 54),
                        Person("Skyler", "White", 45),
                        Person("Saul", "Goodman", 42),
                        Person("Jesse", "Pinkman", 27)
                    )
                )
            )

        StepVerifier.create(deleteAndInsert).expectNextCount(4).verifyComplete()
    }

    @Test
    fun `insert and count in reactive mode`() {
        val saveAndCount = repository.count()
            .doOnNext { println(it) }
            .thenMany(
                repository.saveAll(
                    Flux.just(
                        Person("Hank", "Schrader", 43),
                        Person("Mike", "Ehrmantraut", 62)
                    )
                )
            )
            .last()
            .flatMap { repository.count() }
            .doOnNext { println(it) }

        StepVerifier.create(saveAndCount).expectNext(6L).verifyComplete()
    }
}
