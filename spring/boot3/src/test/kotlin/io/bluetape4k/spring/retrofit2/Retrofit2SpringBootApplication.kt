package io.bluetape4k.spring.retrofit2

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.retrofit2.services.httpbin.HttpbinApi
import io.bluetape4k.spring.retrofit2.services.jsonplaceholder.JsonPlaceHolderApi
import io.bluetape4k.support.uninitialized
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component
import retrofit2.awaitResponse

@SpringBootApplication
class Retrofit2SpringBootApplication {

    companion object: KLoggingChannel()

    @Component
    class Retrofit2SampleRunner: CommandLineRunner {

        @Autowired
        private val httpbinApi: HttpbinApi = uninitialized()

        @Autowired
        private val jsonPlaceHolderApi: JsonPlaceHolderApi = uninitialized()

        override fun run(vararg args: String?) {
            runBlocking {
                val users = jsonPlaceHolderApi.getUsers().awaitResponse()
                log.debug { "users=$users" }
            }
        }
    }
}

fun main(vararg args: String) {
    runApplication<Retrofit2SpringBootApplication>(*args)
}
