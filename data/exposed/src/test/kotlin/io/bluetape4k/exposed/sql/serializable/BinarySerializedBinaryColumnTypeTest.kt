package io.bluetape4k.exposed.sql.serializable

import io.bluetape4k.exposed.AbstractExposedTest
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.utils.runWithTables
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.junit.jupiter.api.Test
import java.io.Serializable

class BinarySerializedBinaryColumnTypeTest: AbstractExposedTest() {

    companion object: KLogging()

    private object T1: IntIdTable() {
        val zstdFury = binarySerializedBinary<Embeddable>("zstd_fury", 1024, BinarySerializers.ZstdFury).nullable()
        val zstdKryo = binarySerializedBinary<Embeddable>("zstd_kryo", 1024, BinarySerializers.ZstdKryo).nullable()
        val lz4Fury = binarySerializedBinary<Embeddable2>("lz4_fury", 1024, BinarySerializers.LZ4Fury).nullable()
        val lz4Kryo = binarySerializedBinary<Embeddable2>("lz4_kryo", 1024, BinarySerializers.LZ4Kryo).nullable()
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
    ): Serializable

    data class Embeddable2(
        val name: String,
        val age: Int,
        val address: String,
        val zipcode: String,
    ): Serializable


    @Test
    fun `Serializable Object 를 DB에 저장하고 로드한다`() {
        runWithTables(T1) {
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
