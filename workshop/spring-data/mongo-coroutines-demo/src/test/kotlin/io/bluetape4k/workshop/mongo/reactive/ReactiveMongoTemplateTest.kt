package io.bluetape4k.workshop.mongo.reactive

import io.bluetape4k.coroutines.flow.extensions.log
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.workshop.mongo.domain.Person
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.count
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

class ReactiveMongoTemplateTest(
    @Autowired private val operations: ReactiveMongoTemplate,
): AbstractReactiveMongoTest(operations) {

    companion object: KLogging()

    @Test
    fun `insert and count data`() = runSuspendIO {
        val prevCount = operations.count<Person>(Query())
            .doOnNext { println(it) }
            .awaitSingle()

        prevCount shouldBeEqualTo 4L

        val persons = listOf(
            newPerson(),
            newPerson()
        )
        operations.insertAll(persons).awaitLast()

        val count = operations.count<Person>(Query())
            .log()
            .awaitSingle()

        count shouldBeEqualTo prevCount + 2L
    }

    @Test
    fun `find by query`() = runTest {
        val query = Query.query(Criteria.where(Person::lastname.name).`is`("White"))
        val persons = operations.find<Person>(query).asFlow().log("persons").toList()

        persons shouldHaveSize 2
    }
}
