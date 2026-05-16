package io.bluetape4k.examples.jpa.blazepersistence

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import net.datafaker.Faker
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

/**
 * Base Spring Boot test configuration for the Blaze Persistence examples.
 */
@SpringBootTest(
    classes = [BlazePersistenceApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:blaze-demo;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.jdbc.batch_size=30",
        "spring.jpa.properties.hibernate.order_inserts=true",
        "spring.jpa.properties.hibernate.order_updates=true",
    ],
)
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractBlazePersistenceTest {

    companion object: KLogging() {
        @JvmStatic
        val faker: Faker = Fakers.faker
    }

    @Autowired
    protected lateinit var tem: TestEntityManager

    protected val em: EntityManager get() = tem.entityManager
    protected val emf: EntityManagerFactory get() = em.entityManagerFactory

    protected fun flushAndClear() {
        tem.flush()
        tem.clear()
    }
}
