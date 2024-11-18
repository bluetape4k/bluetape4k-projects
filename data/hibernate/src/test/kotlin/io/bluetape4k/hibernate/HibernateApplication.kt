package io.bluetape4k.hibernate

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.repository.config.BootstrapMode

@SpringBootApplication
@EnableJpaAuditing(modifyOnCreate = true)
@EnableJpaRepositories(bootstrapMode = BootstrapMode.DEFERRED)
class HibernateApplication 
