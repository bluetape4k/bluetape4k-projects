package io.bluetape4k.examples.cassandra.optimisticlocking

import io.bluetape4k.examples.cassandra.AbstractReactiveCassandraTestConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.data.cassandra.repository.config.EnableReactiveCassandraRepositories

@EntityScan(basePackageClasses = [OptimisticPerson::class])
@EnableReactiveCassandraRepositories(basePackageClasses = [OptimisticPersonRepository::class])
class OptimisticLockTestConfiguration: AbstractReactiveCassandraTestConfiguration()
