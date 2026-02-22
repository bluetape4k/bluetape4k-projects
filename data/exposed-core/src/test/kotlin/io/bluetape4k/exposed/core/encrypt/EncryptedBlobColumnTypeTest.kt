package io.bluetape4k.exposed.core.encrypt

import io.bluetape4k.crypto.encrypt.Encryptors
import io.bluetape4k.exposed.dao.entityToStringBuilder
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class EncryptedBlobColumnTypeTest: AbstractExposedTest() {

    private object T1: IntIdTable() {
        val name = varchar("name", 255)
        val aesBlob = encryptedBlob("aes_blob", Encryptors.AES).nullable()
        val rc4Blob = encryptedBlob("rc4_blob", Encryptors.RC4).nullable()
    }

    class E1(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<E1>(T1)

        var name by T1.name

        var aesBlob by T1.aesBlob
        var rc4Blob by T1.rc4Blob


        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = id.hashCode()
        override fun toString(): String = entityToStringBuilder()
            .add("name", name)
            .add("aesBlob", aesBlob?.toUtf8String())
            .add("rc4Blob", rc4Blob?.toUtf8String())
            .toString()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DSL 방식으로 컬럼 암호화 하기`(testDB: TestDB) {
        withTables(testDB, T1) {
            val id1 = T1.insertAndGetId {
                it[name] = "Alice"
                it[aesBlob] = "aesBlob".toUtf8Bytes()
                it[rc4Blob] = "rc4Blob".toUtf8Bytes()
            }

            val row = T1.selectAll().where { T1.id eq id1 }.single()

            row[T1.id] shouldBeEqualTo id1
            row[T1.aesBlob]?.toUtf8String() shouldBeEqualTo "aesBlob"
            row[T1.rc4Blob]?.toUtf8String() shouldBeEqualTo "rc4Blob"
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAO 방식으로 컬럼 암호화하기`(testDB: TestDB) {
        withTables(testDB, T1) {
            val e1 = E1.new {
                name = "Alice"
                aesBlob = "aesBlob".toUtf8Bytes()
                rc4Blob = "rc4Blob".toUtf8Bytes()
            }
            entityCache.clear()

            val loaded = E1.findById(e1.id)!!

            loaded shouldBeEqualTo e1
            loaded.name shouldBeEqualTo "Alice"
            loaded.aesBlob?.toUtf8String() shouldBeEqualTo "aesBlob"
            loaded.rc4Blob?.toUtf8String() shouldBeEqualTo "rc4Blob"
        }
    }
}
