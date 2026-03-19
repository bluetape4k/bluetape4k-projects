package io.bluetape4k.examples.jpa.querydsl

import io.bluetape4k.examples.jpa.querydsl.services.InitMemberService
import io.bluetape4k.logging.KLogging
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.repository.config.BootstrapMode

@SpringBootApplication
@EnableJpaAuditing(modifyOnCreate = true)
@EnableJpaRepositories(bootstrapMode = BootstrapMode.DEFERRED)
class QueryDslApplication {

    companion object: KLogging()

    @Bean
    fun initMemberService(): InitMemberService = InitMemberService()
}
