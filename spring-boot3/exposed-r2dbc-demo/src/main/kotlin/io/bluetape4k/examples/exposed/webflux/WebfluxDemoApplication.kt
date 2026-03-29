package io.bluetape4k.examples.exposed.webflux

import io.bluetape4k.spring.data.exposed.r2dbc.repository.config.EnableExposedR2dbcRepositories
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(proxyBeanMethods = false)
@EnableExposedR2dbcRepositories(
    basePackages = ["io.bluetape4k.examples.exposed.webflux.repository"]
)
class WebfluxDemoApplication

fun main(args: Array<String>) {
    runApplication<WebfluxDemoApplication>(*args)
}
