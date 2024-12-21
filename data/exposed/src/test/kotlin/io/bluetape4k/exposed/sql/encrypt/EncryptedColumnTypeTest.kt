package io.bluetape4k.exposed.sql.encrypt

import io.bluetape4k.crypto.encrypt.Encryptors
import io.bluetape4k.exposed.AbstractExposedTest
import io.bluetape4k.exposed.utils.runWithTables
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.junit.jupiter.api.Test

class EncryptedColumnTypeTest: AbstractExposedTest() {

    private object T1: IntIdTable() {
        val name = varchar("name", 255)
        val aesVarChar = encryptedVarChar("aes_password", 255, Encryptors.AES)
        val rc4VarChar = encryptedVarChar("rc4_password", 255, Encryptors.RC4)

        val aesBinary = encryptedBinary("aes_binary", 255, Encryptors.AES)
        val rc4Binary = encryptedBinary("rc4_binary", 255, Encryptors.RC4)
    }

    class E1(id: EntityID<Int>): IntEntity(id) {
        companion object: EntityClass<Int, E1>(T1)

        var name by T1.name

        var aesPassword by T1.aesVarChar
        var rc4Password by T1.rc4VarChar

        var aesBinary by T1.aesBinary
        var rc4Binary by T1.rc4Binary
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
            loaded.name shouldBeEqualTo "Alice"
            loaded.aesPassword shouldBeEqualTo "aesPassword"
            loaded.rc4Password shouldBeEqualTo "rc4Password"

            loaded.aesBinary shouldBeEqualTo "aesBinary".toByteArray()
            loaded.rc4Binary shouldBeEqualTo "rc4Binary".toByteArray()
        }
    }
}
