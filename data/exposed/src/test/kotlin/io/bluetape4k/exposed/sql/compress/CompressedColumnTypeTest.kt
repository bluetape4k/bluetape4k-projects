package io.bluetape4k.exposed.sql.compress

import io.bluetape4k.exposed.AbstractExposedTest
import io.bluetape4k.exposed.utils.runWithTables
import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.junit.jupiter.api.Test

class CompressedColumnTypeTest: AbstractExposedTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    private object T1: IntIdTable() {
        val lz4Data = compressedBlob("lz4_data", Compressors.LZ4).nullable()
        val snappyData = compressedBlob("snappy_data", Compressors.Snappy).nullable()
        val zstdData = compressedBlob("zstd_data", Compressors.Zstd).nullable()
    }

    class E1(id: EntityID<Int>): IntEntity(id) {
        companion object: EntityClass<Int, E1>(T1)

        var lz4Data by T1.lz4Data
        var snappyData by T1.snappyData
        var zstdData by T1.zstdData
    }

    @Test
    fun `ByteArray 를 압축하여 저장합니다`() {
        val text = Fakers.randomString(128, 256)

        runWithTables(T1) {
            val e1 = E1.new {
                lz4Data = text
                snappyData = text
                zstdData = text
            }
            entityCache.clear()

            val loaded = E1.findById(e1.id)!!

            loaded.lz4Data shouldBeEqualTo text
            loaded.snappyData shouldBeEqualTo text
            loaded.zstdData shouldBeEqualTo text
        }
    }
}
