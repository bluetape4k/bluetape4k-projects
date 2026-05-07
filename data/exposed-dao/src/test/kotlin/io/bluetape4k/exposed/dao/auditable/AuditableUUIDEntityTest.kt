package io.bluetape4k.exposed.dao.auditable

import io.bluetape4k.exposed.core.auditable.AuditableUUIDTable
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
import java.util.UUID

/**
 * [AuditableUUIDEntity] DAO 통합 테스트입니다.
 *
 * UUID PK를 사용하는 [AuditableUUIDEntity]의 감사 컬럼 자동 설정 동작을 검증합니다.
 */
@EnabledForJreRange(min = JRE.JAVA_21)
class AuditableUUIDEntityTest: AbstractExposedTest() {

    companion object: KLogging()

    object Documents: AuditableUUIDTable("auditable_documents") {
        val title = varchar("title", 255)
    }

    class Document(id: EntityID<UUID>): AuditableUUIDEntity(id) {
        companion object: AuditableUUIDEntityClass<Document>(Documents)

        var title by Documents.title
        override var createdBy by Documents.createdBy
        override var createdAt by Documents.createdAt
        override var updatedBy by Documents.updatedBy
        override var updatedAt by Documents.updatedAt
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `새 UUID PK 엔티티 생성 후 flush 시 createdBy 가 자동 설정된다`(testDB: TestDB) {
        withTables(testDB, Documents) {
            val doc = Document.new {
                title = "첫 번째 문서"
            }
            flushCache()
            entityCache.clear()

            val loaded = Document.findById(doc.id)!!
            loaded.createdBy.shouldNotBeNull()
            loaded.createdAt.shouldNotBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `새 UUID PK 엔티티 생성 직후 updatedBy 와 updatedAt 은 null 이다`(testDB: TestDB) {
        withTables(testDB, Documents) {
            val doc = Document.new {
                title = "두 번째 문서"
            }
            flushCache()
            entityCache.clear()

            val loaded = Document.findById(doc.id)!!
            loaded.updatedAt.shouldBeNull()
            loaded.updatedBy.shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `UserContext withUser 내에서 UUID PK 엔티티 생성 시 createdBy 가 해당 사용자명으로 설정된다`(testDB: TestDB) {
        withTables(testDB, Documents) {
            val authorName = "uuid-author"
            val docId = UserContext.withUser(authorName) {
                val doc = Document.new {
                    title = "작성자 지정 문서"
                }
                flushCache()
                doc.id
            }

            entityCache.clear()

            val loaded = Document.findById(docId)!!
            loaded.createdBy shouldBeEqualTo authorName
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `프로퍼티 수정 후 flush 시 updatedBy 가 자동 설정된다`(testDB: TestDB) {
        withTables(testDB, Documents) {
            val doc = Document.new {
                title = "수정 전 문서"
            }
            flushCache()
            entityCache.clear()

            val loaded = Document.findById(doc.id)!!
            loaded.title = "수정 후 문서"
            loaded.flush()

            entityCache.clear()

            val updated = Document.findById(doc.id)!!
            updated.updatedBy.shouldNotBeNull()
            updated.title shouldBeEqualTo "수정 후 문서"
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `UserContext 없이 생성 시 createdBy 는 DEFAULT_USERNAME 으로 설정된다`(testDB: TestDB) {
        // UserContext 가 설정되지 않은 상태에서 생성하면 "system" 이 fallback 으로 사용된다
        withTables(testDB, Documents) {
            val doc = Document.new {
                title = "시스템 문서"
            }
            flushCache()
            entityCache.clear()

            val loaded = Document.findById(doc.id)!!
            loaded.createdBy shouldBeEqualTo UserContext.DEFAULT_USERNAME
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `UUID PK 는 자동 생성되며 non-null 이다`(testDB: TestDB) {
        // UUID PK 는 client-side 에서 UUID.randomUUID() 로 생성되므로 항상 non-null
        withTables(testDB, Documents) {
            val doc = Document.new {
                title = "UUID 확인 문서"
            }
            flushCache()

            doc.id.value.shouldNotBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `동일 ID 를 가진 두 UUID PK 엔티티는 equals 가 true 이다`(testDB: TestDB) {
        withTables(testDB, Documents) {
            val doc = Document.new {
                title = "equals 테스트"
            }
            flushCache()

            val reloaded = Document.findById(doc.id)!!
            doc shouldBeEqualTo reloaded
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `서로 다른 ID 를 가진 두 UUID PK 엔티티는 equals 가 false 이다`(testDB: TestDB) {
        withTables(testDB, Documents) {
            val doc1 = Document.new { title = "문서 1" }
            val doc2 = Document.new { title = "문서 2" }
            flushCache()

            doc1 shouldNotBeEqualTo doc2
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `변경 없이 flush 호출 시 updatedBy 는 설정되지 않는다`(testDB: TestDB) {
        // writeValues 가 비어있는 상태에서 flush() 를 호출해도 updatedBy 가 설정되어서는 안 된다
        withTables(testDB, Documents) {
            val doc = Document.new {
                title = "수정 없는 문서"
            }
            flushCache()
            entityCache.clear()

            val loaded = Document.findById(doc.id)!!
            loaded.flush()
            entityCache.clear()

            val reloaded = Document.findById(doc.id)!!
            reloaded.updatedBy.shouldBeNull()
            reloaded.updatedAt.shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `UserContext withThreadLocalUser 내에서 UUID PK 엔티티 수정 시 updatedBy 가 설정된다`(testDB: TestDB) {
        withTables(testDB, Documents) {
            val doc = Document.new {
                title = "thread local 문서"
            }
            flushCache()
            entityCache.clear()

            val updatedId = UserContext.withThreadLocalUser("uuid-editor") {
                val loaded = Document.findById(doc.id)!!
                loaded.title = "edited by thread local"
                loaded.flush()
                loaded.id
            }

            entityCache.clear()

            val loaded = Document.findById(updatedId)!!
            loaded.updatedBy shouldBeEqualTo "uuid-editor"
            loaded.title shouldBeEqualTo "edited by thread local"
        }
    }
}
