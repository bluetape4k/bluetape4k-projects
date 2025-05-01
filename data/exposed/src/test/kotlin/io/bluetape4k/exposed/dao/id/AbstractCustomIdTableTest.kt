package io.bluetape4k.exposed.dao.id

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.params.provider.Arguments

abstract class AbstractCustomIdTableTest: AbstractExposedTest() {

    companion object: KLogging()

    fun getTestDBAndEntityCount(): List<Arguments> {
        val recordCounts = listOf(100, 1000)

        return TestDB.enabledDialects().map { testDB ->
            recordCounts.map { entityCount ->
                Arguments.of(testDB, entityCount)
            }
        }.flatten()
    }
}
