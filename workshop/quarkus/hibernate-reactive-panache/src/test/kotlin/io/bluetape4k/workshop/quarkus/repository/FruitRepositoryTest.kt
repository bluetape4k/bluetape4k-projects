package io.bluetape4k.workshop.quarkus.repository

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.quarkus.panache.withPanacheTransactionAndAwait
import io.bluetape4k.workshop.quarkus.model.Fruit
import io.quarkus.test.hibernate.reactive.panache.TransactionalUniAsserter
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.vertx.RunOnVertxContext
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.inject.Inject
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.function.Supplier
import kotlin.random.Random

@QuarkusTest
@RunOnVertxContext
class FruitRepositoryTest {

    companion object: KLogging() {

        private val faker = Fakers.faker

        private fun newGrape(): Fruit = Fruit("Graph-${Random.nextLong()}", "Summer fruit")
        private fun randomFruit(): Fruit = Fruit(
            name = "Fruit-${Random.nextLong(100, 99999)}",
            description = "Random Fruit - ${Random.nextLong()}"
        )
    }

    @Inject
    internal lateinit var repository: FruitRepository

    private val grape = newGrape()

    @Test
    fun `find by name with Uni`(asserter: TransactionalUniAsserter) {
        asserter.execute(Supplier { repository.persist(grape) })

        asserter.assertThat({ repository.findByName(grape.name) }) { fruit ->
            fruit.id.shouldNotBeNull()
            fruit.name shouldBeEqualTo grape.name
        }
    }

    @Disabled("Quarkus 2.+ 버전에서는 되는 거 였는데, 3.+ 버전에서는 TransactionalUniAsserter를 써야 합니다.")
    @Test
    fun `find by name with Coroutines`() = runTest {
        val grape = newGrape()
        withPanacheTransactionAndAwait { _, tx ->
            // HINT: withRollbackAndAwait 을 사용해도 됩니다.
            tx.markForRollback()

            repository.persist(grape).awaitSuspending()

            val fruit = repository.findByName(grape.name).awaitSuspending()

            fruit.id.shouldNotBeNull()
            fruit.name shouldBeEqualTo grape.name
        }
    }
}
