package io.bluetape4k.examples.cassandra.reactive.people

import io.bluetape4k.coroutines.flow.extensions.toFastList
import io.bluetape4k.examples.cassandra.AbstractCassandraCoroutineTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.mono
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [PersonConfiguration::class])
class CoroutinePersonRepositoryTest(
    @param:Autowired private val repository: CoroutinePersonRepository,
): AbstractCassandraCoroutineTest("person") {

    companion object: KLoggingChannel()

    @BeforeEach
    fun setup() = runSuspendTest {
        repository.deleteAll()
        repository.saveAll(
            flowOf(
                Person("Bart", "Simpson", 9),
                Person("Homer", "Simpson", 44),
                Person("Debop", "Bae", 53),
                Person("Lisa", "Simpson", 8),
            )
        ).collect()
    }

    @Test
    fun `insert and count in coroutines`() = runSuspendIO {
        repository.count().also { println("already exists count=$it") }

        repository.saveAll(
            flowOf(
                Person("Iron", "Man", 45),
                Person("Tonny", "Stark", 45)
            )
        )
            .flowOn(Dispatchers.IO)
            .last()


        repository.count().also { println("after two user inserted=$it") } shouldBeEqualTo 6L
    }

    @Test
    fun `find by lastname`() = runSuspendIO {
        val simpsons = repository.findByLastname("Simpson").toList()
        simpsons.size shouldBeEqualTo 3
    }

    @Test
    fun `find by mono lastname`() = runSuspendIO {
        val simpsons = repository
            .findByLastname(mono { delay(10); "Simpson" })
            .toList()
        simpsons.size shouldBeEqualTo 3
    }

    @Test
    fun `find by firstname and lastname`() = runSuspendIO {
        val debop = Person("Debop", "Bae", 53)
        val loaded = repository.findByFirstnameAndLastname("Debop", "Bae")!!
        loaded shouldBeEqualTo debop
    }

    @Test
    fun `find by mono firstname and lastname`() = runSuspendIO {
        val debop = Person("Debop", "Bae", 53)
        val loaded = repository.findByFirstnameAndLastname(mono { "Debop" }, "Bae")!!
        loaded shouldBeEqualTo debop
    }
}
