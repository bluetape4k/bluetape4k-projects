package io.bluetape4k.hibernate.standalone

import io.bluetape4k.hibernate.countAll
import io.bluetape4k.hibernate.deleteAll
import io.bluetape4k.hibernate.save
import io.bluetape4k.hibernate.stateless.createEntityGraphAs
import io.bluetape4k.hibernate.stateless.createNativeQueryAs
import io.bluetape4k.hibernate.stateless.createQueryAs
import io.bluetape4k.hibernate.stateless.createSelectionQueryAs
import io.bluetape4k.hibernate.stateless.getAs
import io.bluetape4k.hibernate.stateless.withStateless
import io.bluetape4k.hibernate.stateless.withStatelss
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StatelessSessionStandaloneTest : AbstractStandaloneHibernateTest() {

    override fun entityClasses() = listOf(StandaloneEntity::class.java)

    @BeforeEach
    fun clearData() {
        inTransaction { deleteAll<StandaloneEntity>() }
    }

    @Test
    fun `SessionFactory_withStateless는 StatelessSession으로 insert를 수행한다`() {
        sessionFactory.withStateless { ss ->
            ss.insert(StandaloneEntity("stateless-1"))
            ss.insert(StandaloneEntity("stateless-2"))
        }

        inTransaction {
            countAll<StandaloneEntity>() shouldBeEqualTo 2L
        }
    }

    @Test
    fun `EntityManager_withStateless는 StatelessSession으로 insert를 수행한다`() {
        val entity = StandaloneEntity("em-stateless")
        inTransaction {
            withStateless { ss ->
                ss.insert(entity)
            }
        }

        inTransaction {
            countAll<StandaloneEntity>() shouldBeEqualTo 1L
        }
    }

    @Test
    fun `StatelessSession_getAs는 엔티티를 조회한다`() {
        val entity = StandaloneEntity("get-as-test")
        sessionFactory.withStateless { ss ->
            ss.insert(entity)
        }

        sessionFactory.withStateless { ss ->
            val loaded = ss.getAs<StandaloneEntity>(entity.id!!)
            loaded.shouldNotBeNull()
            loaded.name shouldBeEqualTo "get-as-test"
        }
    }

    @Test
    fun `StatelessSession_withStateless는 예외 발생 시 rollback한다`() {
        try {
            sessionFactory.withStateless { ss ->
                ss.insert(StandaloneEntity("rollback-test"))
                throw RuntimeException("test rollback")
            }
        } catch (_: RuntimeException) {}

        inTransaction {
            countAll<StandaloneEntity>() shouldBeEqualTo 0L
        }
    }

    @Test
    fun `StatelessSession_createQueryAs는 HQL 쿼리를 실행한다`() {
        sessionFactory.withStateless { ss ->
            ss.insert(StandaloneEntity("query-test"))
        }

        sessionFactory.withStateless { ss ->
            val count = ss.createQueryAs<Long>("SELECT COUNT(e) FROM StandaloneEntity e")
                .uniqueResult()
            count shouldBeEqualTo 1L
        }
    }

    @Test
    fun `StatelessSession_createSelectionQueryAs는 selection 쿼리를 실행한다`() {
        sessionFactory.withStateless { ss ->
            ss.insert(StandaloneEntity("selection-test"))
        }

        sessionFactory.withStateless { ss ->
            val results = ss.createSelectionQueryAs<StandaloneEntity>("FROM StandaloneEntity")
                .list()
            results.shouldNotBeNull()
        }
    }

    @Test
    fun `StatelessSession_createNativeQueryAs는 native 쿼리를 실행한다`() {
        sessionFactory.withStateless { ss ->
            ss.insert(StandaloneEntity("native-test"))
        }

        sessionFactory.withStateless { ss ->
            val results = ss.createNativeQueryAs<Any>("SELECT COUNT(*) FROM standalone_entity")
                .list()
            results.shouldNotBeNull()
        }
    }

    @Test
    fun `StatelessSession_getAs with LockMode는 엔티티를 조회한다`() {
        val entity = StandaloneEntity("lockmode-test")
        sessionFactory.withStateless { ss ->
            ss.insert(entity)
        }

        sessionFactory.withStateless { ss ->
            val loaded = ss.getAs<StandaloneEntity>(entity.id!!, org.hibernate.LockMode.NONE)
            loaded.shouldNotBeNull()
            loaded.name shouldBeEqualTo "lockmode-test"
        }
    }

    @Test
    fun `StatelessSession_createEntityGraphAs는 EntityGraph를 생성한다`() {
        sessionFactory.withStateless { ss ->
            val graph = ss.createEntityGraphAs<StandaloneEntity>()
            graph.shouldNotBeNull()
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun `SessionFactory_withStatelss는 deprecated 버전으로 insert를 수행한다`() {
        sessionFactory.withStatelss { ss ->
            ss.insert(StandaloneEntity("statelss-deprecated-test"))
        }

        inTransaction {
            countAll<StandaloneEntity>() shouldBeEqualTo 1L
        }
    }
}
