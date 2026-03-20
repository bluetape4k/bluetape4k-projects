package io.bluetape4k.spring4.cassandra.domain.schema

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring4.cassandra.AbstractReactiveCassandraTestConfiguration
import io.bluetape4k.spring4.cassandra.convert.model.CounterEntity
import io.bluetape4k.spring4.cassandra.domain.model.AllPossibleTypes
import org.springframework.boot.autoconfigure.domain.EntityScan

@EntityScan(basePackageClasses = [AllPossibleTypes::class, CounterEntity::class])
class SchemaBuilderTestConfiguration: AbstractReactiveCassandraTestConfiguration() {

    companion object: KLoggingChannel()
    
}
