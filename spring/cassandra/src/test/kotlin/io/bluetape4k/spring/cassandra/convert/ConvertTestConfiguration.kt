package io.bluetape4k.spring.cassandra.convert

import io.bluetape4k.spring.cassandra.convert.model.CounterEntity
import io.bluetape4k.spring.cassandra.domain.model.AllPossibleTypes
import org.springframework.boot.autoconfigure.domain.EntityScan

@EntityScan(basePackageClasses = [AllPossibleTypes::class, CounterEntity::class])
class ConvertTestConfiguration: io.bluetape4k.spring.cassandra.AbstractReactiveCassandraTestConfiguration() {
}
