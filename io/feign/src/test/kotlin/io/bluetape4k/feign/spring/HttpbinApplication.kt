package io.bluetape4k.feign.spring

import io.bluetape4k.logging.KLogging
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication
@EnableFeignClients
class HttpbinApplication {
    companion object: KLogging()
}

fun main(args: Array<String>) {
    runApplication<HttpbinApplication>(*args)
}
