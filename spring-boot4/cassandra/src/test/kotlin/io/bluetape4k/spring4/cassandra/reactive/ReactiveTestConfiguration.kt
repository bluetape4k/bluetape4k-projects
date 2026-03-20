package io.bluetape4k.spring4.cassandra.reactive

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring4.cassandra.AbstractReactiveCassandraTestConfiguration
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class ReactiveTestConfiguration: AbstractReactiveCassandraTestConfiguration() {

    companion object: KLoggingChannel()
}
