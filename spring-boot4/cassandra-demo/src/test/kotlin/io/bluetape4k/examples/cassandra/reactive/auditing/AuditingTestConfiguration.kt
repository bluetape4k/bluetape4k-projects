package io.bluetape4k.examples.cassandra.reactive.auditing

import io.bluetape4k.examples.cassandra.AbstractReactiveCassandraTestConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.data.cassandra.config.EnableReactiveCassandraAuditing
import org.springframework.data.cassandra.repository.config.EnableReactiveCassandraRepositories
import org.springframework.data.domain.ReactiveAuditorAware
import reactor.core.publisher.Mono

@EntityScan(basePackageClasses = [Order::class])
@EnableReactiveCassandraRepositories(basePackageClasses = [OrderRepository::class])
@EnableReactiveCassandraAuditing
class AuditingTestConfiguration: AbstractReactiveCassandraTestConfiguration() {

    @Bean
    fun reactiveAuditorAware(): ReactiveAuditorAware<String> =
        ReactiveAuditorAware { Mono.just("the-current-user") }

}
