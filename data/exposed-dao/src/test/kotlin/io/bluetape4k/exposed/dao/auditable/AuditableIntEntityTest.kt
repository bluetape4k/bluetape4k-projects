package io.bluetape4k.exposed.dao.auditable

import io.bluetape4k.exposed.core.auditable.AuditableIntIdTable
import io.bluetape4k.exposed.core.auditable.UserContext
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.dao.flushCache
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * [AuditableIntEntity] DAO 통합 테스트입니다.
 *
 * Int PK를 사용하는 [AuditableIntEntity]의 감사 컬럼 자동 설정 동작을 검증합니다.
 */
@EnabledForJreRange(min = JRE.JAVA_21)
class AuditableIntEntityTest: AbstractExposedTest() {

    companion object: KLogging()

    object Comments: AuditableIntIdTable("auditable_comments") {
        val content = varchar("content", 500)
    }

    class Comment(id: EntityID<Int>): AuditableIntEntity(id) {
        companion object: AuditableIntEntityClass<Comment>(Comments)

        var content by Comments.content
        override var createdBy by Comments.createdBy
        override var createdAt by Comments.createdAt
        override var updatedBy by Comments.updatedBy
        override var updatedAt by Comments.updatedAt
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `새 Int PK 엔티티 생성 후 flush 시 createdBy 가 자동 설정된다`(testDB: TestDB) {
        withTables(testDB, Comments) {
            val comment = Comment.new {
                content = "첫 번째 댓글"
            }
            flushCache()
            entityCache.clear()

            val loaded = Comment.findById(comment.id)!!
            loaded.createdBy.shouldNotBeNull()
            loaded.createdAt.shouldNotBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `새 Int PK 엔티티 생성 직후 updatedBy 와 updatedAt 은 null 이다`(testDB: TestDB) {
        withTables(testDB, Comments) {
            val comment = Comment.new {
                content = "두 번째 댓글"
            }
            flushCache()
            entityCache.clear()

            val loaded = Comment.findById(comment.id)!!
            loaded.updatedAt.shouldBeNull()
            loaded.updatedBy.shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `UserContext withUser 내에서 Int PK 엔티티 생성 시 createdBy 가 해당 사용자명으로 설정된다`(testDB: TestDB) {
        withTables(testDB, Comments) {
            val authorName = "int-author"
            val commentId = UserContext.withUser(authorName) {
                val comment = Comment.new {
                    content = "작성자 지정 댓글"
                }
                flushCache()
                comment.id
            }

            entityCache.clear()

            val loaded = Comment.findById(commentId)!!
            loaded.createdBy shouldBeEqualTo authorName
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `프로퍼티 수정 후 flush 시 updatedBy 가 자동 설정된다`(testDB: TestDB) {
        withTables(testDB, Comments) {
            val comment = Comment.new {
                content = "수정 전 댓글"
            }
            flushCache()
            entityCache.clear()

            val loaded = Comment.findById(comment.id)!!
            loaded.content = "수정 후 댓글"
            loaded.flush()

            entityCache.clear()

            val updated = Comment.findById(comment.id)!!
            updated.updatedBy.shouldNotBeNull()
            updated.content shouldBeEqualTo "수정 후 댓글"
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `UserContext 없이 생성 시 createdBy 는 DEFAULT_USERNAME 으로 설정된다`(testDB: TestDB) {
        // UserContext 가 설정되지 않은 상태에서 생성하면 "system" 이 fallback 으로 사용된다
        withTables(testDB, Comments) {
            val comment = Comment.new {
                content = "시스템 댓글"
            }
            flushCache()
            entityCache.clear()

            val loaded = Comment.findById(comment.id)!!
            loaded.createdBy shouldBeEqualTo UserContext.DEFAULT_USERNAME
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `동일 ID 를 가진 두 Int PK 엔티티는 equals 가 true 이다`(testDB: TestDB) {
        withTables(testDB, Comments) {
            val comment = Comment.new {
                content = "equals 테스트"
            }
            flushCache()

            val reloaded = Comment.findById(comment.id)!!
            comment shouldBeEqualTo reloaded
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `서로 다른 ID 를 가진 두 Int PK 엔티티는 equals 가 false 이다`(testDB: TestDB) {
        withTables(testDB, Comments) {
            val comment1 = Comment.new { content = "댓글 1" }
            val comment2 = Comment.new { content = "댓글 2" }
            flushCache()

            comment1 shouldNotBeEqualTo comment2
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `변경 없이 flush 호출 시 updatedBy 는 설정되지 않는다`(testDB: TestDB) {
        // writeValues 가 비어있는 상태에서 flush() 를 호출해도 updatedBy 가 설정되어서는 안 된다
        withTables(testDB, Comments) {
            val comment = Comment.new {
                content = "수정 없는 댓글"
            }
            flushCache()
            entityCache.clear()

            val loaded = Comment.findById(comment.id)!!
            loaded.flush()
            entityCache.clear()

            val reloaded = Comment.findById(comment.id)!!
            reloaded.updatedBy.shouldBeNull()
            reloaded.updatedAt.shouldBeNull()
        }
    }
}
