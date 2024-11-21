package io.bluetape4k.workshop.redis.cache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.RedisServer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class RedisCacheApplication {

    companion object: KLogging() {
        @JvmStatic
        val redisServer = RedisServer.Launcher.redis
    }
}

fun main(vararg args: String) {
    runApplication<RedisCacheApplication>(*args) {
        setAdditionalProfiles("app")
    }
}
