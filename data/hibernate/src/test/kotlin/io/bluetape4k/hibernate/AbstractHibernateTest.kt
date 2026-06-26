package io.bluetape4k.hibernate

import io.bluetape4k.hibernate.converters.EncryptedStringConverterKeysets
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.tink.aeadKeysetHandle
import io.bluetape4k.tink.daeadKeysetHandle
import io.bluetape4k.tink.keyset.toJsonKeyset
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import net.datafaker.Faker
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

/**
 * `@SpringBootTest`를 사용하려면 SpringBootApplication 이 정의되어 있어야 합니다 (see [HibernateApplication])
 *
 * 참고: [Hibernate Configuration](https://docs.jboss.org/hibernate/orm/7/userguide/html_single/Hibernate_User_Guide.html#configurations)
 */
@SpringBootTest(
    classes = [HibernateApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.show-sql=true",
        "spring.flyway.enabled=false",

        // DML 작업
        "spring.jpa.properties.hibernate.hbm2ddl.auto=update",

        "spring.jpa.properties.hibernate.show_sql=false",
        "spring.jpa.properties.hibernate.format_sql=true",
        "spring.jpa.properties.hibernate.highlight_sql=true",
        // 성능 측정 정보 제공
        "spring.jpa.properties.hibernate.generate_statistics=true",
        //
        // NOTE: literal 을 parameter 로 binding 시킵니다
        // 참고 : https://vladmihalcea.com/how-does-hibernate-handle-jpa-criteria-api-literals/
        "spring.jpa.properties.hibernate.criteria.literal_handling_mode=bind",

        // NOTE: slow query 지정
        "spring.jpa.properties.hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS=10",

        // Second Level Cache
        "spring.jpa.properties.hibernate.cache.use_second_level_cache=false",

        // Query Cache - WHY: disabled to avoid Caffeine JCache singleton being closed by a concurrent
        // Spring context teardown, which causes IllegalStateException in other running test contexts.
        "spring.jpa.properties.hibernate.cache.use_query_cache=false",

        // https://vladmihalcea.com/improve-statement-caching-efficiency-in-clause-parameter-padding/
        "spring.jpa.properties.hibernate.query.in_clause_parameter_padding=true",
        "spring.jpa.properties.hibernate.query.plan_cache_max_size=2048",
        "spring.jpa.properties.hibernate.query.plan_parameter_metadata_max_size=128",

        // JPA Batch Insert
        "spring.jpa.properties.hibernate.jdbc.batch_size=30",
        "spring.jpa.properties.hibernate.order_inserts=true",
        "spring.jpa.properties.hibernate.order_updates=true"
    ]
)
@Transactional
abstract class AbstractHibernateTest {
    companion object: KLogging() {
        @JvmStatic
        val faker: Faker = Fakers.faker
    }

    @Autowired
    protected lateinit var tem: TestEntityManager

    protected val em: EntityManager get() = tem.entityManager
    protected val emf: EntityManagerFactory get() = em.entityManagerFactory

    @BeforeEach
    fun configureEncryptedStringConverters() {
        EncryptedStringConverterKeysets.configureAesKeyset(aeadKeysetHandle().toJsonKeyset())
        EncryptedStringConverterKeysets.configureDeterministicKeyset(daeadKeysetHandle().toJsonKeyset())
    }

    @AfterEach
    fun resetEncryptedStringConverters() {
        EncryptedStringConverterKeysets.resetForTesting()
    }

    protected fun clear() {
        tem.clear()
    }

    protected fun flush() {
        tem.flush()
    }

    protected fun flushAndClear() {
        flush()
        clear()
    }
}
