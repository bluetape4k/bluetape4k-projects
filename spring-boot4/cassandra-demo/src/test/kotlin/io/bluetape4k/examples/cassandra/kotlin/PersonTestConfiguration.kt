package io.bluetape4k.examples.cassandra.kotlin

import io.bluetape4k.examples.cassandra.AbstractReactiveCassandraTestConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.data.cassandra.repository.config.EnableReactiveCassandraRepositories

/**
 * Coroutines 를 사용하려면 `@EnableReactiveCassandraRepositories` 를 적용해주어야 하고,
 * `AbstractReactiveCassandraConfiguration` 을 상속받는 Configuration 을 만들어야 한다.
 */
@EntityScan(basePackageClasses = [Person::class])
@EnableReactiveCassandraRepositories(basePackageClasses = [PersonRepository::class])
class PersonTestConfiguration: AbstractReactiveCassandraTestConfiguration()
