package io.bluetape4k.exposed.sql.encrypt

import io.bluetape4k.crypto.encrypt.Encryptors
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class EncryptedColumnTypeTest: AbstractExposedTest() {

    private object T1: IntIdTable() {
        val name = varchar("name", 255)
        val aesVarChar = encryptedVarChar("aes_password", 255, Encryptors.AES).nullable()
        val rc4VarChar = encryptedVarChar("rc4_password", 255, Encryptors.RC4).nullable()

        val aesBinary = encryptedBinary("aes_binary", 1024, Encryptors.AES).nullable()
        val rc4Binary = encryptedBinary("rc4_binary", 1024, Encryptors.RC4).nullable()
    }

    class E1(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<E1>(T1)

        var name by T1.name

        var aesVarChar by T1.aesVarChar
        var rc4VarChar by T1.rc4VarChar

        var aesBinary by T1.aesBinary
        var rc4Binary by T1.rc4Binary

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = id.hashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .add("aesPassword", this@E1.aesVarChar)
            .add("rc4Password", rc4VarChar)
            .toString()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DSL 방식으로 컬럼 암호화 하기`(testDB: TestDB) {
        withTables(testDB, T1) {
            val id1 = T1.insertAndGetId {
                it[name] = "Alice"
                it[aesVarChar] = "aesVarChar"
                it[rc4VarChar] = "rc4VarChar"

                it[aesBinary] = "aesBinary".toUtf8Bytes()
                it[rc4Binary] = "rc4Binary".toUtf8Bytes()
            }

            val row = T1.selectAll().where { T1.id eq id1 }.single()

            row[T1.id] shouldBeEqualTo id1
            row[T1.aesVarChar] shouldBeEqualTo "aesVarChar"
            row[T1.rc4VarChar] shouldBeEqualTo "rc4VarChar"
            row[T1.aesBinary]?.toUtf8String() shouldBeEqualTo "aesBinary"
            row[T1.rc4Binary]?.toUtf8String() shouldBeEqualTo "rc4Binary"
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAO 방식으로 컬럼 암호화하기`(testDB: TestDB) {
        withTables(testDB, T1) {
            val e1 = E1.new {
                name = "Alice"
                aesVarChar = "aesVarChar"
                rc4VarChar = "rc4VarChar"

                aesBinary = "aesBinary".toUtf8Bytes()
                rc4Binary = "rc4Binary".toUtf8Bytes()
            }
            entityCache.clear()

            val loaded = E1.findById(e1.id)!!

            loaded shouldBeEqualTo e1
            loaded.name shouldBeEqualTo "Alice"
            loaded.aesVarChar shouldBeEqualTo "aesVarChar"
            loaded.rc4VarChar shouldBeEqualTo "rc4VarChar"
            loaded.aesBinary!!.toUtf8String() shouldBeEqualTo "aesBinary"
            loaded.rc4Binary!!.toUtf8String() shouldBeEqualTo "rc4Binary"
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DSL 방식 - 암호화된 컬럼으로 검색하기`(testDB: TestDB) {
        withTables(testDB, T1) {
            val id1 = T1.insertAndGetId {
                it[name] = "Alice"
                it[aesVarChar] = "aesVarChar"
                it[rc4VarChar] = "rc4VarChar"

                it[aesBinary] = "aesBinary".toUtf8Bytes()
                it[rc4Binary] = "rc4Binary".toUtf8Bytes()
            }

            val row = T1.selectAll().where { T1.aesVarChar eq "aesVarChar" }.single()

            row[T1.id] shouldBeEqualTo id1
            row[T1.aesVarChar] shouldBeEqualTo "aesVarChar"
            row[T1.rc4VarChar] shouldBeEqualTo "rc4VarChar"
            row[T1.aesBinary]?.toUtf8String() shouldBeEqualTo "aesBinary"
            row[T1.rc4Binary]?.toUtf8String() shouldBeEqualTo "rc4Binary"
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAO 방식 - 암호화된 컬럼으로 검색하기`(testDB: TestDB) {
        withTables(testDB, T1) {
            val e1 = E1.new {
                name = "Alice"
                aesVarChar = "aesVarChar"
                rc4VarChar = "rc4VarChar"

                aesBinary = "aesBinary".toUtf8Bytes()
                rc4Binary = "rc4Binary".toUtf8Bytes()
            }

            /**
             * ```sql
             * SELECT T1.ID,
             *        T1."name",
             *        T1.AES_PASSWORD,
             *        T1.RC4_PASSWORD,
             *        T1.AES_BINARY,
             *        T1.RC4_BINARY
             *   FROM T1
             *  WHERE T1.AES_PASSWORD = HqssFPg0zN3pqJdFKCSZZ1RczuI8YVN7mauc8H2NgGU=
             */
            val loaded = E1.find { T1.aesVarChar eq "aesVarChar" }.single()
            loaded shouldBeEqualTo e1
            loaded.name shouldBeEqualTo "Alice"
            loaded.aesVarChar shouldBeEqualTo "aesVarChar"
            loaded.rc4VarChar shouldBeEqualTo "rc4VarChar"
            loaded.aesBinary!!.toUtf8String() shouldBeEqualTo "aesBinary"
            loaded.rc4Binary!!.toUtf8String() shouldBeEqualTo "rc4Binary"
        }
    }
}
