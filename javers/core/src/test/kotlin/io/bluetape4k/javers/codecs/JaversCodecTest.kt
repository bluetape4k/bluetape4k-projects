package io.bluetape4k.javers.codecs

import com.google.gson.JsonObject
import io.bluetape4k.javers.repository.jql.queryByInstanceId
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.atomicfu.atomic
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldNotBeNull
import org.javers.core.JaversBuilder
import org.javers.core.model.ShallowPhone
import org.javers.core.model.SnapshotEntity
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class JaversCodecTest {

    companion object: KLogging()

    private val javers = JaversBuilder.javers().build()

    private fun getStringCodecs() = listOf(
        JaversCodecs.String,
        JaversCodecs.GZipString,
        JaversCodecs.SnappyString,
        JaversCodecs.LZ4String,
        JaversCodecs.SnappyString,
        JaversCodecs.ZstdString,
    )

    private fun getBinaryCodecs() = listOf(
        JaversCodecs.Kryo,
        JaversCodecs.GZipKryo,
        JaversCodecs.SnappyKryo,
        JaversCodecs.LZ4Kryo,
        JaversCodecs.SnappyKryo,
        JaversCodecs.ZstdKryo,
    )

    private val idSeq = atomic(0)

    @ParameterizedTest(name = "GsonCodec={0}")
    @MethodSource("getStringCodecs")
    fun `string codec`(codec: JaversCodec<String>) {
        val entity = SnapshotEntity(idSeq.incrementAndGet()).apply { intProperty = 1 }
        javers.commit("a", entity)

        entity.shallowPhone = ShallowPhone(42)
        javers.commit("a", entity)

        entity.entityRef = SnapshotEntity(42)
        javers.commit("a", entity)

        val query = queryByInstanceId<SnapshotEntity>(idSeq.value)

        val snapshots = javers.findSnapshots(query)
        snapshots.forEach { log.debug { "snapshot=$it" } }
        snapshots.size shouldBeGreaterThan 1

        val jsonConverter = javers.jsonConverter

        snapshots.forEach { snapshot ->
            val jsonElement = jsonConverter.toJsonElement(snapshot) as JsonObject

            val encodedData = codec.encode(jsonElement)
            val decoded = codec.decode(encodedData)

            decoded.shouldNotBeNull()
            decoded.toString() shouldBeEqualTo jsonElement.toString()
        }
    }

    @ParameterizedTest(name = "GsonCodec={0}")
    @MethodSource("getBinaryCodecs")
    fun `binary codec`(codec: JaversCodec<ByteArray>) {
        val entity = SnapshotEntity(idSeq.incrementAndGet()).apply { intProperty = 1 }
        javers.commit("a", entity)

        entity.shallowPhone = ShallowPhone(43)
        javers.commit("a", entity)

        entity.entityRef = SnapshotEntity(43)
        javers.commit("a", entity)

        val query = queryByInstanceId<SnapshotEntity>(idSeq.value)

        val snapshots = javers.findSnapshots(query)
        snapshots.forEach { log.debug { "snapshot=$it" } }
        snapshots.size shouldBeGreaterThan 1

        val jsonConverter = javers.jsonConverter
        snapshots.forEach { snapshot ->
            val jsonElement = jsonConverter.toJsonElement(snapshot) as JsonObject

            val encodedData = codec.encode(jsonElement)
            val decoded = codec.decode(encodedData)

            decoded.shouldNotBeNull()
            val decodedMap = JaversGsonElementConverter.fromJsonObject(decoded)
            val jsonMap = JaversGsonElementConverter.fromJsonObject(jsonElement)
            decodedMap shouldContainSame jsonMap
        }
    }
}
