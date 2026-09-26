package io.bluetape4k.examples.cassandra.udt

import io.bluetape4k.examples.cassandra.AbstractReactiveCassandraTestConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan

@EntityScan(basePackageClasses = [Person::class])
class UserDefinedTypeTestConfiguration: AbstractReactiveCassandraTestConfiguration() {
}
