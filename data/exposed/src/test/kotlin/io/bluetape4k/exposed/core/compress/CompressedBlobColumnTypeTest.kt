package io.bluetape4k.exposed.core.compress

import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class CompressedBlobColumnTypeTest: AbstractExposedTest() {

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

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = id.hashCode()
        override fun toString(): String = "E1(id=$id)"
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DSL 방식으로 ByteArray 를 압축하여 저장합니다`(testDB: TestDB) {
        val text = Fakers.randomString(1024, 2048)
        val data = text.toUtf8Bytes()

        withTables(testDB, T1) {
            val id = T1.insertAndGetId {
                it[lz4Data] = data
                it[snappyData] = data
                it[zstdData] = data
            }

            val row = T1.selectAll().where { T1.id eq id }.single()

            row[T1.id] shouldBeEqualTo id
            row[T1.lz4Data]?.toUtf8String() shouldBeEqualTo text
            row[T1.snappyData]?.toUtf8String() shouldBeEqualTo text
            row[T1.zstdData]?.toUtf8String() shouldBeEqualTo text
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAO 방식으로 ByteArray 를 압축하여 저장합니다`(testDB: TestDB) {
        val text = Fakers.randomString(1024, 2048)
        val data = text.toUtf8Bytes()

        withTables(testDB, T1) {
            val e1 = E1.new {
                lz4Data = data
                snappyData = data
                zstdData = data
            }
            entityCache.clear()

            val loaded = E1.findById(e1.id)!!

            loaded shouldBeEqualTo e1
            loaded.lz4Data?.toUtf8String() shouldBeEqualTo text
            loaded.snappyData?.toUtf8String() shouldBeEqualTo text
            loaded.zstdData?.toUtf8String() shouldBeEqualTo text
        }
    }
}
