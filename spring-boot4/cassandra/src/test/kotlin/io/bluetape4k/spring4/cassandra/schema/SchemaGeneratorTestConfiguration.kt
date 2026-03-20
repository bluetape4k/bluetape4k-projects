package io.bluetape4k.spring4.cassandra.schema

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring4.cassandra.AbstractCassandraTestConfiguration
import io.bluetape4k.spring4.cassandra.domain.model.AllPossibleTypes
import org.springframework.boot.autoconfigure.domain.EntityScan

@EntityScan(basePackageClasses = [AllPossibleTypes::class])
class SchemaGeneratorTestConfiguration: AbstractCassandraTestConfiguration() {

    companion object: KLoggingChannel()
}
