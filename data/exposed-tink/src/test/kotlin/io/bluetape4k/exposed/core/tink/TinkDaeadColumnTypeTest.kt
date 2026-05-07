package io.bluetape4k.exposed.core.tink

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import io.bluetape4k.tink.daead.TinkDaeads
import io.bluetape4k.assertions.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import io.bluetape4k.assertions.assertFailsWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Tink Deterministic AEAD(DAEAD) 컬럼 타입들에 대한 단위/통합 테스트입니다.
 *
 * - [TinkDaeadBinaryColumnType]: `VARBINARY` 컬럼, 결정적 암호화 바이트 배열
 * - [TinkDaeadBlobColumnType]: `BLOB` 컬럼, 결정적 암호화 바이트 배열
 * - [TinkDaeadVarCharColumnType]: `VARCHAR` 컬럼, 결정적 암호화 문자열
 *
 * 결정적 암호화는 동일 평문에 대해 항상 동일한 암호문을 생성하므로
 * `WHERE col = value` 형태의 인덱스 조건 검색이 가능합니다.
 */
class TinkDaeadColumnTypeTest: AbstractExposedTest() {

    companion object: KLogging()

    // ── TinkDaeadBinaryColumnType ─────────────────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAEAD Binary 컬럼은 암호화 복호화 라운드트립을 보장한다`(testDB: TestDB) {
        val table = object: IntIdTable("tink_daead_binary_rt_$testDB") {
            val data = tinkDaeadBinary("data", 512, TinkDaeads.AES256_SIV)
        }

