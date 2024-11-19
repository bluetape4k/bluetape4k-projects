package io.bluetape4k.spring.cassandra.reactive

import com.datastax.oss.driver.api.core.uuid.Uuids
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.cassandra.AbstractCassandraCoroutineTest
import io.bluetape4k.spring.cassandra.query.eq
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.data.annotation.Id
import org.springframework.data.cassandra.core.ReactiveCassandraOperations
import org.springframework.data.cassandra.core.mapping.Column
import org.springframework.data.cassandra.core.mapping.Indexed
import org.springframework.data.cassandra.core.mapping.Table
import org.springframework.data.cassandra.core.query.Query
import org.springframework.data.cassandra.core.query.Update
import org.springframework.data.cassandra.core.query.query
import org.springframework.data.cassandra.core.query.where
import org.springframework.data.cassandra.core.selectOne
import org.springframework.data.cassandra.core.truncate
import org.springframework.data.cassandra.core.update
import java.io.Serializable

@SpringBootTest
class ReactiveUpdateOperationsTest(
    @Autowired private val operations: ReactiveCassandraOperations,
): AbstractCassandraCoroutineTest("update-op") {

    companion object: KLogging() {
        private const val PERSON_TABLE_NAME = "update_op_person"
    }

    @Configuration(proxyBeanMethods = false)
    //@EntityScan(basePackageClasses = [Person::class]) // 내부 엔티티는 Scan 없이도 사용 가능하다
    class TestConfiguration: io.bluetape4k.spring.cassandra.AbstractReactiveCassandraTestConfiguration()

    @Table(PERSON_TABLE_NAME)
    data class Person(
        @field:Id val id: String,
        @field:Indexed var firstName: String,
        @field:Indexed var lastName: String,
    ): Serializable

    private data class Jedi(
        @field:Column("firstname") val name: String,
    )

    private fun newPerson(): Person {
        return Person(
            id = Uuids.timeBased().toString(),
            firstName = faker.name().firstName(),
            lastName = faker.name().lastName()
        )
    }

    private val han = newPerson()
    private val luke = newPerson()

    @BeforeEach
    fun beforeEach() {
        runBlocking {
            operations.truncate<Person>().awaitSingleOrNull()

            operations.insert(han).awaitSingle()
            operations.insert(luke).awaitSingle()
        }
    }


    @Test
    fun `update all matching`() = runSuspendIO {
        val writeResult = operations.update<Person>()
            .matching(queryHan())
            .apply(Update.update("firstname", "Han"))
            .awaitSingle()

        writeResult.wasApplied().shouldBeTrue()
    }

    @Test
    fun `update with different domain class and collection`() = runSuspendIO {
        val writeResult = operations.update<Jedi>()
            .inTable(PERSON_TABLE_NAME)
            .matching(query(where("id").eq(han.id)))
            .apply(Update.update("name", "Han"))
            .awaitSingle()

        writeResult.wasApplied().shouldBeTrue()

        val loaded = operations.selectOne<Person>(queryHan()).awaitSingle()
        loaded shouldNotBeEqualTo han
        loaded.firstName shouldBeEqualTo "Han"
    }

    private fun queryHan(): Query = query(where("id").eq(han.id))
}
