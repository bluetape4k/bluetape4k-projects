package io.bluetape4k.exposed.core.tink

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import io.bluetape4k.tink.aead.TinkAeads
import io.bluetape4k.tink.daead.TinkDaeads
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * [tinkAeadVarChar], [tinkAeadBinary], [tinkAeadBlob],
 * [tinkDaeadVarChar], [tinkDaeadBinary], [tinkDaeadBlob]
 * 확장 함수로 생성된 테이블 컬럼 타입과 동작을 검증하는 통합 테스트입니다.
 *
 * - AEAD 컬럼: 비결정적 암호화, 인덱스/조건 검색 불가
 * - DAEAD 컬럼: 결정적 암호화, 인덱스/조건 검색 가능
 */
class TinkTableTest: AbstractExposedTest() {

    companion object: KLogging()

    // ── AEAD 테이블 정의 ──────────────────────────────────────────────────────

    /** AEAD(비결정적) 암호화 컬럼 전체를 포함하는 테이블 */
    object TinkAeadTable: IntIdTable("tink_aead_full_table") {
        val varchar = tinkAeadVarChar("varchar_col", 512, TinkAeads.AES256_GCM).nullable()
        val binary = tinkAeadBinary("binary_col", 512, TinkAeads.AES256_GCM).nullable()
        val blob = tinkAeadBlob("blob_col", TinkAeads.AES256_GCM).nullable()
    }

    // ── DAEAD 테이블 정의 ─────────────────────────────────────────────────────

    /** DAEAD(결정적) 암호화 컬럼 전체를 포함하는 테이블 */
    object TinkDaeadTable: IntIdTable("tink_daead_full_table") {
        val varchar = tinkDaeadVarChar("varchar_col", 512, TinkDaeads.AES256_SIV).nullable().index()
        val binary = tinkDaeadBinary("binary_col", 512, TinkDaeads.AES256_SIV).nullable()
        val blob = tinkDaeadBlob("blob_col", TinkDaeads.AES256_SIV).nullable()
    }

