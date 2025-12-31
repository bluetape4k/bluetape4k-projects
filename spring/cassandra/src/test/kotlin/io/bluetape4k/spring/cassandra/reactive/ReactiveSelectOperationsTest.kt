package io.bluetape4k.spring.cassandra.reactive

import com.datastax.oss.driver.api.core.uuid.Uuids
import io.bluetape4k.cassandra.cql.simpleStatement
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring.cassandra.AbstractCassandraCoroutineTest
import io.bluetape4k.spring.cassandra.AbstractReactiveCassandraTestConfiguration
import io.bluetape4k.spring.cassandra.cast
import io.bluetape4k.spring.cassandra.query.eq
import io.bluetape4k.spring.cassandra.suspendCount
import io.bluetape4k.spring.cassandra.suspendExists
import io.bluetape4k.spring.cassandra.suspendInsert
import io.bluetape4k.spring.cassandra.suspendTruncate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeIn
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.dao.IncorrectResultSizeDataAccessException
import org.springframework.data.annotation.Id
import org.springframework.data.cassandra.core.ReactiveCassandraOperations
import org.springframework.data.cassandra.core.mapping.Column
import org.springframework.data.cassandra.core.mapping.Indexed
import org.springframework.data.cassandra.core.mapping.Table
import org.springframework.data.cassandra.core.query
import org.springframework.data.cassandra.core.query.Query
import org.springframework.data.cassandra.core.query.query
import org.springframework.data.cassandra.core.query.where
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@SpringBootTest
class ReactiveSelectOperationsTest(
    @param:Autowired private val operations: ReactiveCassandraOperations,
): AbstractCassandraCoroutineTest("reactive-select-op") {

    companion object: KLoggingChannel() {
        private const val PERSON_TABLE_NAME = "select_op_person"
    }

    @Configuration
    class TestConfiguration: AbstractReactiveCassandraTestConfiguration()

    private lateinit var han: Person
    private lateinit var luke: Person

    @BeforeEach
    fun beforeEach() {
        runBlocking(Dispatchers.IO) {
            operations.suspendTruncate<Person>()

            han = newPerson()
            luke = newPerson()

            operations.suspendInsert(han)
            operations.suspendInsert(luke)
        }
    }

    @Test
    fun `context loading`() {
        operations.shouldNotBeNull()
    }

    @Test
    fun `find all with execution profile`() {
        // NOTE: Profile을 이용하여, Consistency Level, PageSize, Keyspace 등을 동적으로 설정할 수 있다.
        // 참고 : https://docs.datastax.com/en/developer/java-driver/4.15/manual/core/configuration/

        session.context.config.profiles.forEach { (name, profile) ->
            println("profile name : $name")
            println("profile options : ${profile.entrySet().joinToString()}")
        }

        val stmt = simpleStatement("SELECT * FROM $PERSON_TABLE_NAME") {
            setExecutionProfileName("olap")
        }
        // ExecutionProfileResolver.from("olap").apply(stmt)
        session.execute(stmt).all().shouldHaveSize(2)
    }

    @Test
    fun `find all by query`() = runSuspendIO {
        val result = operations.query<Person>().all().asFlow().toList()
        result shouldHaveSize 2
        result shouldContainSame listOf(han, luke)
    }

    @Test
    fun `find all with collection`() = runSuspendIO {
        val result = operations.query<Human>()
            .inTable(PERSON_TABLE_NAME)
            .all()
            .asFlow()
            .toList()

        result shouldHaveSize 2
        result shouldContainSame listOf(Human(han.id!!), Human(luke.id!!))
    }

    @Test
    fun `find all with projection`() = runSuspendIO {
        val result = operations.query<Person>()
            .cast<Jedi>()
            .all()
            .asFlow()
            .toList()

        result.all { it is Jedi }.shouldBeTrue()
        result shouldHaveSize 2
        result.map { it.firstName } shouldContainSame listOf(han.firstName, luke.firstName)
    }

    @Test
    fun `find by returning all values as closed interface porjection`() = runSuspendIO {
        val result = operations
            .query<Person>()
            .cast<PersonProjection>()
            .all()
            .asFlow()
            .toList()

        assertTrue { result.all { it is PersonProjection } }
        result shouldHaveSize 2
        result.map { it.firstName } shouldContainSame listOf(han.firstName, luke.firstName)
    }

    @Test
    fun `find by`() = runSuspendIO {
        val result = operations.query<Person>().matching(queryLuke()).all().asFlow().toList()
        result shouldBeEqualTo listOf(luke)
    }

    @Test
    fun `find by no match`() = runSuspendIO {
        val result = operations.query<Person>().matching(querySpock()).all().asFlow().toList()
        result.shouldBeEmpty()
    }

    @Test
    fun `find by too many results`() = runSuspendIO {
        assertFailsWith<IncorrectResultSizeDataAccessException> {
            operations.query<Person>().one().awaitSingleOrNull()
        }
    }

    @Test
    fun `find by returing first`() = runSuspendIO {
        val result = operations.query<Person>().matching(queryLuke()).first().awaitSingleOrNull()
        result shouldBeEqualTo luke
    }

    @Test
    fun `find by returing first for many results`() = runSuspendIO {
        val result = operations.query<Person>().first().awaitSingleOrNull()
        result shouldBeIn arrayOf(han, luke)
    }

    @Test
    fun `find by returning first as closed interface projection`() = runSuspendIO {
        val result = operations.query<Person>()
            .cast<PersonProjection>()
            .matching(query(where("firstName").eq(han.firstName)).withAllowFiltering())
            .first()
            .awaitSingleOrNull()

        result shouldBeInstanceOf PersonProjection::class
        result!!.firstName shouldBeEqualTo han.firstName
    }

    @Test
    fun `find by returning first as open interface projection`() = runSuspendIO {
        val result = operations.query<Person>()
            .cast<PersonSpELProjection>()
            .matching(query(where("firstName").eq(han.firstName)).withAllowFiltering())
            .first()
            .awaitSingleOrNull()

        result shouldBeInstanceOf PersonSpELProjection::class
        result!!.name shouldBeEqualTo han.firstName
    }

    @Test
    fun `조건절 없이 모든 레코드 Count 얻기`() = runSuspendIO {
        val count = operations.suspendCount<Person>()
        count shouldBeEqualTo 2L

        val count2 = operations.query<Person>().count().awaitSingle()
        count2 shouldBeEqualTo 2L
    }

    @Test
    fun `조건절에 매칭되는 레코드의 count 얻기`() = runSuspendIO {
        val count = operations.query<Person>()
            .matching(query(where("firstName").eq(luke.firstName)).withAllowFiltering())
            .count()
            .awaitSingle()

        count shouldBeEqualTo 1L
    }

    @Test
    fun `조건절 없이 모든 레코드 exists`() = runSuspendIO {
        operations.query<Person>().exists().awaitSingle().shouldBeTrue()
    }

    @Test
    fun `조건절에 매칭되는 레코드의 exists 얻기`() = runSuspendIO {
        operations.query<Person>()
            .matching(query(where("firstName").eq(luke.firstName)).withAllowFiltering())
            .exists()
            .awaitSingle()
            .shouldBeTrue()

        operations.query<Person>()
            .matching(query(where("firstName").eq("not-exists")).withAllowFiltering())
            .exists()
            .awaitSingle()
            .shouldBeFalse()
    }

    @Test
    fun `레코드가 없는 테이블의 exists`() = runSuspendIO {
        operations.suspendTruncate<Person>()
        operations.suspendExists<Person>().shouldBeFalse()
        operations.query<Person>().exists().awaitSingle().shouldBeFalse()
    }

    @Test
    fun `조건에 매칭되는 것이 없는 경우 exists는 false 반환`() = runSuspendIO {
        operations.query<Person>().matching(querySpock()).exists().awaitSingle().shouldBeFalse()
        operations.suspendExists<Person>(querySpock()).shouldBeFalse()
    }

    @Test
    fun `projection interface를 반환할 때는 구현된 target object 를 반환한다`() = runSuspendIO {
        val result = operations.query<Person>().cast<Contact>().all().asFlow()
        result.toList().all { it is Person }.shouldBeTrue()
    }


    private fun newPerson(): Person {
        return Person(
            id = Uuids.timeBased().toString(),
            firstName = faker.name().firstName(),
            lastName = faker.name().lastName()
        )
    }

    private interface Contact

    @Table(PERSON_TABLE_NAME)
    data class Person(
        @field:Id val id: String? = null,
        @field:Indexed val firstName: String? = null,
        @field:Indexed val lastName: String? = null,
    ): Contact

    private interface PersonProjection {
        val firstName: String?
    }

    private interface PersonSpELProjection {
        @get:Value("#{target.firstName}")
        val name: String?
    }

    private data class Human(@field:Id var id: String)

    private data class Jedi(
        @field:Column("firstName")
        var firstName: String? = null,
    )

    private data class Sith(val rank: String)

    private interface PlanetProjection {
        val name: String
    }

    private interface PlatnetSpELProjection {
        @get:Value("#{target.name}")
        val id: String
        // @Value("#{target.name}")
        // fun getId(): String
    }

    private fun queryLuke(): Query =
        query(where("firstName").eq(luke.firstName)).withAllowFiltering()

    private fun querySpock(): Query =
        query(where("firstName").eq("spock")).withAllowFiltering()
}
