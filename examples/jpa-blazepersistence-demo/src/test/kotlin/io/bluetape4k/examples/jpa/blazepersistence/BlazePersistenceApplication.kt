package io.bluetape4k.examples.jpa.blazepersistence

import io.bluetape4k.logging.KLogging
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

/**
 * Blaze Persistence JPA example용 Spring Boot test application입니다.
 */
@SpringBootApplication
@EnableJpaAuditing(modifyOnCreate = true)
class BlazePersistenceApplication {

    companion object: KLogging()
}
