package io.bluetape4k.hibernate.cache.lettuce

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.hibernate.cache.lettuce.model.Article
import io.bluetape4k.hibernate.findAs
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * @ElementCollection 캐시 테스트.
 */
class HibernateElementCollectionCacheTest: AbstractHibernateNearCacheTest() {

    companion object: KLogging()

    @BeforeEach
    fun reset() {
        sessionFactory.cache.evictAllRegions()
        sessionFactory.statistics.clear()
        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            s.createMutationQuery("DELETE FROM Article").executeUpdate()
            s.transaction.commit()
        }
    }

    @Test
    fun `Article tags ElementCollection이 2nd level cache에 적재된다`() {
        val articleId = sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val article =
                Article().apply {
                    title = "Caching Guide"
                    tags.addAll(listOf("kotlin", "hibernate", "redis"))
                }
            s.persist(article)
            s.transaction.commit()
            article.id.shouldNotBeNull()
        }
        sessionFactory.statistics.clear()

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val a = s.findAs<Article>(articleId).shouldNotBeNull()
            a.tags.size shouldBeEqualTo 3
            s.transaction.commit()
        }

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val a = s.findAs<Article>(articleId).shouldNotBeNull()
            a.tags shouldContain "kotlin"
            s.transaction.commit()
        }

        sessionFactory.statistics.secondLevelCacheHitCount shouldBeGreaterThan 0L
    }

    @Test
    fun `Article ratings ElementCollection 수정 후 캐시가 갱신된다`() {
        val articleId = sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val article =
                Article().apply {
                    title = "Ratings Test"
                    ratings.addAll(listOf(3, 4, 5))
                }
            s.persist(article)
            s.transaction.commit()
            article.id.shouldNotBeNull()
        }

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val a = s.findAs<Article>(articleId).shouldNotBeNull()
            a.ratings.size shouldBeEqualTo 3
            s.transaction.commit()
        }

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val a = s.findAs<Article>(articleId).shouldNotBeNull()
            a.ratings.clear()
            a.ratings.addAll(listOf(1, 2))
            s.transaction.commit()
        }
        sessionFactory.statistics.clear()

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val a = s.findAs<Article>(articleId).shouldNotBeNull()
            a.ratings.size shouldBeEqualTo 2
            s.transaction.commit()
        }
    }

    @Test
    fun `Article tags region evict 후 DB에서 재로드`() {
        val articleId = sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val article = Article().apply {
                title = "Evict Test"
                tags.addAll(listOf("cache", "evict"))
            }
            s.persist(article)
            s.transaction.commit()
            article.id.shouldNotBeNull()
        }

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val a = s.findAs<Article>(articleId).shouldNotBeNull()
            a.tags.size
            s.transaction.commit()
        }

        sessionFactory.cache.evictCollectionData("${Article::class.java.name}.tags", articleId)
        sessionFactory.statistics.clear()

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val a = s.findAs<Article>(articleId).shouldNotBeNull()
            a.tags.size shouldBeEqualTo 2
            s.transaction.commit()
        }
    }

    @Test
    fun `Article 삭제 시 ElemenCollection도 캐시에서 제거된다`() {
        val articleId = sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val article =
                Article().apply {
                    title = "Delete Test"
                    tags.add("delete-me")
                }
            s.persist(article)
            s.transaction.commit()
            article.id.shouldNotBeNull()
        }

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val a = s.findAs<Article>(articleId).shouldNotBeNull()
            a.tags.size
            s.transaction.commit()
        }

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val a = s.findAs<Article>(articleId).shouldNotBeNull()
            s.remove(a)
            s.transaction.commit()
        }

        sessionFactory.cache.containsEntity(Article::class.java, articleId).shouldBeFalse()
    }
}
