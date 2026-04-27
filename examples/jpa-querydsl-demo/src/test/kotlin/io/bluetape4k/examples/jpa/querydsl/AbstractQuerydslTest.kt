package io.bluetape4k.examples.jpa.querydsl

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import net.datafaker.Faker
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

/**
 * `@SpringBootTest`를 사용하려면 SpringBootApplication 이 정의되어 있어야 합니다 (see [QueryDslApplication])
 *
 * 참고: [Hibernate Configuration](https://docs.jboss.org/hibernate/orm/7/userguide/html_single/Hibernate_User_Guide.html#configurations)
 */
@SpringBootTest(
    classes = [QueryDslApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false",

        // DML 작업
        "spring.jpa.properties.hibernate.hbm2ddl.auto=update",

        "spring.jpa.properties.hibernate.show_sql=false",
        "spring.jpa.properties.hibernate.format_sql=true",
        "spring.jpa.properties.hibernate.highlight_sql=true",
        // 성능 측정 정보 제공
        "spring.jpa.properties.hibernate.generate_statistics=false",
        //
        // NOTE: literal 을 parameter 로 binding 시킵니다
        "spring.jpa.properties.hibernate.criteria.literal_handling_mode=bind",

        // NOTE: slow query 지정
        "spring.jpa.properties.hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS=10",

        // Second Level Cache
        "spring.jpa.properties.hibernate.cache.use_second_level_cache=true",

        // Query Cache
        "spring.jpa.properties.hibernate.cache.use_query_cache=true",

        // https://vladmihalcea.com/improve-statement-caching-efficiency-in-clause-parameter-padding/
        "spring.jpa.properties.hibernate.query.in_clause_parameter_padding=true",
        "spring.jpa.properties.hibernate.query.plan_cache_max_size=2048",
        "spring.jpa.properties.hibernate.query.plan_parameter_metadata_max_size=128",

        // Caching Providers (hibernate-jcache 와 caffeine-jcache 를 참조해야 합니다)
        "spring.jpa.properties.hibernate.cache.region.factory_class=jcache",
        "spring.jpa.properties.hibernate.jakarta.cache.provider=com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider",

        // JPA Batch Insert
        "spring.jpa.properties.hibernate.jdbc.batch_size=30",
        "spring.jpa.properties.hibernate.order_inserts=true",
        "spring.jpa.properties.hibernate.order_updates=true"
    ]
)
@Transactional
abstract class AbstractQuerydslTest {

    companion object: KLogging() {
        @JvmStatic
        val faker: Faker = Fakers.faker
    }

    @Autowired
    protected lateinit var tem: TestEntityManager

    protected val em: EntityManager get() = tem.entityManager
    protected val emf: EntityManagerFactory get() = em.entityManagerFactory

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
