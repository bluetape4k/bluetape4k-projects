package io.bluetape4k.hibernate

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.repository.config.BootstrapMode

@SpringBootApplication
@EnableJpaAuditing(modifyOnCreate = true)
@EnableJpaRepositories(bootstrapMode = BootstrapMode.DEFERRED)
@ComponentScan(
    basePackages = ["io.bluetape4k.hibernate"],
    excludeFilters = [ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        classes = [SpringBootApplication::class]
    )]
)
class HibernateApplication
