package io.bluetape4k.examples.cassandra.reactive.people

import io.bluetape4k.examples.cassandra.AbstractReactiveCassandraTestConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.data.cassandra.repository.config.EnableReactiveCassandraRepositories

@EntityScan(basePackageClasses = [Person::class])
@EnableReactiveCassandraRepositories(basePackageClasses = [ReactivePersonRepository::class])
class PersonConfiguration: AbstractReactiveCassandraTestConfiguration()
