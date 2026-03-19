package io.bluetape4k.examples.redisson.coroutines.cachestrategy

import io.bluetape4k.logging.KLogging
import org.redisson.spring.starter.RedissonAutoConfigurationV2
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    exclude = [RedissonAutoConfigurationV2::class]
)
class CacheApplication {
    companion object: KLogging()
}

fun main(args: Array<String>) {
    runApplication<CacheApplication>(*args)
}
