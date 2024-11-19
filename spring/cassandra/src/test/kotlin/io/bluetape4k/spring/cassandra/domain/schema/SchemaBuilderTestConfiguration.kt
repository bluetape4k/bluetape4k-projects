package io.bluetape4k.spring.cassandra.domain.schema

import io.bluetape4k.spring.cassandra.convert.model.CounterEntity
import io.bluetape4k.spring.cassandra.domain.model.AllPossibleTypes
import org.springframework.boot.autoconfigure.domain.EntityScan

@EntityScan(basePackageClasses = [AllPossibleTypes::class, CounterEntity::class])
class SchemaBuilderTestConfiguration:
    io.bluetape4k.spring.cassandra.AbstractReactiveCassandraTestConfiguration() {
}
