package io.bluetape4k.examples.cassandra.streamnullable

import io.bluetape4k.examples.cassandra.AbstractReactiveCassandraTestConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories

@EntityScan(basePackageClasses = [Person::class])
@EnableCassandraRepositories(basePackageClasses = [PersonRepository::class])
class StreamNullableTestConfiguration: AbstractReactiveCassandraTestConfiguration()
