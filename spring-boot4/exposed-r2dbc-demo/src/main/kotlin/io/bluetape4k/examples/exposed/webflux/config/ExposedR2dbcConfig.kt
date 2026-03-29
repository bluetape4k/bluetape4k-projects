package io.bluetape4k.examples.exposed.webflux.config

import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * WebFlux 데모에서 사용할 Exposed R2DBC 데이터베이스를 구성합니다.
 */
@Configuration(proxyBeanMethods = false)
class ExposedR2dbcConfig {

    /**
     * Exposed `suspendTransaction` 호출에서 사용할 기본 R2DBC 데이터베이스 인스턴스입니다.
     */
    @Bean
    fun r2dbcDatabase(
        @Value("\${spring.r2dbc.url}") url: String,
        @Value("\${spring.r2dbc.username:}") username: String,
        @Value("\${spring.r2dbc.password:}") password: String,
    ): R2dbcDatabase = R2dbcDatabase.connect(
        url = url,
        driver = "h2",
        user = username,
        password = password,
    )
}
