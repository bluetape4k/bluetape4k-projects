package io.bluetape4k.exposed.sql.encrypt

import io.bluetape4k.crypto.encrypt.Encryptors
import io.bluetape4k.exposed.AbstractExposedTest
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.exposed.utils.runWithTables
import io.bluetape4k.support.toUtf8String
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.junit.jupiter.api.Test

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

        var aesPassword by T1.aesVarChar
        var rc4Password by T1.rc4VarChar

        var aesBinary by T1.aesBinary
        var rc4Binary by T1.rc4Binary

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = id.hashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .add("aesPassword", aesPassword)
            .add("rc4Password", rc4Password)
            .toString()
    }

    @Test
    fun `save string via encryptor`() {
        runWithTables(T1) {
            val e1 = E1.new {
                name = "Alice"
                aesPassword = "aesPassword"
                rc4Password = "rc4Password"

                aesBinary = "aesBinary".toByteArray()
                rc4Binary = "rc4Binary".toByteArray()
            }
            entityCache.clear()

            val loaded = E1.findById(e1.id)!!

            loaded shouldBeEqualTo e1
            loaded.name shouldBeEqualTo "Alice"

            loaded.aesPassword shouldBeEqualTo "aesPassword"
            loaded.rc4Password shouldBeEqualTo "rc4Password"

            loaded.aesBinary!!.toUtf8String() shouldBeEqualTo "aesBinary"
            loaded.rc4Binary!!.toUtf8String() shouldBeEqualTo "rc4Binary"
        }
    }

    @Test
    fun `search by encrypted column`() {
        runWithTables(T1) {
            val e1 = E1.new {
                name = "Alice"
                aesPassword = "aesPassword"
                rc4Password = "rc4Password"
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
            val loaded = E1.find { T1.aesVarChar eq "aesPassword" }.single()
            loaded shouldBeEqualTo e1
        }
    }
}
