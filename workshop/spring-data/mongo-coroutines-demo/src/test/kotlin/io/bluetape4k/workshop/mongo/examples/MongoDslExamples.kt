package io.bluetape4k.workshop.mongo.examples

import io.bluetape4k.logging.KLogging
import io.bluetape4k.workshop.mongo.AbstractMongoTest
import io.bluetape4k.workshop.mongo.domain.Person
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.dropCollection
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.insert
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.regex

class MongoDslExamples(
    @Autowired private val operations: MongoOperations,
): AbstractMongoTest() {

    companion object: KLogging()

    @BeforeEach
    fun beforeEach() {
        operations.dropCollection<Person>()
    }

    @Test
    fun `simple type-safe find`() {
        val person1 = operations.insert<Person>(newPerson())
        val person2 = operations.insert<Person>(newPerson())

        val persons = operations.find<Person>(Query(Person::firstname isEqualTo person2.firstname!!))

        persons shouldHaveSize 1
        persons shouldBeEqualTo listOf(person2)

        val persons2 = operations.find<Person>(Query(Person::firstname isEqualTo person1.firstname!!))
        persons2 shouldHaveSize 1
        persons2 shouldBeEqualTo listOf(person1)
    }

    @Test
    fun `more complex type-safe find`() {
        val person1 = operations.insert<Person>().one(newPerson())
        val person2 = operations.insert<Person>().one(newPerson().copy(firstname = "Sunghyouk"))

        val criteria = Criteria().andOperator(
            Person::lastname isEqualTo person2.lastname!!,
            Person::firstname regex "^Sun.*"
        )
        val persons = operations.find<Person>(Query(criteria))

        persons shouldHaveSize 1
        persons.first() shouldBeEqualTo person2
        persons.first() shouldNotBeEqualTo person1
    }
}
