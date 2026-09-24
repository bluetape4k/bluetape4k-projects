package io.bluetape4k.examples.cassandra.projection

import io.bluetape4k.examples.cassandra.AbstractReactiveCassandraTestConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories

@EntityScan(basePackageClasses = [Customer::class])
@EnableCassandraRepositories(basePackageClasses = [CustomerRepository::class])
class ProjectionTestConfiguration: AbstractReactiveCassandraTestConfiguration()
