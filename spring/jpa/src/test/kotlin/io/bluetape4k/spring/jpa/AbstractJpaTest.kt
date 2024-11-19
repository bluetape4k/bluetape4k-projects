package io.bluetape4k.spring.jpa

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import net.datafaker.Faker
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager

/**
 * `@DataJpaTest`를 사용하려면 SpringBootApplication 이 정의되어 있어야 합니다 (see [SpringDataJpaTestApplication])
 */
// NOTE: @DataJpaTest에서는 무조건 H2 를 사용하므로, SpringBootTest를 이용할 때에는 MySQL 모드로 사용하도록 해야 한다.
@DataJpaTest(
    properties = [
        // @DataJpaTest는 H2를 사용합니다. jdbc url을 mysql 로 지정하면 H2 DB를 MySQL 모드로 실행됩니다.
        "spring.datasource.url=jdbc:mysql://localhost:3306/test;",
        //                 "spring.datasource.driver-class-name = com.mysql.cj.jdbc.Driver",
        //                 "spring.datasource.username = test",
        //                 "spring.datasource.password = test",
        //                 "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL57Dialect"

        // DML 작업
        "spring.jpa.properties.hibernate.hbm2ddl.auto=update",

        "spring.jpa.properties.hibernate.show_sql=false",
        "spring.jpa.properties.hibernate.format_sql=true",
        "spring.jpa.properties.hibernate.highlight_sql=true",

        // 성능 측정 정보 제공
        "spring.jpa.properties.hibernate.generate_statistics=true",
        //"spring.jpa.properties.hibernate.use_sql_comments=true",
        //
        // NOTE: literal 을 parameter 로 binding 시킵니다
        // 참고 : https://vladmihalcea.com/how-does-hibernate-handle-jpa-criteria-api-literals/
        "spring.jpa.properties.hibernate.criteria.literal_handling_mode=bind",

        // NOTE: slow query 지정
        // logging 설정에 다음과 같이 추가해야 함
        // <logger name="org.hibernate.SQL_SLOW" level="INFO"/>
        "spring.jpa.properties.hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS=10",

        // Seond Cache
        "spring.jpa.properties.hibernate.cache.use_secoond_level_cache=false",
        // Query Cache
        "spring.jpa.properties.hibernate.cache.use_query_cache=false",

        // JPA Batch Insert (https://cheese10yun.github.io/jpa-batch-insert/)
        // MySQL인 경우 jdbc url에 `rewriteBatchedStatements=true` 추가해야 함
        "spring.jpa.properties.hibernate.jdbc.batch_size=10",
        "spring.jpa.properties.hibernate.order_inserts=true",
        "spring.jpa.properties.hibernate.order_updates=true"
    ],
    showSql = true,
    excludeAutoConfiguration = [FlywayAutoConfiguration::class]
)
abstract class AbstractJpaTest {

    companion object: KLogging() {
        // @DataJpaTest 는 H2 를 사용합니다. @SprintBootTest 로 직접적으로 사용하려면 MySQLServer를 생성해야 합니다.
        //        val mysql: MySQLServer by lazy {
        //            MySQLServer(useDefaultPort = true).apply {
        //                start()
        //                ShutdownQueue.register(this)
        //            }
        //        }

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
