package io.bluetape4k.workshop.mongo.examples

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.workshop.mongo.AbstractMongoTest
import io.bluetape4k.workshop.mongo.domain.Person
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldNotBeNull
import org.bson.Document
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.asType
import org.springframework.data.mongodb.core.createCollection
import org.springframework.data.mongodb.core.dropCollection
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findOne
import org.springframework.data.mongodb.core.insert
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo

class MongoTemplateExamples(
    @Autowired private val operations: MongoOperations,
): AbstractMongoTest() {

    companion object: KLogging()

    @BeforeEach
    fun beforeEach() {
        operations.dropCollection<Person>()
    }

    @Test
    fun `should create collection leveraging reified type parameters`() {
        operations.createCollection<Person>()
        operations.collectionNames shouldContain "person"
    }

    @Test
    fun `should insert and find person in a fluent API style`() {
        val person = operations.insert<Person>().inCollection("person").one(newPerson())

        val persons = operations.query<Person>()
            .matching(Query.query(Criteria.where(Person::firstname.name).isEqualTo(person.firstname)))
            .all()

        persons.size shouldBeEqualTo 1
        persons shouldContainSame listOf(person)
    }

    @Test
    fun `should insert and project query results`() {
        val person = operations.insert<Person>().inCollection("person").one(newPerson())

        val people = operations.query<Person>()
            .asType<FirstnameOnly>()
            .matching(Query.query(Criteria.where(Person::firstname.name).isEqualTo(person.firstname)))
            .oneValue()

        people.shouldNotBeNull()
        people.getFirstname() shouldBeEqualTo person.firstname
    }

    @Test
    fun `should insert and count objects in a fluent API style`() {
        val person = operations.insert<Person>().inCollection("person").one(newPerson())

        val count = operations.query<Person>()
            .matching(Query.query(Criteria.where(Person::firstname.name).isEqualTo(person.firstname)))
            .count()

        count shouldBeEqualTo 1
    }

    @Test
    fun `should insert and find person`() {
        val person = operations.insert(newPerson())

        val query = Query.query(Criteria.where(Person::firstname.name).isEqualTo(person.firstname))
        val persons = operations.find<Person>(query)

        persons.size shouldBeEqualTo 1
        persons shouldBeEqualTo listOf(person)
    }

    @Test
    fun `should apply defaulting for absent properties`() {
        val document = operations.insert<Document>().inCollection("person").one(Document("lastname", "White"))

        val persons = operations.query<Person>()
            .matching(Query.query(Criteria.where(Person::lastname.name).isEqualTo(document["lastname"])))
            .firstValue()!!

        log.debug { "Load person=$persons" }
        persons.firstname shouldBeEqualTo "Walter" // Default 값을 사용합니다.
        persons.lastname shouldBeEqualTo document["lastname"]

        val walter = operations.findOne<Document>(
            Query.query(Criteria.where(Person::lastname.name).isEqualTo(document["lastname"])),
            "person"
        )

        log.debug { "Load walter=$walter" }
        walter.shouldNotBeNull()
        walter["lastname"] shouldBeEqualTo document["lastname"]
        walter.containsKey("_id").shouldBeTrue()
        walter.containsKey("firstname").shouldBeFalse()
    }

    interface FirstnameOnly {
        fun getFirstname(): String
    }
}
