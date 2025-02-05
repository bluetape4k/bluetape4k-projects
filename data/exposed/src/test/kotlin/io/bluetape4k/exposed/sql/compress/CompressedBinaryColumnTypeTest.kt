package io.bluetape4k.exposed.sql.compress

import io.bluetape4k.exposed.AbstractExposedTest
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.utils.runWithTables
import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.junit.jupiter.api.Test

class CompressedBinaryColumnTypeTest: AbstractExposedTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    private object T1: IntIdTable() {
        val lz4Data = compressedBinary("lz4_data", 4096, Compressors.LZ4).nullable()
        val snappyData = compressedBinary("snappy_data", 4096, Compressors.Snappy).nullable()
        val zstdData = compressedBinary("zstd_data", 4096, Compressors.Zstd).nullable()
    }

    class E1(id: EntityID<Int>): IntEntity(id) {
        companion object: EntityClass<Int, E1>(T1)

        var lz4Data by T1.lz4Data
        var snappyData by T1.snappyData
        var zstdData by T1.zstdData

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = id.hashCode()
        override fun toString(): String = "E1(id=$id)"
    }

    @Test
    fun `ByteArray 를 압축하여 저장합니다`() {
        val text = Fakers.randomString(2048, 4096)
        val data = text.toUtf8Bytes()

        runWithTables(T1) {
            val e1 = E1.new {
                lz4Data = data
                snappyData = data
                zstdData = data
            }
            entityCache.clear()

            val loaded = E1.findById(e1.id)!!

            loaded.lz4Data!!.toUtf8String() shouldBeEqualTo text
            loaded.snappyData!!.toUtf8String() shouldBeEqualTo text
            loaded.zstdData!!.toUtf8String() shouldBeEqualTo text
        }
    }
}
