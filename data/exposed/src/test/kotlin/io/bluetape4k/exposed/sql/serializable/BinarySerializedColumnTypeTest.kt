package io.bluetape4k.exposed.sql.serializable

import io.bluetape4k.exposed.AbstractExposedTest
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

class BinarySerializedColumnTypeTest: AbstractExposedTest() {

    companion object: KLogging()

    private object T1: IntIdTable() {
        val kryoData = binarySerialized<Embedded>("kryo_data", BinarySerializers.ZstdKryo).nullable()
        val furyData = binarySerialized<Embedded>("fury_data", BinarySerializers.ZstdFury).nullable()
    }

    class E1(id: EntityID<Int>): IntEntity(id) {
        companion object: EntityClass<Int, E1>(T1)

        var kryoData by T1.kryoData
        var furyData by T1.furyData
    }

    data class Embedded(
        val name: String,
        val age: Int,
        val address: String,
    ): Serializable

    @Test
    fun `Serializable Object 를 DB에 저장하고 로드한다`() {
        runWithTables(T1) {
            val embedded = Embedded("Alice", 20, "Seoul")
            val e1 = E1.new {
                kryoData = embedded
                furyData = embedded
            }
            entityCache.clear()

            val loaded = E1.findById(e1.id)!!
            loaded.kryoData shouldBeEqualTo embedded
            loaded.furyData shouldBeEqualTo embedded
        }
    }
}
