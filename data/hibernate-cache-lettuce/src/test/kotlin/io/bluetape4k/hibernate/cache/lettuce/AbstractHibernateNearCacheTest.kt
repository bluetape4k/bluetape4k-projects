package io.bluetape4k.hibernate.cache.lettuce

import io.bluetape4k.hibernate.cache.lettuce.model.Article
import io.bluetape4k.hibernate.cache.lettuce.model.CompositePerson
import io.bluetape4k.hibernate.cache.lettuce.model.Department
import io.bluetape4k.hibernate.cache.lettuce.model.Employee
import io.bluetape4k.hibernate.cache.lettuce.model.NaturalUser
import io.bluetape4k.hibernate.cache.lettuce.model.Person
import io.bluetape4k.hibernate.cache.lettuce.model.Project
import io.bluetape4k.hibernate.cache.lettuce.model.VersionedCategory
import io.bluetape4k.hibernate.cache.lettuce.model.VersionedCategoryItem
import io.bluetape4k.hibernate.cache.lettuce.model.VersionedItem
import io.lettuce.core.KeyScanCursor
import io.lettuce.core.RedisClient
import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanCursor
import io.lettuce.core.codec.StringCodec
import org.hibernate.SessionFactory
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll

/**
 * Hibernate Near Cache 테스트 베이스.
 *
 * Testcontainers Redis 7+ + H2 in-memory + SessionFactory를 설정한다.
 */
abstract class AbstractHibernateNearCacheTest {
    companion object {
        lateinit var sessionFactory: SessionFactory
        lateinit var redisUri: String

        @JvmStatic
        @BeforeAll
        fun setupSessionFactory() {
            redisUri = "redis://${RedisServers.redis.host}:${RedisServers.redis.port}"

            val registry =
                StandardServiceRegistryBuilder()
                    .applySetting("hibernate.connection.driver_class", "org.h2.Driver")
                    .applySetting("hibernate.connection.url", "jdbc:h2:mem:nearCacheTest;DB_CLOSE_DELAY=-1")
                    .applySetting("hibernate.connection.username", "sa")
                    .applySetting("hibernate.connection.password", "")
                    .applySetting("hibernate.hbm2ddl.auto", "create-drop")
                    .applySetting("hibernate.cache.use_second_level_cache", "true")
                    .applySetting("hibernate.cache.use_query_cache", "true")
                    .applySetting("hibernate.generate_statistics", "true")
                    .applySetting(
                        "hibernate.cache.region.factory_class",
                        LettuceNearCacheRegionFactory::class.java.name
                    ).applySetting("hibernate.cache.lettuce.redis_uri", redisUri)
                    .applySetting("hibernate.cache.lettuce.use_resp3", "true")
                    .applySetting("hibernate.cache.lettuce.local.max_size", "1000")
                    .applySetting("hibernate.cache.lettuce.redis_ttl.default", "60s")
                    .build()

            sessionFactory =
                MetadataSources(registry)
                    .addAnnotatedClass(Person::class.java)
                    .addAnnotatedClass(CompositePerson::class.java)
                    .addAnnotatedClass(NaturalUser::class.java)
                    .addAnnotatedClass(Department::class.java)
                    .addAnnotatedClass(Employee::class.java)
                    .addAnnotatedClass(Project::class.java)
                    .addAnnotatedClass(VersionedItem::class.java)
                    .addAnnotatedClass(VersionedCategory::class.java)
                    .addAnnotatedClass(VersionedCategoryItem::class.java)
                    .addAnnotatedClass(Article::class.java)
                    .buildMetadata()
                    .buildSessionFactory()
        }

        @JvmStatic
        @AfterAll
        fun tearDownSessionFactory() {
            runCatching { sessionFactory.close() }
        }
    }

    protected fun redisKeys(pattern: String): Set<String> {
        val redisClient = RedisClient.create(redisUri)
        return redisClient.use { client ->
            client.connect(StringCodec.UTF8).use { connection ->
                val commands = connection.sync()
                val keys = linkedSetOf<String>()
                var cursor: ScanCursor = ScanCursor.INITIAL
                do {
                    val result: KeyScanCursor<String> =
                        commands.scan(cursor, ScanArgs.Builder.matches(pattern).limit(100))
                    keys += result.keys
                    cursor = result
                } while (!result.isFinished)
                keys
            }
        }
    }
}
