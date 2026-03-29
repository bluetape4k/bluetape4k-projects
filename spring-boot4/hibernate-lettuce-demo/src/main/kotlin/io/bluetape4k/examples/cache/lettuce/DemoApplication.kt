package io.bluetape4k.examples.cache.lettuce

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(proxyBeanMethods = false)
class DemoApplication

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}