        withTables(testDB, table) {
            val plaintext = faker.lorem().paragraph().toUtf8Bytes()

            val id = table.insertAndGetId { it[data] = plaintext }

            val row = table.selectAll().where { table.id eq id }.single()

            row[table.data] shouldBeEqualTo plaintext
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAEAD Binary 컬럼은 결정적이므로 WHERE 조건 검색이 가능하다`(testDB: TestDB) {
        val table = object: IntIdTable("tink_daead_binary_search_$testDB") {
            val fingerprint = tinkDaeadBinary("fingerprint", 256, TinkDaeads.AES256_SIV)
        }

        withTables(testDB, table) {
            val fp1 = "fingerprint-alice".toUtf8Bytes()
            val fp2 = "fingerprint-bob".toUtf8Bytes()

            table.insertAndGetId { it[fingerprint] = fp1 }
            table.insertAndGetId { it[fingerprint] = fp2 }

            table.selectAll().count() shouldBeEqualTo 2L

            // 결정적 암호화로 동일 평문 → 동일 암호문 → WHERE 조건 검색 가능
            table.selectAll()
                .where { table.fingerprint eq fp1 }
                .count() shouldBeEqualTo 1L

            table.selectAll()
                .where { table.fingerprint eq fp1 }
                .single()[table.fingerprint] shouldBeEqualTo fp1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAEAD Binary 컬럼 UPDATE 후 새 값으로 재검색 가능하다`(testDB: TestDB) {
        val table = object: IntIdTable("tink_daead_binary_upd_$testDB") {
            val data = tinkDaeadBinary("data", 512, TinkDaeads.AES256_SIV)
        }

        withTables(testDB, table) {
            val original = "original-bytes".toUtf8Bytes()
            val updated = "updated-bytes".toUtf8Bytes()

            val id = table.insertAndGetId { it[data] = original }

            table.selectAll().where { table.data eq original }.count() shouldBeEqualTo 1L

            table.update({ table.id eq id }) { it[data] = updated }

            table.selectAll().where { table.data eq updated }.count() shouldBeEqualTo 1L
            table.selectAll().where { table.data eq original }.count() shouldBeEqualTo 0L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAEAD Binary 컬럼 길이가 0 이하이면 IllegalArgumentException 이 발생한다`(testDB: TestDB) {
        assertFailsWith<IllegalArgumentException> {
            object: IntIdTable("invalid_daead_binary_len_$testDB") {
                val data = tinkDaeadBinary("data", 0)
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAEAD Binary 컬럼 이름이 blank 이면 IllegalArgumentException 이 발생한다`(testDB: TestDB) {
        assertFailsWith<IllegalArgumentException> {
            object: IntIdTable("invalid_daead_binary_name_$testDB") {
                val data = tinkDaeadBinary("  ", 256)
            }
        }
    }

    // ── TinkDaeadBlobColumnType ───────────────────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAEAD Blob 컬럼은 암호화 복호화 라운드트립을 보장한다`(testDB: TestDB) {
        val table = object: IntIdTable("tink_daead_blob_rt_$testDB") {
            val data = tinkDaeadBlob("data", TinkDaeads.AES256_SIV)
        }

        withTables(testDB, table) {
            val plaintext = faker.lorem().paragraph().toUtf8Bytes()

            val id = table.insertAndGetId { it[data] = plaintext }

            val row = table.selectAll().where { table.id eq id }.single()

            row[table.data] shouldBeEqualTo plaintext
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAEAD Blob 컬럼은 대용량 데이터도 정상 처리된다`(testDB: TestDB) {
        val table = object: IntIdTable("tink_daead_blob_large_$testDB") {
            val payload = tinkDaeadBlob("payload", TinkDaeads.AES256_SIV)
        }

        withTables(testDB, table) {
            // 4KB 크기의 데이터 테스트
            val largeData = ByteArray(4096) { it.toByte() }

            val id = table.insertAndGetId { it[payload] = largeData }

            val row = table.selectAll().where { table.id eq id }.single()

            row[table.payload] shouldBeEqualTo largeData
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAEAD Blob 컬럼은 결정적이므로 WHERE 조건 검색이 가능하다`(testDB: TestDB) {
        val table = object: IntIdTable("tink_daead_blob_search_$testDB") {
            val data = tinkDaeadBlob("data", TinkDaeads.AES256_SIV)
        }

        withTables(testDB, table) {
            val payload = "searchable-blob-content".toUtf8Bytes()
            val other = "other-blob-content".toUtf8Bytes()

            table.insertAndGetId { it[data] = payload }
            table.insertAndGetId { it[data] = other }

            table.selectAll()
                .where { table.data eq payload }
                .count() shouldBeEqualTo 1L

            table.selectAll()
                .where { table.data eq payload }
                .single()[table.data] shouldBeEqualTo payload
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAEAD Blob nullable 컬럼에 null 을 저장하고 조회한다`(testDB: TestDB) {
        val table = object: IntIdTable("tink_daead_blob_null_$testDB") {
            val data = tinkDaeadBlob("data", TinkDaeads.AES256_SIV).nullable()
        }

        withTables(testDB, table) {
            val id = table.insertAndGetId { it[data] = null }

            val row = table.selectAll().where { table.id eq id }.single()

            row[table.data] shouldBeEqualTo null
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAEAD Blob 컬럼 이름이 blank 이면 IllegalArgumentException 이 발생한다`(testDB: TestDB) {
        assertFailsWith<IllegalArgumentException> {
            object: IntIdTable("invalid_daead_blob_name_$testDB") {
                val data = tinkDaeadBlob("")
            }
        }
    }

    // ── TinkDaeadVarCharColumnType ────────────────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAEAD VarChar 컬럼은 암호화 복호화 라운드트립을 보장한다`(testDB: TestDB) {
        val table = object: IntIdTable("tink_daead_varchar_rt_$testDB") {
            val email = tinkDaeadVarChar("email", 512, TinkDaeads.AES256_SIV)
        }

        withTables(testDB, table) {
            val plaintext = faker.internet().emailAddress()

            val id = table.insertAndGetId { it[email] = plaintext }

            val row = table.selectAll().where { table.id eq id }.single()

            row[table.email] shouldBeEqualTo plaintext
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAEAD VarChar 컬럼은 결정적이므로 WHERE 조건 검색이 가능하다`(testDB: TestDB) {
        val table = object: IntIdTable("tink_daead_varchar_search_$testDB") {
            val email = tinkDaeadVarChar("email", 512, TinkDaeads.AES256_SIV).index()
        }

        withTables(testDB, table) {
            val email1 = "alice@example.com"
            val email2 = "bob@example.com"
            val email3 = "carol@example.com"

            table.insertAndGetId { it[email] = email1 }
            table.insertAndGetId { it[email] = email2 }
            table.insertAndGetId { it[email] = email3 }

            table.selectAll().count() shouldBeEqualTo 3L

            listOf(email1, email2, email3).forEach { expected ->
                table.selectAll()
                    .where { table.email eq expected }
                    .count() shouldBeEqualTo 1L

                table.selectAll()
                    .where { table.email eq expected }
                    .single()[table.email] shouldBeEqualTo expected
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAEAD VarChar 컬럼 UPDATE 후 새 값으로 재검색 가능하다`(testDB: TestDB) {
        val table = object: IntIdTable("tink_daead_varchar_upd_$testDB") {
            val email = tinkDaeadVarChar("email", 512, TinkDaeads.AES256_SIV)
        }

        withTables(testDB, table) {
            val original = "original@example.com"
            val updated = "updated@example.com"

            val id = table.insertAndGetId { it[email] = original }

            table.selectAll().where { table.email eq original }.count() shouldBeEqualTo 1L

            table.update({ table.id eq id }) { it[email] = updated }

            table.selectAll().where { table.email eq updated }.count() shouldBeEqualTo 1L
            table.selectAll().where { table.email eq original }.count() shouldBeEqualTo 0L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAEAD VarChar nullable 컬럼에 null 을 저장하고 조회한다`(testDB: TestDB) {
        val table = object: IntIdTable("tink_daead_varchar_null_$testDB") {
            val email = tinkDaeadVarChar("email", 512).nullable()
        }

        withTables(testDB, table) {
            val id = table.insertAndGetId { it[email] = null }

            val row = table.selectAll().where { table.id eq id }.single()

            row[table.email] shouldBeEqualTo null
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAEAD VarChar 컬럼 길이가 0 이하이면 IllegalArgumentException 이 발생한다`(testDB: TestDB) {
        assertFailsWith<IllegalArgumentException> {
            object: IntIdTable("invalid_daead_varchar_len_$testDB") {
                val email = tinkDaeadVarChar("email", 0)
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAEAD VarChar 컬럼 이름이 blank 이면 IllegalArgumentException 이 발생한다`(testDB: TestDB) {
        assertFailsWith<IllegalArgumentException> {
            object: IntIdTable("invalid_daead_varchar_name_$testDB") {
                val email = tinkDaeadVarChar("", 256)
            }
        }
    }

    // ── 여러 행 + 다중 컬럼 복합 시나리오 ────────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAEAD 혼합 테이블에서 varchar binary blob 각 컬럼으로 개별 검색이 가능하다`(testDB: TestDB) {
        val table = object: IntIdTable("tink_daead_mixed_$testDB") {
            val email = tinkDaeadVarChar("email", 512, TinkDaeads.AES256_SIV)
            val fingerprint = tinkDaeadBinary("fingerprint", 256, TinkDaeads.AES256_SIV)
            val payload = tinkDaeadBlob("payload", TinkDaeads.AES256_SIV)
        }

        withTables(testDB, table) {
            val email = faker.internet().emailAddress()
            val fingerprint = faker.lorem().word().toUtf8Bytes()
            val payload = faker.lorem().sentence().toUtf8Bytes()

            val id = table.insertAndGetId {
                it[table.email] = email
                it[table.fingerprint] = fingerprint
                it[table.payload] = payload
            }

            // id로 조회해서 모든 컬럼이 올바르게 복호화됨을 확인
            val row = table.selectAll().where { table.id eq id }.single()

            row[table.email] shouldBeEqualTo email
            row[table.fingerprint] shouldBeEqualTo fingerprint
            row[table.payload] shouldBeEqualTo payload

            // 각 컬럼별 WHERE 검색도 정상 동작
            table.selectAll().where { table.email eq email }.count() shouldBeEqualTo 1L
            table.selectAll().where { table.fingerprint eq fingerprint }.count() shouldBeEqualTo 1L
            table.selectAll().where { table.payload eq payload }.count() shouldBeEqualTo 1L
        }
    }
}
