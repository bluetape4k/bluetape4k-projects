package io.bluetape4k.examples.cassandra.auditing

import com.datastax.oss.driver.api.core.CqlSession
import io.bluetape4k.examples.cassandra.AbstractReactiveCassandraTestConfiguration
import io.bluetape4k.logging.KLogging
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.data.cassandra.config.EnableReactiveCassandraAuditing
import org.springframework.data.cassandra.core.convert.CassandraCustomConversions
import org.springframework.data.cassandra.core.convert.MappingCassandraConverter
import org.springframework.data.cassandra.core.mapping.CassandraMappingContext
import org.springframework.data.cassandra.core.mapping.SimpleUserTypeResolver
import org.springframework.data.cassandra.repository.config.EnableReactiveCassandraRepositories
import org.springframework.data.domain.AuditorAware
import org.springframework.data.domain.ReactiveAuditorAware
import reactor.core.publisher.Mono
import java.util.*


@EntityScan(basePackageClasses = [AuditedPerson::class])
@EnableReactiveCassandraRepositories(basePackageClasses = [AuditedPersonRepository::class])
@EnableReactiveCassandraAuditing
class AuditingTestConfiguration: AbstractReactiveCassandraTestConfiguration() {

    companion object: KLogging() {
        const val ACTOR = "Some user"
    }

    // Session 또는 ThreadLocal 값을 사용하는 것이 좋다
    @Bean
    fun auditorAware(): AuditorAware<String> =
        AuditorAware { Optional.of(ACTOR) }

    // Reactive 사용 시에는 별도로 [ReactiveAuditorAware] 를 지정해주어야 합니다.
    @Bean
    fun reactiveAuditorAware(): ReactiveAuditorAware<String> =
        ReactiveAuditorAware { Mono.just(ACTOR) }


    @Bean
    fun cassandraConverter(
        mapping: CassandraMappingContext,
        conversions: CassandraCustomConversions,
        session: CqlSession,
    ): MappingCassandraConverter {
        return MappingCassandraConverter(mapping).apply {
            codecRegistry = session.context.codecRegistry
            customConversions = conversions
            userTypeResolver = SimpleUserTypeResolver(session)
        }
    }
}
