package io.bluetape4k.examples.cassandra.event

import io.bluetape4k.examples.cassandra.AbstractReactiveCassandraTestConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean

@EntityScan(basePackageClasses = [User::class])
class EventTestConfiguration: AbstractReactiveCassandraTestConfiguration() {

    /**
     * [LoggingEventListener] 를 이용하여 Entity 변화를 기록합니다.
     */
    @Bean
    fun listener(): LoggingEventListener = LoggingEventListener()
}