    // ── AEAD 컬럼 구조 검증 ───────────────────────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `TinkAeadTable 컬럼이 올바른 타입으로 등록된다`(testDB: TestDB) {
        withTables(testDB, TinkAeadTable) {
            // 컬럼이 정상적으로 등록되었는지 null-check로 검증
            (TinkAeadTable.varchar as Column<*>).shouldNotBeNull()
            (TinkAeadTable.binary as Column<*>).shouldNotBeNull()
            (TinkAeadTable.blob as Column<*>).shouldNotBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `TinkDaeadTable 컬럼이 올바른 타입으로 등록된다`(testDB: TestDB) {
        withTables(testDB, TinkDaeadTable) {
            (TinkDaeadTable.varchar as Column<*>).shouldNotBeNull()
            (TinkDaeadTable.binary as Column<*>).shouldNotBeNull()
            (TinkDaeadTable.blob as Column<*>).shouldNotBeNull()
        }
    }

    // ── AEAD 암호화 INSERT / SELECT ────────────────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `TinkAeadTable 모든 컬럼에 값을 저장하고 복호화해서 조회한다`(testDB: TestDB) {
        withTables(testDB, TinkAeadTable) {
            val text = faker.lorem().sentence()
            val bytes = faker.address().fullAddress().toUtf8Bytes()
            val blobBytes = faker.lorem().paragraph().toUtf8Bytes()

            val id = TinkAeadTable.insertAndGetId {
                it[varchar] = text
                it[binary] = bytes
                it[blob] = blobBytes
            }

            TinkAeadTable.selectAll().count() shouldBeEqualTo 1L

            val row = TinkAeadTable.selectAll().where { TinkAeadTable.id eq id }.single()

            row[TinkAeadTable.varchar] shouldBeEqualTo text
            row[TinkAeadTable.binary]!!.toUtf8String() shouldBeEqualTo bytes.toUtf8String()
            row[TinkAeadTable.blob]!!.toUtf8String() shouldBeEqualTo blobBytes.toUtf8String()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `TinkAeadTable nullable 컬럼에 null 을 저장하고 조회한다`(testDB: TestDB) {
        withTables(testDB, TinkAeadTable) {
            val id = TinkAeadTable.insertAndGetId {
                it[varchar] = null
                it[binary] = null
                it[blob] = null
            }

            val row = TinkAeadTable.selectAll().where { TinkAeadTable.id eq id }.single()

            row[TinkAeadTable.varchar] shouldBeEqualTo null
            row[TinkAeadTable.binary] shouldBeEqualTo null
            row[TinkAeadTable.blob] shouldBeEqualTo null
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `TinkAeadTable 컬럼 UPDATE 후 복호화 값이 갱신된다`(testDB: TestDB) {
        withTables(testDB, TinkAeadTable) {
            val original = faker.lorem().sentence()
            val updated = faker.lorem().sentence()

            val id = TinkAeadTable.insertAndGetId {
                it[varchar] = original
                it[binary] = original.toUtf8Bytes()
                it[blob] = original.toUtf8Bytes()
            }

            TinkAeadTable.update({ TinkAeadTable.id eq id }) {
                it[varchar] = updated
                it[binary] = updated.toUtf8Bytes()
                it[blob] = updated.toUtf8Bytes()
            }

            val row = TinkAeadTable.selectAll().where { TinkAeadTable.id eq id }.single()

            row[TinkAeadTable.varchar] shouldBeEqualTo updated
            row[TinkAeadTable.binary]!!.toUtf8String() shouldBeEqualTo updated
            row[TinkAeadTable.blob]!!.toUtf8String() shouldBeEqualTo updated
        }
    }

    // ── DAEAD 암호화 INSERT / SELECT / WHERE ───────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `TinkDaeadTable 모든 컬럼에 값을 저장하고 복호화해서 조회한다`(testDB: TestDB) {
        withTables(testDB, TinkDaeadTable) {
            val text = faker.internet().emailAddress()
            val bytes = faker.address().fullAddress().toUtf8Bytes()
            val blobBytes = faker.lorem().paragraph().toUtf8Bytes()

            val id = TinkDaeadTable.insertAndGetId {
                it[varchar] = text
                it[binary] = bytes
                it[blob] = blobBytes
            }

            TinkDaeadTable.selectAll().count() shouldBeEqualTo 1L

            val row = TinkDaeadTable.selectAll().where { TinkDaeadTable.id eq id }.single()

            row[TinkDaeadTable.varchar] shouldBeEqualTo text
            row[TinkDaeadTable.binary]!!.toUtf8String() shouldBeEqualTo bytes.toUtf8String()
            row[TinkDaeadTable.blob]!!.toUtf8String() shouldBeEqualTo blobBytes.toUtf8String()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `TinkDaeadTable varchar 컬럼은 WHERE 조건 검색이 가능하다`(testDB: TestDB) {
        withTables(testDB, TinkDaeadTable) {
            val email1 = "alice@example.com"
            val email2 = "bob@example.com"

            TinkDaeadTable.insertAndGetId { it[varchar] = email1 }
            TinkDaeadTable.insertAndGetId { it[varchar] = email2 }

            TinkDaeadTable.selectAll().count() shouldBeEqualTo 2L

            // DAEAD 결정적 암호화로 WHERE 절 검색이 정확히 1건을 반환해야 한다
            TinkDaeadTable.selectAll()
                .where { TinkDaeadTable.varchar eq email1 }
                .count() shouldBeEqualTo 1L

            TinkDaeadTable.selectAll()
                .where { TinkDaeadTable.varchar eq email1 }
                .single()[TinkDaeadTable.varchar] shouldBeEqualTo email1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `TinkDaeadTable binary 컬럼은 WHERE 조건 검색이 가능하다`(testDB: TestDB) {
        withTables(testDB, TinkDaeadTable) {
            val fingerprint = "fingerprint-data-unique".toUtf8Bytes()

            TinkDaeadTable.insertAndGetId { it[binary] = fingerprint }

            TinkDaeadTable.selectAll()
                .where { TinkDaeadTable.binary eq fingerprint }
                .count() shouldBeEqualTo 1L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `TinkDaeadTable nullable 컬럼에 null 을 저장하고 조회한다`(testDB: TestDB) {
        withTables(testDB, TinkDaeadTable) {
            val id = TinkDaeadTable.insertAndGetId {
                it[varchar] = null
                it[binary] = null
                it[blob] = null
            }

            val row = TinkDaeadTable.selectAll().where { TinkDaeadTable.id eq id }.single()

            row[TinkDaeadTable.varchar] shouldBeEqualTo null
            row[TinkDaeadTable.binary] shouldBeEqualTo null
            row[TinkDaeadTable.blob] shouldBeEqualTo null
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `TinkDaeadTable varchar UPDATE 후 새 값으로 재검색이 가능하다`(testDB: TestDB) {
        withTables(testDB, TinkDaeadTable) {
            val original = "original@example.com"
            val updated = "updated@example.com"

            val id = TinkDaeadTable.insertAndGetId { it[varchar] = original }

            // 업데이트 전: 원본으로 검색 가능
            TinkDaeadTable.selectAll()
                .where { TinkDaeadTable.varchar eq original }
                .count() shouldBeEqualTo 1L

            TinkDaeadTable.update({ TinkDaeadTable.id eq id }) {
                it[varchar] = updated
            }

            // 업데이트 후: 새 값으로 검색 가능, 이전 값은 0건
            TinkDaeadTable.selectAll()
                .where { TinkDaeadTable.varchar eq updated }
                .count() shouldBeEqualTo 1L

            TinkDaeadTable.selectAll()
                .where { TinkDaeadTable.varchar eq original }
                .count() shouldBeEqualTo 0L
        }
    }

    // ── 여러 행 시나리오 ──────────────────────────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `TinkAeadTable 여러 행을 저장하고 각각 올바르게 복호화된다`(testDB: TestDB) {
        withTables(testDB, TinkAeadTable) {
            val records = (1..5).map { faker.lorem().sentence() }

            val ids = records.map { text ->
                TinkAeadTable.insertAndGetId { it[varchar] = text }
            }

            TinkAeadTable.selectAll().count() shouldBeEqualTo 5L

            ids.zip(records).forEach { (id, expected) ->
                val row = TinkAeadTable.selectAll().where { TinkAeadTable.id eq id }.single()
                row[TinkAeadTable.varchar] shouldBeEqualTo expected
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `TinkDaeadTable 여러 행 중 특정 이메일만 정확히 검색된다`(testDB: TestDB) {
        withTables(testDB, TinkDaeadTable) {
            val emails = listOf("alice@example.com", "bob@example.com", "carol@example.com")
            emails.forEach { email ->
                TinkDaeadTable.insertAndGetId { it[varchar] = email }
            }

            // 각 이메일별로 WHERE 검색이 정확히 1건만 반환해야 한다
            emails.forEach { email ->
                TinkDaeadTable.selectAll()
                    .where { TinkDaeadTable.varchar eq email }
                    .count() shouldBeEqualTo 1L
            }
        }
    }
}
