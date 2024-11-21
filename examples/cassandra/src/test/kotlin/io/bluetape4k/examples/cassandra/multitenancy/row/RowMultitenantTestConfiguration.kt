package io.bluetape4k.examples.cassandra.multitenancy.row

import io.bluetape4k.examples.cassandra.AbstractReactiveCassandraTestConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.data.cassandra.repository.config.EnableReactiveCassandraRepositories

@EntityScan(basePackageClasses = [Employee::class])
@EnableReactiveCassandraRepositories(basePackageClasses = [EmployeeRepository::class])
class RowMultitenantTestConfiguration: AbstractReactiveCassandraTestConfiguration()
