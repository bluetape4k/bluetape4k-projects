package io.bluetape4k.hibernate.cache.lettuce

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.cache.nearcache.LettuceNearCache
import io.bluetape4k.cache.nearcache.LettuceNearCacheConfig
import io.bluetape4k.hibernate.cache.lettuce.model.CompositePerson
import io.bluetape4k.hibernate.cache.lettuce.model.CompositePersonId
import io.bluetape4k.hibernate.cache.lettuce.model.NaturalUser
import io.bluetape4k.hibernate.cache.lettuce.model.Person
import io.lettuce.core.RedisClient
import io.lettuce.core.codec.StringCodec
import org.hibernate.KeyType
import org.hibernate.cache.internal.BasicCacheKeyImplementation
import org.hibernate.cache.internal.NaturalIdCacheKey
import org.hibernate.cache.spi.RegionFactory
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.Serializable

class HibernateAdvancedKeyCacheTest: AbstractHibernateNearCacheTest() {

    @BeforeEach
    fun clearCacheAndData() {
        sessionFactory.cache.evictAllRegions()
        sessionFactory.statistics.clear()
        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            session.createMutationQuery("DELETE FROM NaturalUser").executeUpdate()
            session.createMutationQuery("DELETE FROM CompositePerson").executeUpdate()
            session.transaction.commit()
        }
    }

    @Test
    fun `composite id entity는 2nd level cache를 사용하고 Redis key는 digest 문자열이다`() {
        val id = CompositePersonId(companyCode = "ACME", employeeNo = 1001L)

        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            session.persist(
                CompositePerson().apply {
                    this.id = id
                    name = "Composite Alice"
                }
            )
            session.transaction.commit()
        }

        sessionFactory.statistics.clear()

        repeat(2) {
            sessionFactory.openSession().use { session ->
                session.beginTransaction()
                session.find(CompositePerson::class.java, id).shouldNotBeNull()
                session.transaction.commit()
            }
        }

        val regionFactory = (sessionFactory as SessionFactoryImplementor).serviceRegistry
            .getService(org.hibernate.cache.spi.RegionFactory::class.java) as LettuceNearCacheRegionFactory
        val regionName = regionFactory.getCaches().keys.first { it.contains("CompositePerson") }
        val redisKeys = redisKeys("$regionName:*")

        redisKeys.size shouldBeGreaterThan 0
        redisKeys.any { it.contains("hck2:") }.shouldBeTrue()
    }

    @Test
    fun `natural-id cache는 hit를 만들고 Redis key는 digest 문자열을 사용한다`() {
        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            session.persist(
                NaturalUser().apply {
                    email = "natural@example.com"
                    displayName = "Natural User"
                }
            )
            session.transaction.commit()
        }

        sessionFactory.statistics.clear()

        repeat(2) {
            sessionFactory.openSession().use { session ->
                session.beginTransaction()
                session.find(
                    NaturalUser::class.java,
                    "natural@example.com",
                    KeyType.NATURAL
                ).shouldNotBeNull()
                session.transaction.commit()
            }
        }

        sessionFactory.statistics.naturalIdCachePutCount shouldBeGreaterThan 0L
        sessionFactory.statistics.naturalIdCacheHitCount shouldBeGreaterThan 0L

        val regionFactory = (sessionFactory as SessionFactoryImplementor).serviceRegistry
            .getService(org.hibernate.cache.spi.RegionFactory::class.java) as LettuceNearCacheRegionFactory
        val regionName =
            regionFactory.getCaches().keys.first { it.contains("##NaturalId") || it.contains("NaturalUser") }
        val redisKeys = redisKeys("$regionName:*")

        redisKeys.any { key -> key.contains("hck2:") }.shouldBeTrue()
    }

    @Test
    fun `natural-id delimiter value와 composite arity는 같은 cache key로 충돌하지 않는다`() {
        withStorageAccess("natural-id-collision-${System.nanoTime()}") { cacheName, storageAccess, session ->
            val singleValueKey = NaturalIdCacheKey("alpha, beta", "NaturalEntity", null, 1)
            val compositeValueKey = NaturalIdCacheKey(arrayOf("alpha", "beta"), "NaturalEntity", null, 2)

            storageAccess.putIntoCache(singleValueKey, "single-value", session)
            storageAccess.putIntoCache(compositeValueKey, "composite-value", session)

            storageAccess.getFromCache(singleValueKey, session) shouldBeEqualTo "single-value"
            storageAccess.getFromCache(compositeValueKey, session) shouldBeEqualTo "composite-value"
            redisKeys("$cacheName:*").size shouldBeEqualTo 2
        }
    }

    @Test
    fun `scalar identifier와 object array identifier는 같은 cache key로 충돌하지 않는다`() {
        withStorageAccess("array-scalar-collision-${System.nanoTime()}") { cacheName, storageAccess, session ->
            val scalarKey = BasicCacheKeyImplementation("[1, 2]" as Serializable, "ArrayEntity", 1)
            val arrayKey = BasicCacheKeyImplementation(arrayOf(1, 2) as Serializable, "ArrayEntity", 2)

            storageAccess.putIntoCache(scalarKey, "scalar-id", session)
            storageAccess.putIntoCache(arrayKey, "array-id", session)

            storageAccess.getFromCache(scalarKey, session) shouldBeEqualTo "scalar-id"
            storageAccess.getFromCache(arrayKey, session) shouldBeEqualTo "array-id"
            redisKeys("$cacheName:*").size shouldBeEqualTo 2
        }
    }

    @Test
    fun `same toString custom identifiers는 같은 cache key로 충돌하지 않는다`() {
        withStorageAccess("custom-id-collision-${System.nanoTime()}") { cacheName, storageAccess, session ->
            val firstKey = BasicCacheKeyImplementation(OpaqueIdentifier("first"), "OpaqueEntity", 1)
            val secondKey = BasicCacheKeyImplementation(OpaqueIdentifier("second"), "OpaqueEntity", 2)

            storageAccess.putIntoCache(firstKey, "first-id", session)
            storageAccess.putIntoCache(secondKey, "second-id", session)

            storageAccess.getFromCache(firstKey, session) shouldBeEqualTo "first-id"
            storageAccess.getFromCache(secondKey, session) shouldBeEqualTo "second-id"
            redisKeys("$cacheName:*").size shouldBeEqualTo 2
        }
    }

    @Test
    fun `nested graph serialization failure는 cache key를 fail-closed 처리한다`() {
        withStorageAccess("broken-serializable-id-${System.nanoTime()}") { cacheName, storageAccess, session ->
            val firstKey = BrokenSerializableIdentifier("first")
            val secondKey = BrokenSerializableIdentifier("second")

            storageAccess.putIntoCache(firstKey, "first-value", session)
            storageAccess.putIntoCache(secondKey, "second-value", session)

            storageAccess.getFromCache(firstKey, session).shouldBeNull()
            storageAccess.getFromCache(secondKey, session).shouldBeNull()
            storageAccess.contains(firstKey) shouldBeEqualTo false
            redisKeys("$cacheName:*").size shouldBeEqualTo 0
        }
    }

    @Test
    fun `동일 textual representation의 비직렬화 식별자는 cache key를 fail-closed 처리한다`() {
        withStorageAccess("non-serializable-id-${System.nanoTime()}") { cacheName, storageAccess, session ->
            val firstKey = NonSerializableIdentifier("first")
            val secondKey = NonSerializableIdentifier("second")

            storageAccess.putIntoCache(firstKey, "first-value", session)
            storageAccess.putIntoCache(secondKey, "second-value", session)

            storageAccess.getFromCache(firstKey, session).shouldBeNull()
            storageAccess.getFromCache(secondKey, session).shouldBeNull()
            storageAccess.contains(firstKey) shouldBeEqualTo false
            redisKeys("$cacheName:*").size shouldBeEqualTo 0
        }
    }

    @Test
    fun `update timestamps cache도 digest 문자열 key를 사용한다`() {
        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            session.persist(
                Person().apply {
                    name = "Timestamp User"
                    age = 33
                }
            )
            session.transaction.commit()
        }

        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            session.createSelectionQuery(
                "select p from Person p where p.age >= :age",
                Person::class.java,
            )
                .setParameter("age", 30)
                .setCacheable(true)
                .list()
                .size shouldBeEqualTo 1
            session.transaction.commit()
        }

        sessionFactory.statistics.updateTimestampsCachePutCount shouldBeGreaterThan 0L

        val redisKeys = redisKeys("${RegionFactory.DEFAULT_UPDATE_TIMESTAMPS_REGION_UNQUALIFIED_NAME}:*")

        redisKeys.shouldNotBeNull()
        redisKeys.any { key -> key.contains("hck2:") }.shouldBeTrue()
    }

    private fun withStorageAccess(
        cacheName: String,
        block: (String, LettuceNearCacheStorageAccess, SharedSessionContractImplementor) -> Unit,
    ) {
        val redisClient = RedisClient.create(redisUri)
        val config = LettuceNearCacheConfig<String, String>(
            cacheName = cacheName,
            useRespProtocol3 = false,
        )

        @Suppress("UNCHECKED_CAST")
        val nearCache = LettuceNearCache(redisClient, StringCodec.UTF8, config) as LettuceNearCache<Any>

        redisClient.use {
            nearCache.use { cache ->
                try {
                    sessionFactory.openSession().use { session ->
                        block(
                            cacheName,
                            LettuceNearCacheStorageAccess(cacheName, cache),
                            session as SharedSessionContractImplementor,
                        )
                    }
                } finally {
                    cache.clearAll()
                }
            }
        }
    }

    private data class OpaqueIdentifier(
        private val value: String,
    ): Serializable {
        override fun toString(): String = "opaque"

        companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    private class BrokenSerializableIdentifier(
        private val value: String,
    ): Serializable {
        private val nestedState: Any = Any()

        override fun hashCode(): Int = 0

        override fun toString(): String = "same-text"

        companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    private class NonSerializableIdentifier(
        private val value: String,
    ) {
        override fun toString(): String = "same-text"
    }
}
