package io.bluetape4k.exposed.core.tink

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import io.bluetape4k.tink.aead.TinkAeads
import io.bluetape4k.tink.daead.TinkDaeads
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class TinkColumnTypeTest: AbstractExposedTest() {

    companion object: KLogging()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `AEAD 컬럼 암호화 및 복호화`(testDB: TestDB) {
        val secretTable = object: IntIdTable("tink_aead_table") {
            val secret = tinkAeadVarChar("secret", 512, TinkAeads.AES256_GCM).nullable()
            val data = tinkAeadBinary("data", 512, TinkAeads.AES256_GCM).nullable()
            val blob = tinkAeadBlob("blob", TinkAeads.AES256_GCM).nullable()
        }

        withTables(testDB, secretTable) {
            val insertedSecret = faker.lorem().sentence()
            val insertedData = faker.lorem().sentence()
            val insertedBlob = faker.lorem().sentence()

            val id = secretTable.insertAndGetId {
                it[secret] = insertedSecret
                it[data] = insertedData.toUtf8Bytes()
                it[blob] = insertedBlob.toUtf8Bytes()
            }

            secretTable.selectAll().count() shouldBeEqualTo 1L

            val row = secretTable.selectAll().where { secretTable.id eq id }.single()

            row[secretTable.secret] shouldBeEqualTo insertedSecret
            row[secretTable.data]!!.toUtf8String() shouldBeEqualTo insertedData
            row[secretTable.blob]!!.toUtf8String() shouldBeEqualTo insertedBlob
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAEAD 컬럼 암호화, 복호화 및 조건 검색`(testDB: TestDB) {
        val searchableTable = object: IntIdTable("tink_daead_table") {
            val email = tinkDaeadVarChar("email", 512, TinkDaeads.AES256_SIV).nullable().index()
            val fingerprint = tinkDaeadBinary("fingerprint", 256, TinkDaeads.AES256_SIV).nullable()
            val blob = tinkDaeadBlob("blob", TinkDaeads.AES256_SIV).nullable()
        }

        withTables(testDB, searchableTable) {
            val insertedEmail = faker.internet().emailAddress()
            val insertedFingerprint = faker.lorem().word()
            val insertedBlob = faker.lorem().sentence()

            val id = searchableTable.insertAndGetId {
                it[email] = insertedEmail
                it[fingerprint] = insertedFingerprint.toUtf8Bytes()
                it[blob] = insertedBlob.toUtf8Bytes()
            }

            searchableTable.selectAll().count() shouldBeEqualTo 1L

            val row = searchableTable.selectAll().where { searchableTable.id eq id }.single()

            row[searchableTable.email] shouldBeEqualTo insertedEmail
            row[searchableTable.fingerprint]!!.toUtf8String() shouldBeEqualTo insertedFingerprint
            row[searchableTable.blob]!!.toUtf8String() shouldBeEqualTo insertedBlob


            /**
             * DAEAD(결정적 암호화)는 WHERE 절로 검색이 가능합니다.
             * ```sql
             * SELECT COUNT(*) FROM tink_daead_table WHERE tink_daead_table.email = '<암호문>'
             * ```
             */
            searchableTable.selectAll()
                .where { searchableTable.email eq row[searchableTable.email] }
                .count() shouldBeEqualTo 1L

            searchableTable.selectAll()
                .where { searchableTable.fingerprint eq row[searchableTable.fingerprint] }
                .count() shouldBeEqualTo 1L

            searchableTable.selectAll()
                .where { searchableTable.blob eq row[searchableTable.blob] }
                .count() shouldBeEqualTo 1L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `AEAD 컬럼은 동일 평문으로 WHERE 조건 검색이 동작하지 않는다`(testDB: TestDB) {
        val aeadTable = object: IntIdTable("tink_aead_search_table") {
            val secret = tinkAeadVarChar("secret", 512, TinkAeads.AES256_GCM)
        }

        withTables(testDB, aeadTable) {
            val insertedSecret = faker.internet().emailAddress()

            aeadTable.insertAndGetId {
                it[secret] = insertedSecret
            }

            aeadTable.selectAll()
                .where { aeadTable.secret eq insertedSecret }
                .count() shouldBeEqualTo 0L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `컬럼 이름은 blank 일 수 없다`(testDB: TestDB) {
        assertThrows<IllegalArgumentException> {
            object: IntIdTable("invalid_name_aead_varchar_$testDB") {
                val invalid = tinkAeadVarChar(" ", 32)
            }
        }

        assertThrows<IllegalArgumentException> {
            object: IntIdTable("invalid_name_daead_blob_$testDB") {
                val invalid = tinkDaeadBlob("")
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `AEAD 컬럼 Update`(testDB: TestDB) {
        val secretTable = object: IntIdTable("tink_aead_update_table") {
            val secret = tinkAeadVarChar("secret", 512, TinkAeads.AES256_GCM)
            val data = tinkAeadBinary("data", 512, TinkAeads.AES256_GCM).nullable()
            val blob = tinkAeadBlob("blob", TinkAeads.AES256_GCM).nullable()
        }

        withTables(testDB, secretTable) {
            val insertedSecret = faker.lorem().sentence()
            val insertedData = faker.lorem().word()
            val insertedBlob = faker.lorem().word()

            val id = secretTable.insertAndGetId {
                it[secret] = insertedSecret
                it[data] = insertedData.toUtf8Bytes()
                it[blob] = insertedBlob.toUtf8Bytes()
            }

            val insertedRow = secretTable.selectAll().where { secretTable.id eq id }.single()
            insertedRow[secretTable.secret] shouldBeEqualTo insertedSecret
            insertedRow[secretTable.data]!!.toUtf8String() shouldBeEqualTo insertedData
            insertedRow[secretTable.blob]!!.toUtf8String() shouldBeEqualTo insertedBlob


            val updatedSecret = faker.lorem().sentence()
            val updatedData = faker.lorem().word()
            val updatedBlob = faker.lorem().word()

            secretTable.update({ secretTable.id eq id }) {
                it[secret] = updatedSecret
                it[data] = updatedData.toUtf8Bytes()
                it[blob] = updatedBlob.toUtf8Bytes()
            }

            val updatedRow = secretTable.selectAll().where { secretTable.id eq id }.single()
            updatedRow[secretTable.secret] shouldBeEqualTo updatedSecret
            updatedRow[secretTable.data]!!.toUtf8String() shouldBeEqualTo updatedData
            updatedRow[secretTable.blob]!!.toUtf8String() shouldBeEqualTo updatedBlob
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `nullable 암호화 컬럼은 null 값을 저장하고 조회할 수 있다`(testDB: TestDB) {
        val nullableTable = object: IntIdTable("tink_nullable_table") {
            val aeadSecret = tinkAeadVarChar("aead_secret", 512).nullable()
            val daeadSecret = tinkDaeadVarChar("daead_secret", 512).nullable()
            val aeadData = tinkAeadBinary("aead_data", 512).nullable()
            val daeadData = tinkDaeadBinary("daead_data", 512).nullable()
            val aeadBlob = tinkAeadBlob("aead_blob").nullable()
            val daeadBlob = tinkDaeadBlob("daead_blob").nullable()
        }

        withTables(testDB, nullableTable) {
            val id = nullableTable.insertAndGetId {
                it[aeadSecret] = null
                it[daeadSecret] = null
                it[aeadData] = null
                it[daeadData] = null
                it[aeadBlob] = null
                it[daeadBlob] = null
            }

            val row = nullableTable.selectAll().where { nullableTable.id eq id }.single()
            row[nullableTable.aeadSecret] shouldBeEqualTo null
            row[nullableTable.daeadSecret] shouldBeEqualTo null
            row[nullableTable.aeadData] shouldBeEqualTo null
            row[nullableTable.daeadData] shouldBeEqualTo null
            row[nullableTable.aeadBlob] shouldBeEqualTo null
            row[nullableTable.daeadBlob] shouldBeEqualTo null
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `컬럼 길이는 0보다 커야 한다`(testDB: TestDB) {
        assertThrows<IllegalArgumentException> {
            object: IntIdTable("invalid_aead_varchar_$testDB") {
                val invalid = tinkAeadVarChar("invalid", 0)
            }
        }

        assertThrows<IllegalArgumentException> {
            object: IntIdTable("invalid_aead_binary_$testDB") {
                val invalid = tinkAeadBinary("invalid", 0)
            }
        }

        assertThrows<IllegalArgumentException> {
            object: IntIdTable("invalid_daead_varchar_$testDB") {
                val invalid = tinkDaeadVarChar("invalid", 0)
            }
        }

        assertThrows<IllegalArgumentException> {
            object: IntIdTable("invalid_daead_binary_$testDB") {
                val invalid = tinkDaeadBinary("invalid", 0)
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `다양한 Tink AEAD 알고리즘으로 암호화 컬럼을 사용할 수 있다`(testDB: TestDB) {
        val multiAlgoTable = object: IntIdTable("tink_multi_algo_table") {
            val aes256 = tinkAeadVarChar("aes256", 512, TinkAeads.AES256_GCM)
            val aes128 = tinkAeadVarChar("aes128", 512, TinkAeads.AES128_GCM)
            val chacha20 = tinkAeadVarChar("chacha20", 512, TinkAeads.CHACHA20_POLY1305)
        }

        withTables(testDB, multiAlgoTable) {
            val value = faker.lorem().sentence()

            val id = multiAlgoTable.insertAndGetId {
                it[aes256] = value
                it[aes128] = value
                it[chacha20] = value
            }

            val row = multiAlgoTable.selectAll().where { multiAlgoTable.id eq id }.single()

            row[multiAlgoTable.aes256] shouldBeEqualTo value
            row[multiAlgoTable.aes128] shouldBeEqualTo value
            row[multiAlgoTable.chacha20] shouldBeEqualTo value
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `기본 AEAD VARCHAR 길이 255 는 일반적인 이메일 길이 암호문을 수용하지 못할 수 있다`(testDB: TestDB) {
        val constrainedTable = object: IntIdTable("tink_aead_default_length_$testDB") {
            val secret = tinkAeadVarChar("secret")
        }

        withTables(testDB, constrainedTable) {
            val longEmail = "${"a".repeat(180)}@example.com"

            assertThrows<Exception> {
                constrainedTable.insertAndGetId {
                    it[secret] = longEmail
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `AEAD VARCHAR 길이를 512 로 늘리면 일반적인 이메일 길이 암호문을 저장할 수 있다`(testDB: TestDB) {
        val roomyTable = object: IntIdTable("tink_aead_roomy_length_$testDB") {
            val secret = tinkAeadVarChar("secret", 512)
        }

        withTables(testDB, roomyTable) {
            val longEmail = "${"a".repeat(180)}@example.com"

            val id = roomyTable.insertAndGetId {
                it[secret] = longEmail
            }

            val row = roomyTable.selectAll().where { roomyTable.id eq id }.single()
            row[roomyTable.secret] shouldBeEqualTo longEmail
        }
    }
}
