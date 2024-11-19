package io.bluetape4k.feign.spring

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.uninitialized
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication
@EnableFeignClients
class JsonPlaceApplication {

    companion object: KLogging()

    @Autowired
    private val jsonPlaceClient: JsonPlaceClient = uninitialized()
}

fun main(args: Array<String>) {
    runApplication<JsonPlaceApplication>(*args) {
        webApplicationType = WebApplicationType.REACTIVE
    }
}
