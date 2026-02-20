package io.bluetape4k.spring.cassandra.reactive

import com.datastax.oss.driver.api.core.uuid.Uuids
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring.cassandra.AbstractCassandraCoroutineTest
import io.bluetape4k.spring.cassandra.AbstractReactiveCassandraTestConfiguration
import io.bluetape4k.spring.cassandra.cql.insertOptions
import io.bluetape4k.spring.cassandra.selectOneByIdSuspending
import io.bluetape4k.spring.cassandra.truncateSuspending
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.data.annotation.Id
import org.springframework.data.cassandra.core.ReactiveCassandraOperations
import org.springframework.data.cassandra.core.insert
import org.springframework.data.cassandra.core.mapping.Indexed
import org.springframework.data.cassandra.core.mapping.Table
import java.io.Serializable

@SpringBootTest
class ReactiveInsertOperationsTest(
    @param:Autowired private val reactiveOps: ReactiveCassandraOperations,
): AbstractCassandraCoroutineTest("insert-op") {

    companion object: KLoggingChannel() {
        private const val PERSON_TABLE_NAME = "insert_op_person"
    }

    @Configuration
    //@EntityScan(basePackageClasses = [Person::class]) // 내부 엔티티는 Scan 없이도 사용 가능하다
    class TestConfiguration: AbstractReactiveCassandraTestConfiguration()

    @Table(PERSON_TABLE_NAME)
    data class Person(
        @field:Id val id: String,
        @field:Indexed var firstName: String,
        @field:Indexed var lastName: String,
    ): Serializable

    private fun newPerson(): Person {
        return Person(
            id = Uuids.timeBased().toString(),
            firstName = faker.name().firstName(),
            lastName = faker.name().lastName()
        )
    }

    @BeforeEach
    fun beforeEach() {
        runBlocking {
            reactiveOps.truncateSuspending<Person>()
        }
    }

    @Test
    fun `context loading`() {
        reactiveOps.shouldNotBeNull()
    }

    @Test
    fun `insert one entity`() = runSuspendIO {
        val person = newPerson()
        val writeResult = reactiveOps.insert<Person>()
            .inTable(PERSON_TABLE_NAME)
            .one(person)
            .awaitSingle()

        writeResult.wasApplied().shouldBeTrue()
        writeResult.entity shouldBeEqualTo person

        reactiveOps.selectOneByIdSuspending<Person>(person.id) shouldBeEqualTo person
    }

    @Test
    fun `insert one entity with options`() = runSuspendIO {
        val person = newPerson()

        reactiveOps.insert<Person>()
            .inTable(PERSON_TABLE_NAME)
            .one(person)
            .awaitSingle()
            .wasApplied().shouldBeTrue()

        // 이미 있기 때문에 insert 되지 않는다 
        val options = insertOptions { withIfNotExists() }
        val writeResult = reactiveOps.insert<Person>()
            .inTable(PERSON_TABLE_NAME)
            .withOptions(options)
            .one(person)
            .awaitSingle()
        writeResult.wasApplied().shouldBeFalse()

        // 기존에 없으므로 insert 된다
        val person2 = newPerson()
        val writeResult2 = reactiveOps.insert<Person>()
            .inTable(PERSON_TABLE_NAME)
            .withOptions(options)
            .one(person2)
            .awaitSingle()
        writeResult2.wasApplied().shouldBeTrue()
        writeResult2.entity shouldBeEqualTo person2
    }
}
