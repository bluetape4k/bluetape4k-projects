package io.bluetape4k.hibernate.spring.stateless

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.hibernate.spring.AbstractJpaTest
import io.bluetape4k.hibernate.stateless.withStateless
import io.bluetape4k.idgenerators.uuid.Uuid
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.asInt
import io.bluetape4k.support.asString
import org.hibernate.SessionFactory
import org.hibernate.StatelessSession
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.transaction.AfterTransaction
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import kotlin.system.measureTimeMillis

/**
 * 대량의 데이터 삽입 시에는 Stateless 가 Stateful 보다 최소 3배 정도 빠르다
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@Import(StatelessSessionTestConfiguration::class)
class StatelessSessionTest: AbstractJpaTest() {
    companion object: KLogging() {
        private const val COUNT = 10 // 1_000
        private const val DETAIL_COUNT = 10 // 1_000
        private const val REPEAT_COUNT = 3

        private val faker = Fakers.faker

        fun getStatelessEntity(index: Int): StatelessEntity =
            StatelessEntity(Uuid.V7.nextIdAsString() + "-" + index).apply {
                firstname = faker.name().firstName()
                lastname = faker.name().lastName()
                age = faker.number().numberBetween(10, 99)
                street = faker.address().streetAddress()
                city = faker.address().city()
                zipcode = faker.address().zipCode()
            }
    }

    @Autowired
    private lateinit var statelessSession: StatelessSession

    @Autowired
    private lateinit var sessionFactory: SessionFactory

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @AfterTransaction
    fun assertNoStatelessSessionResourceLeak() {
        currentStatelessResources().shouldHaveSize(0)
    }

    @Order(0)
    @Test
    fun `warm up`() {
        // Use Stateless Session (단 JPA EntityListener가 작동하지 않습니다)
        tem.entityManager.withStateless { stateless ->
            repeat(REPEAT_COUNT) {
                stateless.insert(getStatelessEntity(it))
            }
        }

        // Use Stateful Session
        repeat(REPEAT_COUNT) {
            tem.persist(getStatelessEntity(it))
        }
        flushAndClear()
    }

    @Test
    fun `injected StatelessSession proxy reuses dedicated transaction resource`() {
        statelessSession.isOpen.shouldBeTrue()
        val first = currentStatelessResource()
        (TransactionSynchronizationManager.getResource(sessionFactory) is StatelessSession).shouldBeFalse()

        statelessSession.isOpen.shouldBeTrue()
        val second = currentStatelessResource()

        second shouldBeSameInstanceAs first
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `injected StatelessSession proxy unbinds and closes after transaction completion`() {
        var opened: StatelessSession? = null

        TransactionTemplate(transactionManager).executeWithoutResult {
            statelessSession.isOpen.shouldBeTrue()
            opened = currentStatelessResource()
            opened.shouldNotBeNull().isOpen.shouldBeTrue()
        }

        currentStatelessResources().shouldHaveSize(0)
        opened.shouldNotBeNull().isOpen.shouldBeFalse()
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `injected StatelessSession proxy rejects calls outside transaction`() {
        assertFailsWith<IllegalStateException> {
            statelessSession.isOpen
        }
    }

    @Nested
    inner class WithSession: AbstractJpaTest() {
        @RepeatedTest(REPEAT_COUNT)
        fun `simple entity with session`() {
            val elapsed =
                measureTimeMillis {
                    repeat(COUNT) {
                        val entity = getStatelessEntity(it)
                        log.debug { "Persist entity: $entity" }
                        tem.persist(entity)
                    }
                    flush()
                }
            log.debug { "Session save: $elapsed  msec" }
        }

        @RepeatedTest(REPEAT_COUNT)
        fun `one-to-many entity with session`() {
            val elapsed =
                measureTimeMillis {
                    repeat(COUNT) {
                        val master = createMaster("stateful master-$it")
                        tem.persist(master)
                        log.debug { "Persist master: $master" }
                    }
                    tem.flush()
                }
            log.debug { "Session save: $elapsed msec" }
        }
    }

    @Nested
    inner class WithStateless: AbstractJpaTest() {
        @RepeatedTest(REPEAT_COUNT)
        fun `simple entity with stateless`() {
            val elapsed =
                measureTimeMillis {
                    tem.entityManager.withStateless { stateless ->
                        repeat(COUNT) {
                            val entity = getStatelessEntity(it)
                            log.debug { "Persist entity: $entity" }
                            stateless.insert(entity)
                        }
                    }
                }
            log.debug { "Stateless save: $elapsed  msec" }
        }

        @RepeatedTest(REPEAT_COUNT)
        fun `one-to-many entity with stateless`() {
            val elapsed =
                measureTimeMillis {
                    tem.entityManager.withStateless { stateless ->
                        repeat(COUNT) {
                            val master = createMaster("stateless master-$it")
                            stateless.insert(master)
                            log.debug { "Persist master: $master" }
                            master.details.forEach { detail ->
                                stateless.insert(detail)
                            }
                        }
                    }
                }
            log.debug { "Stateless save: $elapsed msec" }
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `load one-to-many with stateless`() {
        tem.entityManager.withStateless { stateless ->
            repeat(COUNT) {
                val master = createMaster("master-$it")
                stateless.insert(master)
                master.details.forEach { detail ->
                    stateless.insert(detail)
                }
            }
        }

        val rows: List<Any> =
            tem.entityManager.withStateless { stateless ->
                stateless.createNativeQuery("select * from spring_stateless_master m", Any::class.java).list()
            } ?: emptyList()

        rows.shouldNotBeEmpty()

        rows.forEach { row ->
            val row = row as Array<Any?>
            val id = row[0].asInt()
            val name = row[1].asString()
            val master = StatelessMaster(name).also { it.id = id }
            log.debug { "master=$master" }
        }
    }

    private fun createMaster(
        name: String,
        detailCount: Int = DETAIL_COUNT,
    ): StatelessMaster {
        val master = StatelessMaster(name)
        repeat(detailCount) { index ->
            val detail = StatelessDetail("details-$index").also { it.master = master }
            master.details.add(detail)
            detail.master = master
        }
        return master
    }

    private fun currentStatelessResource(): StatelessSession {
        val resources = currentStatelessResources()
        resources.shouldHaveSize(1)
        return resources.single()
    }

    private fun currentStatelessResources(): List<StatelessSession> {
        return TransactionSynchronizationManager.getResourceMap()
            .values
            .filterIsInstance<StatelessSession>()
    }
}
