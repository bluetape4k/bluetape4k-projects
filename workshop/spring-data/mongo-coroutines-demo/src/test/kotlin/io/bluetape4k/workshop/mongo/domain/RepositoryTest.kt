package io.bluetape4k.workshop.mongo.domain

import io.bluetape4k.logging.KLogging
import io.bluetape4k.workshop.mongo.AbstractMongoTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.dropCollection

class RepositoryTest @Autowired constructor(
    private val repository: PersonRepository,
    private val operations: MongoOperations,
): AbstractMongoTest() {

    companion object: KLogging()

    @BeforeEach
    fun beforeEach() {
        operations.dropCollection<Person>()
    }

    @Test
    fun `should find one person`() {
        val person = repository.save(newPerson())

        val loaded = repository.findOneByFirstname(person.firstname!!)
        loaded shouldBeEqualTo person
    }
}
