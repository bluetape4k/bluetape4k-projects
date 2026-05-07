package io.bluetape4k.exposed.core.auditable

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * [AuditableIntIdTable], [AuditableLongIdTable], [AuditableUUIDTable] 단위 테스트.
 *
 * DB 없이도 테이블 컬럼 구조를 검증하고, H2 in-memory DB로 실제 INSERT 동작을 확인한다.
 */
class AuditableTableTest: AbstractExposedTest() {

    companion object: KLogging()

    // AuditableIntIdTable 구체 구현체
    object IntAuditTable: AuditableIntIdTable("int_audit_test") {
        val name = varchar("name", 128)
    }

    // AuditableLongIdTable 구체 구현체
    object LongAuditTable: AuditableLongIdTable("long_audit_test") {
        val title = varchar("title", 128)
    }

    // AuditableUUIDTable 구체 구현체
    object UUIDAuditTable: AuditableUUIDTable("uuid_audit_test") {
        val label = varchar("label", 128)
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `AuditableIntIdTable - INSERT 시 createdBy가 기본값으로 설정된다`(testDB: TestDB) {
        withTables(testDB, IntAuditTable) {
            IntAuditTable.insert {
                it[name] = "test-int"
            }

            val row = IntAuditTable.selectAll().single()
            row[IntAuditTable.name] shouldBeEqualTo "test-int"
            row[IntAuditTable.createdBy] shouldBeEqualTo UserContext.DEFAULT_USERNAME
            row[IntAuditTable.createdAt].shouldNotBeNull()
            row[IntAuditTable.updatedBy] shouldBeEqualTo null
            row[IntAuditTable.updatedAt] shouldBeEqualTo null
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `AuditableIntIdTable - withUser 블록 내 INSERT 시 createdBy에 사용자명이 설정된다`(testDB: TestDB) {
        withTables(testDB, IntAuditTable) {
            UserContext.withUser("alice") {
                IntAuditTable.insert {
                    it[name] = "by-alice"
                }
            }

            val row = IntAuditTable.selectAll().single()
            row[IntAuditTable.createdBy] shouldBeEqualTo "alice"
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `AuditableLongIdTable - INSERT 시 createdBy가 기본값으로 설정된다`(testDB: TestDB) {
        withTables(testDB, LongAuditTable) {
            LongAuditTable.insert {
                it[title] = "long-title"
            }

            val row = LongAuditTable.selectAll().single()
            row[LongAuditTable.title] shouldBeEqualTo "long-title"
            row[LongAuditTable.createdBy] shouldBeEqualTo UserContext.DEFAULT_USERNAME
            row[LongAuditTable.createdAt].shouldNotBeNull()
            row[LongAuditTable.updatedBy] shouldBeEqualTo null
            row[LongAuditTable.updatedAt] shouldBeEqualTo null
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `AuditableLongIdTable - withUser 블록 내 INSERT 시 createdBy에 사용자명이 설정된다`(testDB: TestDB) {
        withTables(testDB, LongAuditTable) {
            UserContext.withUser("bob") {
                LongAuditTable.insert {
                    it[title] = "by-bob"
                }
            }

            val row = LongAuditTable.selectAll().single()
            row[LongAuditTable.createdBy] shouldBeEqualTo "bob"
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `AuditableUUIDTable - INSERT 시 createdBy가 기본값으로 설정된다`(testDB: TestDB) {
        withTables(testDB, UUIDAuditTable) {
            UUIDAuditTable.insert {
                it[label] = "uuid-label"
            }

            val row = UUIDAuditTable.selectAll().single()
            row[UUIDAuditTable.label] shouldBeEqualTo "uuid-label"
            row[UUIDAuditTable.createdBy] shouldBeEqualTo UserContext.DEFAULT_USERNAME
            row[UUIDAuditTable.createdAt].shouldNotBeNull()
            row[UUIDAuditTable.id].shouldNotBeNull()
            row[UUIDAuditTable.updatedBy] shouldBeEqualTo null
            row[UUIDAuditTable.updatedAt] shouldBeEqualTo null
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `AuditableUUIDTable - withUser 블록 내 INSERT 시 createdBy에 사용자명이 설정된다`(testDB: TestDB) {
        withTables(testDB, UUIDAuditTable) {
            UserContext.withUser("carol") {
                UUIDAuditTable.insert {
                    it[label] = "by-carol"
                }
            }

            val row = UUIDAuditTable.selectAll().single()
            row[UUIDAuditTable.createdBy] shouldBeEqualTo "carol"
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `AuditableIntIdTable - 컬럼 이름이 올바르게 정의된다`(testDB: TestDB) {
        withTables(testDB, IntAuditTable) {
            IntAuditTable.createdBy.name shouldBeEqualTo "created_by"
            IntAuditTable.createdAt.name shouldBeEqualTo "created_at"
            IntAuditTable.updatedBy.name shouldBeEqualTo "updated_by"
            IntAuditTable.updatedAt.name shouldBeEqualTo "updated_at"
        }
    }
}
