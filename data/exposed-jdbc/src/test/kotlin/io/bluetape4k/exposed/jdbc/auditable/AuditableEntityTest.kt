package io.bluetape4k.exposed.jdbc.auditable

import io.bluetape4k.exposed.core.auditable.AuditableLongIdTable
import io.bluetape4k.exposed.core.auditable.UserContext
import io.bluetape4k.exposed.dao.auditable.AuditableLongEntity
import io.bluetape4k.exposed.dao.auditable.AuditableLongEntityClass
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.dao.flushCache
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * [io.bluetape4k.exposed.dao.auditable.AuditableEntity] DAO 통합 테스트입니다.
 *
 * [AuditableLongEntity]를 상속한 Article 엔티티를 통해
 * 감사 컬럼(createdBy/createdAt/updatedBy/updatedAt)의 자동 설정 동작을 검증합니다.
 *
 * Java 21+ 환경의 [ScopedValue] API 사용으로 인해 Java 21 이상에서만 실행됩니다.
 */
@EnabledForJreRange(min = JRE.JAVA_21)
class AuditableEntityTest : AbstractExposedTest() {

    companion object : KLogging()

    // Articles 테이블 정의
    object Articles : AuditableLongIdTable("auditable_articles") {
        val title = varchar("title", 200)
    }

    // Article 엔티티 정의
    class Article(id: EntityID<Long>) : AuditableLongEntity(id) {
        companion object : AuditableLongEntityClass<Article>(Articles)

        var title by Articles.title
        override var createdBy by Articles.createdBy
        override var createdAt by Articles.createdAt
        override var updatedBy by Articles.updatedBy
        override var updatedAt by Articles.updatedAt
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `새 엔티티 생성 후 flush 시 createdBy 가 자동 설정된다`(testDB: TestDB) {
        withTables(testDB, Articles) {
            val article = Article.new {
                title = "첫 번째 아티클"
            }
            flushCache()
            entityCache.clear()

            val loaded = Article.findById(article.id)!!
            loaded.createdBy.shouldNotBeNull()
            loaded.createdAt.shouldNotBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `새 엔티티 생성 직후 updatedBy 와 updatedAt 은 null 이다`(testDB: TestDB) {
        withTables(testDB, Articles) {
            val article = Article.new {
                title = "두 번째 아티클"
            }
            flushCache()
            entityCache.clear()

            val loaded = Article.findById(article.id)!!
            loaded.updatedAt.shouldBeNull()
            loaded.updatedBy.shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `프로퍼티 수정 후 flush 시 updatedBy 가 자동 설정된다`(testDB: TestDB) {
        withTables(testDB, Articles) {
            val article = Article.new {
                title = "수정 전 아티클"
            }
            flushCache()
            entityCache.clear()

            val loaded = Article.findById(article.id)!!
            loaded.title = "수정 후 아티클"
            loaded.flush()

            entityCache.clear()

            val updated = Article.findById(article.id)!!
            updated.updatedBy.shouldNotBeNull()
            updated.title shouldBeEqualTo "수정 후 아티클"
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `UserContext withUser 내에서 생성 시 createdBy 가 해당 사용자명으로 설정된다`(testDB: TestDB) {
        withTables(testDB, Articles) {
            val authorName = "author"
            val articleId = UserContext.withUser(authorName) {
                val article = Article.new {
                    title = "작성자 지정 아티클"
                }
                flushCache()
                article.id
            }

            entityCache.clear()

            val loaded = Article.findById(articleId)!!
            loaded.createdBy shouldBeEqualTo authorName
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `동일 ID 를 가진 두 엔티티는 equals 가 true 이다`(testDB: TestDB) {
        withTables(testDB, Articles) {
            val article = Article.new {
                title = "equals 테스트"
            }
            flushCache()

            val reloaded = Article.findById(article.id)!!
            article shouldBeEqualTo reloaded
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `서로 다른 ID 를 가진 두 엔티티는 equals 가 false 이다`(testDB: TestDB) {
        withTables(testDB, Articles) {
            val article1 = Article.new { title = "아티클 1" }
            val article2 = Article.new { title = "아티클 2" }
            flushCache()

            article1 shouldNotBeEqualTo article2
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `동일 ID 를 가진 두 엔티티는 hashCode 가 동일하다`(testDB: TestDB) {
        withTables(testDB, Articles) {
            val article = Article.new {
                title = "hashCode 테스트"
            }
            flushCache()

            val reloaded = Article.findById(article.id)!!
            article.hashCode() shouldBeEqualTo reloaded.hashCode()
        }
    }
}
