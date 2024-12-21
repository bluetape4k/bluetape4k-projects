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

class EncryptedStringColumnTypeTest: AbstractExposedTest() {

    private object T1: IntIdTable() {
        val name = varchar("name", 255)
        val aesPassword = encryptedString("aes_password", Encryptors.AES)
        val rc4Password = encryptedString("rc4_password", Encryptors.RC4)
    }

    class E1(id: EntityID<Int>): IntEntity(id) {
        companion object: EntityClass<Int, E1>(T1)

        var name by T1.name
        var aesPassword by T1.aesPassword
        var rc4Password by T1.rc4Password
    }

    @Test
    fun `save string via encryptor`() {
        runWithTables(T1) {
            val e1 = E1.new {
                name = "Alice"
                aesPassword = "aesPassword"
                rc4Password = "rc4Password"
            }
            entityCache.clear()

            val loaded = E1.findById(e1.id)!!
            loaded.name shouldBeEqualTo "Alice"
            loaded.aesPassword shouldBeEqualTo "aesPassword"
            loaded.rc4Password shouldBeEqualTo "rc4Password"
        }
    }
}
