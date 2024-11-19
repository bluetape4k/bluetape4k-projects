package io.bluetape4k.spring.cassandra.reactive

import io.bluetape4k.spring.cassandra.AbstractReactiveCassandraTestConfiguration
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class ReactiveTestConfiguration: AbstractReactiveCassandraTestConfiguration()
