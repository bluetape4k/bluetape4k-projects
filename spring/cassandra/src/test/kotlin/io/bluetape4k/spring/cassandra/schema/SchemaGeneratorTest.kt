package io.bluetape4k.spring.cassandra.schema

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring.cassandra.AbstractCassandraTest
import io.bluetape4k.spring.cassandra.domain.model.AllPossibleTypes
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.cassandra.core.CassandraOperations

@SpringBootTest(classes = [SchemaGeneratorTestConfiguration::class])
class SchemaGeneratorTest(
    @Autowired private val operations: CassandraOperations,
): AbstractCassandraTest() {

    companion object: KLoggingChannel()

    @Test
    fun `generate table schema for entity`() {
        SchemaGenerator.truncate<AllPossibleTypes>(operations)
        SchemaGenerator.potentiallyCreateTableFor<AllPossibleTypes>(operations)
    }
}
