package io.bluetape4k.hibernate.spring.stateless

import org.hibernate.SessionFactory
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration(proxyBeanMethods = false)
class StatelessSessionTestConfiguration {

    @Bean
    fun statelessSession(sf: SessionFactory): StatelessSessionFactoryBean {
        return StatelessSessionFactoryBean(sf)
    }
}
