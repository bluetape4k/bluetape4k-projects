package io.bluetape4k.exposed.sql.serializable

import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.dao.flushCache
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.Serializable

class BinarySerializedBlobColumTypeTest: AbstractExposedTest() {

    companion object: KLogging()

    private object T1: IntIdTable() {
        val zstdFury = binarySerializedBlob<Embeddable>("zstd_fury", BinarySerializers.ZstdFury).nullable()
        val zstdKryo = binarySerializedBlob<Embeddable>("zstd_kryo", BinarySerializers.ZstdKryo).nullable()
        val lz4Fury = binarySerializedBlob<Embeddable2>("lz4_fury", BinarySerializers.LZ4Fury).nullable()
        val lz4Kryo = binarySerializedBlob<Embeddable2>("lz4_kryo", BinarySerializers.LZ4Kryo).nullable()
    }

    class E1(id: EntityID<Int>): IntEntity(id) {
        companion object: EntityClass<Int, E1>(T1)

        var zstdFury by T1.zstdFury
        var zstdKryo by T1.zstdKryo

        var lz4Fury by T1.lz4Fury
        var lz4Kryo by T1.lz4Kryo

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = id.hashCode()
        override fun toString(): String = "E1(id=$id)"
    }

    data class Embeddable(
        val name: String,
        val age: Int,
        val address: String,
    ): Serializable {
        var bytes: ByteArray = Fakers.faker.image().base64JPEG().toUtf8Bytes()
    }

    data class Embeddable2(
        val name: String,
        val age: Int,
        val address: String,
        val zipcode: String,
    ): Serializable {
        var bytes: ByteArray = Fakers.faker.image().base64JPEG().toUtf8Bytes()
        var bytes2: ByteArray = Fakers.faker.image().base64PNG().toUtf8Bytes()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DSL 방식으로 Serializable Object 를 DB에 저장하고 로드한다`(testDB: TestDB) {
        withTables(testDB, T1) {
            val embedded = Embeddable("Alice", 20, "Seoul")
            val embedded2 = Embeddable2("Alice", 20, "Seoul", "12914")

            val id1 = T1.insertAndGetId {
                it[zstdFury] = embedded
                it[zstdKryo] = embedded

                it[lz4Fury] = embedded2
                it[lz4Kryo] = embedded2
            }
            flushCache()

            val row = T1.selectAll().where { T1.id eq id1 }.single()

            row[T1.id] shouldBeEqualTo id1

            row[T1.zstdKryo] shouldBeEqualTo embedded
            row[T1.zstdFury] shouldBeEqualTo embedded

            row[T1.lz4Kryo] shouldBeEqualTo embedded2
            row[T1.lz4Fury] shouldBeEqualTo embedded2
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAO 방식으로 Serializable Object 를 DB에 저장하고 로드한다`(testDB: TestDB) {
        withTables(testDB, T1) {
            val embedded = Embeddable("Alice", 20, "Seoul")
            val embedded2 = Embeddable2("Alice", 20, "Seoul", "12914")
            val e1 = E1.new {
                zstdFury = embedded
                zstdKryo = embedded

                lz4Fury = embedded2
                lz4Kryo = embedded2
            }
            entityCache.clear()

            val loaded = E1.findById(e1.id)!!

            loaded shouldBeEqualTo e1

            loaded.zstdKryo shouldBeEqualTo embedded
            loaded.zstdFury shouldBeEqualTo embedded

            loaded.lz4Kryo shouldBeEqualTo embedded2
            loaded.lz4Fury shouldBeEqualTo embedded2
        }
    }
}
