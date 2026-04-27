package io.bluetape4k.hibernate.standalone

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
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SessionSupportStandaloneTest : AbstractStandaloneHibernateTest() {

    override fun entityClasses() = listOf(StandaloneEntity::class.java)

    @BeforeEach
    fun clearData() {
        inTransaction { deleteAll<StandaloneEntity>() }
    }

    @Test
    fun `currentSession은 Hibernate Session을 반환한다`() {
        inTransaction {
            val session = currentSession()
            session.shouldNotBeNull()
        }
    }

    @Test
    fun `asSession은 currentSession과 동일한 Session을 반환한다`() {
        inTransaction {
            val s1 = currentSession()
            val s2 = asSession()
            s1.shouldNotBeNull()
            s2.shouldNotBeNull()
        }
    }

    @Test
    fun `Session_findAs는 id로 엔티티를 조회한다`() {
        val entity = StandaloneEntity("session-find-test")
        inTransaction { save(entity) }

        inTransaction {
            val session = currentSession()
            val loaded = session.findAs<StandaloneEntity>(entity.id!!)
            loaded.shouldNotBeNull()
            loaded.name shouldBeEqualTo "session-find-test"
        }
    }

    @Test
    fun `Session_findAs는 없는 id에 대해 null을 반환한다`() {
        inTransaction {
            val session = currentSession()
            val result = session.findAs<StandaloneEntity>(Long.MAX_VALUE)
            result.shouldBeNull()
        }
    }

    @Test
    fun `Session_createQueryAs는 Query를 반환한다`() {
        inTransaction { save(StandaloneEntity("q1")) }

        inTransaction {
            val session = currentSession()
            val result = session.createQueryAs<Long>(
                "select count(e) from StandaloneEntity e"
            ).uniqueResult()
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
        inTransaction { save(entity) }

        inTransaction {
            val session = currentSession()
            val ref = session.getReferenceAs<StandaloneEntity>(entity.id!!)
            ref.shouldNotBeNull()
        }
    }

    @Test
    fun `Session_createQueryAs with KClass는 Query를 반환한다`() {
        inTransaction { save(StandaloneEntity("kclass-query-test")) }

        inTransaction {
            val session = currentSession()
            val result = session.createQueryAs(
                "SELECT COUNT(e) FROM StandaloneEntity e",
                Long::class
            ).uniqueResult()
            result shouldBeEqualTo 1L
        }
    }

    @Test
    fun `Session_createNativeQueryAs는 Native Query를 반환한다`() {
        inTransaction { save(StandaloneEntity("native-test")) }

        inTransaction {
            val session = currentSession()
            val result = session.createNativeQueryAs<Any>(
                "SELECT COUNT(*) FROM standalone_entity"
            ).uniqueResult()
            result.shouldNotBeNull()
        }
    }

    @Test
    fun `Session_createNativeQueryAs with KClass는 Native Query를 반환한다`() {
        inTransaction { save(StandaloneEntity("native-kclass-test")) }

        inTransaction {
            val session = currentSession()
            val result = session.createNativeQueryAs(
                "SELECT COUNT(*) FROM standalone_entity",
                Any::class
            ).uniqueResult()
            result.shouldNotBeNull()
        }
    }
}
