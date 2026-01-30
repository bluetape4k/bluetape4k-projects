package io.bluetape4k.exposed.dao.id

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.params.provider.Arguments

abstract class AbstractCustomIdTableTest: AbstractExposedTest() {

    companion object: KLogging()

    fun getTestDBAndEntityCount(): List<Arguments> {
        val recordCounts = listOf(10, 50, 100, 300)

        return TestDB.enabledDialects().flatMap { testDB ->
            recordCounts.map { entityCount ->
                Arguments.of(testDB, entityCount)
            }
        }
    }
}
