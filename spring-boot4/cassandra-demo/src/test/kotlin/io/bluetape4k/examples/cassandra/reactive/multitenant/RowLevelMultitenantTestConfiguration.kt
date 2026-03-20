package io.bluetape4k.examples.cassandra.reactive.multitenant

import io.bluetape4k.examples.cassandra.AbstractReactiveCassandraTestConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.data.cassandra.repository.config.EnableReactiveCassandraRepositories
import org.springframework.data.spel.spi.EvaluationContextExtension
import org.springframework.data.spel.spi.ReactiveEvaluationContextExtension
import reactor.core.publisher.Mono

@EntityScan(basePackageClasses = [Employee::class])
@EnableReactiveCassandraRepositories(basePackageClasses = [EmployeeRepository::class])
class RowLevelMultitenantTestConfiguration: AbstractReactiveCassandraTestConfiguration() {

    //
    // 한 테이블에서 multi-tenant 를 지원하기 위해, tenant-id를 partition key 로 사용하도록 합니다.
    //

    @Bean
    fun tenantExtension(): ReactiveTenantExtension = ReactiveTenantExtension.INSTANCE

    enum class ReactiveTenantExtension: ReactiveEvaluationContextExtension {
        INSTANCE {
            override fun getExtensionId(): String = "my-reactive-tenant-extension"

            override fun getExtension(): Mono<out EvaluationContextExtension> {
                return Mono.deferContextual { cv -> Mono.just(TenantExtension(cv.get(Tenant::class.java))) }
            }
        };
    }
}
