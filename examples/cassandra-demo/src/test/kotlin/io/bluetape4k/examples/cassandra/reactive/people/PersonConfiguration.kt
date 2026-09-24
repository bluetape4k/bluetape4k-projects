package io.bluetape4k.examples.cassandra.reactive.people

import io.bluetape4k.examples.cassandra.AbstractReactiveCassandraTestConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.data.cassandra.repository.config.EnableReactiveCassandraRepositories

@EntityScan(basePackageClasses = [Person::class])
@EnableReactiveCassandraRepositories(basePackageClasses = [ReactivePersonRepository::class])
class PersonConfiguration: AbstractReactiveCassandraTestConfiguration()
