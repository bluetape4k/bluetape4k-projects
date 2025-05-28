package io.bluetape4k.exposed.core.jasypt

import io.bluetape4k.crypto.encrypt.Encryptors
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class JasyptColumnTypeTest: AbstractExposedTest() {

    companion object: KLogging()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `문자열에 대해 암호화,복호화 하기`(testDB: TestDB) {
        val stringTable = object: IntIdTable("string_table") {
            val name = jasyptVarChar("name", 255, Encryptors.AES).nullable().index()
            val city = jasyptVarChar("city", 255, Encryptors.RC4).nullable().index()
            val address = jasyptBinary("address", 255, Encryptors.TripleDES).nullable()
            val age = jasyptVarChar("age", 255, Encryptors.RC2).nullable()
        }

        withTables(testDB, stringTable) {
            val insertedName = faker.name().firstName()
            val insertedCity = faker.address().city()
            val insertedAddress = faker.address().fullAddress()
            val insertedAge = faker.number().numberBetween(18, 90).toString()

            val id = stringTable.insertAndGetId {
                it[name] = insertedName
                it[city] = insertedCity
                it[address] = insertedAddress.toUtf8Bytes()
                it[age] = insertedAge
            }

            stringTable.selectAll().count() shouldBeEqualTo 1L

            val row = stringTable.selectAll().where { stringTable.id eq id }.single()

            row[stringTable.name] shouldBeEqualTo insertedName
            row[stringTable.city] shouldBeEqualTo insertedCity
            row[stringTable.address]!!.toUtf8String() shouldBeEqualTo insertedAddress
            row[stringTable.age] shouldBeEqualTo insertedAge

            /**
             * Jasypt 암호화는 항상 같은 결과를 반환하므로, WHERE 절로 검색이 가능합니다.
             * ```sql
             * SELECT COUNT(*) FROM string_table WHERE string_table.`name` = UPq8X_QFkR-tsUFSOwffVQ==
             * ```
             */
            stringTable.selectAll()
                .where { stringTable.name eq row[stringTable.name] }
                .count() shouldBeEqualTo 1L

            /**
             * ```sql
             * SELECT COUNT(*) FROM string_table WHERE string_table.city = z4BcM_G7SfeezTVchpiz2U5PcOG8z88x
             * ```
             */
            stringTable.selectAll()
                .where { stringTable.city eq row[stringTable.city] }
                .count() shouldBeEqualTo 1L

            stringTable.selectAll()
                .where { stringTable.address eq row[stringTable.address] }
                .count() shouldBeEqualTo 1L

            stringTable.selectAll()
                .where { stringTable.age eq row[stringTable.age] }
                .count() shouldBeEqualTo 1L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `암호화된 컬럼을 Update 하기`(testDB: TestDB) {
        val stringTable = object: IntIdTable("string_table") {
            val name = jasyptVarChar("name", 255, Encryptors.AES).index()
            val city = jasyptVarChar("city", 255, Encryptors.RC4).index()
            val address = jasyptBinary("address", 255, Encryptors.TripleDES).nullable()
        }

        withTables(testDB, stringTable) {
            val insertedName = faker.name().firstName()
            val insertedCity = faker.address().city()
            val insertedAddress = faker.address().fullAddress()

            val id = stringTable.insertAndGetId {
                it[name] = insertedName
                it[city] = insertedCity
                it[address] = insertedAddress.toUtf8Bytes()
            }
            val insertedRow = stringTable.selectAll().where { stringTable.id eq id }.single()
            insertedRow[stringTable.name] shouldBeEqualTo insertedName
            insertedRow[stringTable.city] shouldBeEqualTo insertedCity
            insertedRow[stringTable.address]!!.toUtf8String() shouldBeEqualTo insertedAddress

            val updatedName = faker.name().firstName()
            val updatedCity = faker.address().city()
            val updatedAddress = faker.address().fullAddress()

            stringTable.update({ stringTable.id eq id }) {
                it[name] = updatedName
                it[city] = updatedCity
                it[address] = updatedAddress.toUtf8Bytes()
            }

            val updatedRow = stringTable.selectAll().where { stringTable.id eq id }.single()

            updatedRow[stringTable.name] shouldBeEqualTo updatedName
            updatedRow[stringTable.city] shouldBeEqualTo updatedCity
            updatedRow[stringTable.address]!!.toUtf8String() shouldBeEqualTo updatedAddress
        }
    }
}
