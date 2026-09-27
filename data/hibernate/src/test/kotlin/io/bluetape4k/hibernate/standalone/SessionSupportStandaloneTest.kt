package io.bluetape4k.hibernate.standalone

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.hibernate.asSession
import io.bluetape4k.hibernate.countAll
import io.bluetape4k.hibernate.createNativeQueryAs
import io.bluetape4k.hibernate.createQueryAs
import io.bluetape4k.hibernate.currentSession
import io.bluetape4k.hibernate.deleteAll
import io.bluetape4k.hibernate.findAs
import io.bluetape4k.hibernate.getReferenceAs
import io.bluetape4k.hibernate.save
import io.bluetape4k.hibernate.withBatchSize
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SessionSupportStandaloneTest: AbstractStandaloneHibernateTest() {

    companion object: KLogging()

    override fun entityClasses() = listOf(StandaloneEntity::class.java)

    @BeforeEach
    fun clearData() {
        inTransaction {
            deleteAll<StandaloneEntity>()
        }
    }

    @Test
    fun `currentSession은 Hibernate Session을 반환한다`() {
        inTransaction {
            currentSession().shouldNotBeNull()
        }
    }

    @Test
    fun `asSession은 currentSession과 동일한 Session을 반환한다`() {
        inTransaction {
            val s1 = currentSession().shouldNotBeNull()
            val s2 = asSession().shouldNotBeNull()
            s1 shouldBeEqualTo s2
        }
    }

    @Test
    fun `Session_findAs는 id로 엔티티를 조회한다`() {
        val entity = StandaloneEntity("session-find-test")
        inTransaction { save(entity) }

        inTransaction {
            val session = currentSession()
            val loaded = session.findAs<StandaloneEntity>(entity.id!!).shouldNotBeNull()
            loaded.name shouldBeEqualTo "session-find-test"
        }
    }

    @Test
    fun `Session_findAs는 없는 id에 대해 null을 반환한다`() {
        inTransaction {
            val session = currentSession()
            session.findAs<StandaloneEntity>(Long.MAX_VALUE).shouldBeNull()
        }
    }

    @Test
    fun `Session_createQueryAs는 Query를 반환한다`() {
        inTransaction {
            save(StandaloneEntity("q1"))
        }

        inTransaction {
            val session = currentSession()
            val result = session.createQueryAs<Long>("select count(e) from StandaloneEntity e").uniqueResult()
            result shouldBeEqualTo 1L
        }
    }

    @Test
    fun `Session_withBatchSize는 배치 크기를 설정하고 실행한다`() {
        inTransaction {
            val session = currentSession()
            var executed = false
            session.withBatchSize(10) {
                executed = true
                save(StandaloneEntity("batch-1"))
                save(StandaloneEntity("batch-2"))
            }
            executed.shouldBeTrue()
        }

        inTransaction {
            countAll<StandaloneEntity>() shouldBeEqualTo 2L
        }
    }

    @Test
    fun `Session_getReferenceAs는 엔티티 프록시를 반환한다`() {
        val entity = StandaloneEntity("ref-test")
        inTransaction {
            save(entity)
        }

        inTransaction {
            val session = currentSession()
            val ref = session.getReferenceAs<StandaloneEntity>(entity.id!!).shouldNotBeNull()
            ref shouldBeEqualTo entity
        }
    }

    @Test
    fun `Session_createQueryAs with KClass는 Query를 반환한다`() {
        inTransaction {
            save(StandaloneEntity("kclass-query-test"))
        }

        inTransaction {
            val session = currentSession()
            val result = session.createQueryAs<Long>("SELECT COUNT(e) FROM StandaloneEntity e").uniqueResult()
            result shouldBeGreaterThan 0L
        }
    }

    @Test
    fun `Session_createNativeQueryAs는 Native Query를 반환한다`() {
        inTransaction {
            save(StandaloneEntity("native-test"))
        }

        inTransaction {
            val session = currentSession()
            val result = session.createNativeQueryAs<Long>("SELECT COUNT(*) FROM standalone_entity").uniqueResult()
            result shouldBeGreaterThan 0L
        }
    }

    @Test
    fun `Session_createNativeQueryAs with KClass는 Native Query를 반환한다`() {
        inTransaction {
            save(StandaloneEntity("native-kclass-test"))
        }

        inTransaction {
            val session = currentSession()
            val result = session.createNativeQueryAs<Long>("SELECT COUNT(*) FROM standalone_entity").uniqueResult()
            result shouldBeGreaterThan 0L
        }
    }
}
