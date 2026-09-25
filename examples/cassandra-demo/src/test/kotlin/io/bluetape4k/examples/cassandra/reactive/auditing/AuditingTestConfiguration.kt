package io.bluetape4k.examples.cassandra.reactive.auditing

import io.bluetape4k.examples.cassandra.AbstractReactiveCassandraTestConfiguration
import io.bluetape4k.logging.KLogging
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.data.cassandra.config.EnableReactiveCassandraAuditing
import org.springframework.data.cassandra.repository.config.EnableReactiveCassandraRepositories
import org.springframework.data.domain.ReactiveAuditorAware
import reactor.core.publisher.Mono

@EntityScan(basePackageClasses = [Order::class])
@EnableReactiveCassandraRepositories(basePackageClasses = [OrderRepository::class])
@EnableReactiveCassandraAuditing
class AuditingTestConfiguration: AbstractReactiveCassandraTestConfiguration() {

    companion object: KLogging() {
        const val CURRENT_USER = "the-current-user"
    }

    @Bean
    fun reactiveAuditorAware(): ReactiveAuditorAware<String> =
        ReactiveAuditorAware { Mono.just(CURRENT_USER) }

}
