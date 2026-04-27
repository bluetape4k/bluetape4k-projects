package io.bluetape4k.hibernate.standalone

import io.bluetape4k.logging.KLogging
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.hibernate.SessionFactory
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

/**
 * Spring 없이 Hibernate EMF를 직접 생성하는 standalone 테스트 기반 클래스.
 * H2 인메모리 DB를 사용하며, 엔티티 클래스는 subclass에서 제공한다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractStandaloneHibernateTest {

    companion object : KLogging()

    abstract fun entityClasses(): List<Class<*>>

    protected lateinit var emf: EntityManagerFactory

    @BeforeAll
    fun setupEmf() {
        val registry = StandardServiceRegistryBuilder()
            .applySetting("hibernate.connection.driver_class", "org.h2.Driver")
            .applySetting(
                "hibernate.connection.url",
                "jdbc:h2:mem:standalone_test_${System.nanoTime()};DB_CLOSE_DELAY=-1"
            )
            .applySetting("hibernate.connection.username", "sa")
            .applySetting("hibernate.connection.password", "")
            .applySetting("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
            .applySetting("hibernate.hbm2ddl.auto", "create-drop")
            .applySetting("hibernate.show_sql", "false")
            .applySetting("hibernate.format_sql", "false")
            .applySetting("hibernate.connection.pool_size", "5")
            .build()

        val sources = MetadataSources(registry)
        entityClasses().forEach { sources.addAnnotatedClass(it) }

        emf = sources.buildMetadata().buildSessionFactory()
    }

    @AfterAll
    fun tearDownEmf() {
        if (::emf.isInitialized) {
            emf.close()
        }
    }

    protected fun <T> inTransaction(block: EntityManager.() -> T): T {
        val em = emf.createEntityManager()
        val tx = em.transaction
        return try {
            tx.begin()
            val result = em.block()
            tx.commit()
            result
        } catch (e: Exception) {
            if (tx.isActive) tx.rollback()
            throw e
        } finally {
            em.close()
        }
    }

    protected fun <T> readOnly(block: EntityManager.() -> T): T {
        val em = emf.createEntityManager()
        return try {
            em.block()
        } finally {
            em.close()
        }
    }

    protected val sessionFactory: SessionFactory
        get() = emf.unwrap(SessionFactory::class.java)
}
