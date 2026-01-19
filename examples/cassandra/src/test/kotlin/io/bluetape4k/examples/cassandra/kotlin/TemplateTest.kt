package io.bluetape4k.examples.cassandra.kotlin

import com.datastax.oss.driver.api.querybuilder.QueryBuilder.insertInto
import io.bluetape4k.cassandra.querybuilder.literal
import io.bluetape4k.cassandra.quote
import io.bluetape4k.cassandra.toCqlIdentifier
import io.bluetape4k.examples.cassandra.AbstractCassandraTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.cassandra.core.CassandraOperations
import org.springframework.data.cassandra.core.asType
import org.springframework.data.cassandra.core.getTableName
import org.springframework.data.cassandra.core.insert
import org.springframework.data.cassandra.core.query
import org.springframework.data.cassandra.core.query.isEqualTo
import org.springframework.data.cassandra.core.query.query
import org.springframework.data.cassandra.core.query.where
import org.springframework.data.cassandra.core.select
import org.springframework.data.cassandra.core.truncate

@SpringBootTest(classes = [PersonTestConfiguration::class])
class TemplateTest(
    @param:Autowired private val operations: CassandraOperations,
): AbstractCassandraTest() {

    companion object: KLogging() {
        private const val PERSON_TABLE = "kotlin_people"
    }

    interface FirstnameOnly {
        val firstname: String
    }

    private fun newPerson(): Person = Person(
        faker.name().firstName(),
        faker.name().lastName()
    )


    @BeforeEach
    fun beforeEach() {
        operations.truncate<Person>()
    }

    @Test
    fun `should create collection leveraging reified type parameters`() {
        operations.getTableName<Person>() shouldBeEqualTo PERSON_TABLE.toCqlIdentifier()
    }

    @Test
    fun `should insert and find person in a fluent API style`() {
        val person = newPerson()
        operations.insert<Person>().inTable(PERSON_TABLE).one(person)

        val people = operations
            .query<Person>()
            .matching(query(where("firstname").isEqualTo(person.firstname)))
            .all()

        people shouldBeEqualTo listOf(person)
    }

    @Test
    fun `should insert and project query results`() {
        val person = newPerson()
        operations.insert<Person>().inTable(PERSON_TABLE).one(person)

        val firstnameOnly = operations
            .query<Person>()
            .asType<FirstnameOnly>()
            .matching(query(where("firstname").isEqualTo(person.firstname)))
            .oneValue()

        firstnameOnly?.firstname shouldBeEqualTo person.firstname
    }

    @Test
    fun `should insert and count objects in a fluent API style`() {
        val person = newPerson()
        operations.insert<Person>().inTable(PERSON_TABLE).one(person)

        val count = operations
            .query<Person>()
            .matching(query(where("firstname").isEqualTo(person.firstname)))
            .count()

        count shouldBeEqualTo 1
    }

    @Test
    fun `should insert and find person`() {
        val person = newPerson()
        operations.insert<Person>().inTable(PERSON_TABLE).one(person)

        val people = operations
            .select<Person>(
                query(where("firstname").isEqualTo(person.firstname))
            )

        people shouldBeEqualTo listOf(person)
    }

    @Test
    fun `should apply defaulting for absent properties`() {
        val person = newPerson()

        // Spring의 InsertOperations 를 사용하는 것을 추천합니다.
        operations.cqlOperations
            .execute(
                insertInto(PERSON_TABLE)
                    .value("firstname", person.firstname.literal())
                    .asCql()
            )

        // RowMapper를 data class 로 mapping 할 때 속성에 기본값이 있다면 그 값을 씁니다.
        val loaded = operations
            .query<Person>()
            .matching(query(where("firstname").isEqualTo(person.firstname)))
            .firstValue()!!

        loaded.firstname shouldBeEqualTo person.firstname

        // Spring의 SelectOperations 를 사용하는 것을 추천합니다.
        val resultSet = operations.cqlOperations
            .queryForResultSet("SELECT * FROM $PERSON_TABLE WHERE firstname=${person.firstname.quote()}")
        val row = resultSet.one()!!

        row.getString("firstname") shouldBeEqualTo person.firstname
        row.getString("lastname").shouldBeNull()
    }
}
