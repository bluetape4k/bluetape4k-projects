package io.bluetape4k.workshop.mongo

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.MongoDBServer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.core.mapping.event.LoggingEventListener

@SpringBootApplication
class MongoApplication {

    companion object: KLogging() {
        val mongodb = MongoDBServer.Launcher.mongoDB
    }

    /**
     * MongoDB 관련 EmitValue 를 Log 에 쓰는 Listener 입니다.
     */
    @Bean
    fun mongoEventListener() = LoggingEventListener()
}

fun main(vararg args: String) {
    runApplication<MongoApplication>(*args)
}
