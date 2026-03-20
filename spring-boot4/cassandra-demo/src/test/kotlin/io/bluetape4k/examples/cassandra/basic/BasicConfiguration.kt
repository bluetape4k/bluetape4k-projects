package io.bluetape4k.examples.cassandra.basic

import io.bluetape4k.examples.cassandra.AbstractReactiveCassandraTestConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.data.cassandra.config.EnableReactiveCassandraAuditing
import org.springframework.data.cassandra.repository.config.EnableReactiveCassandraRepositories

@EntityScan(basePackageClasses = [BasicUser::class])
@EnableReactiveCassandraRepositories(basePackageClasses = [BasicUserRepository::class])
@EnableReactiveCassandraAuditing
class BasicConfiguration: AbstractReactiveCassandraTestConfiguration()
