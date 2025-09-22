package io.bluetape4k.spring.cassandra.domain.schema

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring.cassandra.domain.model.AllPossibleTypes
import io.bluetape4k.spring.cassandra.schema.SchemaGenerator
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.cassandra.core.CassandraOperations

@SpringBootTest(classes = [SchemaBuilderTestConfiguration::class])
class SchemaBuilderTest(
    @param:Autowired private val operations: CassandraOperations,
): io.bluetape4k.spring.cassandra.AbstractCassandraTest() {

    companion object: KLoggingChannel()

    @Test
    fun `generate table schema for entity`() {
        SchemaGenerator.truncate<AllPossibleTypes>(operations)
        SchemaGenerator.potentiallyCreateTableFor<AllPossibleTypes>(operations)
        SchemaGenerator.truncate<AllPossibleTypes>(operations)
    }
}
