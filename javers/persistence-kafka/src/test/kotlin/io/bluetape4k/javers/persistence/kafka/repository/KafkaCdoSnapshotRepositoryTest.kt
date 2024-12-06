package io.bluetape4k.javers.persistence.kafka.repository

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.metamodel.`object`.SnapshotType
import org.javers.core.model.PrimitiveEntity
import org.javers.core.model.SnapshotEntity
import org.javers.core.repository.AbstractJaversCommitTest
import org.junit.jupiter.api.Test

/**
 * NOTE: **Redis나 MongoDB와 같이 테스트는 할 수 없고, snapshot 저장을 수행하는 테스트만 해야 한다**
 * Consumer 를 통한 검증을 수행해야 하는데, 귀찮아서 로그보고 확인했습니다. ㅋㅋ
 */
class KafkaCdoSnapshotRepositoryTest: AbstractJaversCommitTest() {

    companion object: KLogging()

    private val kafkaRepository by lazy {
        KafkaCdoSnapshotRepository(KafkaProvider.kafkaTemplate)
    }

    override fun newJavers(): Javers =
        JaversBuilder.javers()
            .registerJaversRepository(kafkaRepository)
            .build()

    val javers = newJavers()

    @Test
    fun `CommitMetadata에 현재 LocalDateTime과 Instant를 사용한다`() {
        val commit = javers.commit("author", SnapshotEntity(1))
        commit.snapshots.size shouldBeEqualTo 1
        val snapshot = commit.snapshots.first()
        snapshot.type shouldBeEqualTo SnapshotType.INITIAL
    }

    @Test
    fun `다양한 Primitive 수형 변화를 Commit한다`() {
        // GIVEN
        val s = PrimitiveEntity("1")

        // WHEN
        javers.commit("author", s)

        s.intField = 10
        s.longField = 10L
        s.doubleField = 1.1
        s.floatField = 1.1F
        s.charField = 'c'
        s.byteField = 10.toByte()
        s.shortField = 10.toShort()
        s.booleanField = true
        s.IntegerField = 10
        s.LongField = 10
        s.DoubleField = 1.1
        s.FloatField = 1.1F
        s.CharField = 'c'
        s.ByteField = 10.toByte()
        s.ShortField = 10.toShort()
        s.BooleanField = true

        val commit = javers.commit("author", s)

        val snapshot = commit.snapshots.first()

        snapshot.state.getPropertyValue("floatField") shouldBeEqualTo 1.1F
        snapshot.state.getPropertyValue("LongField") shouldBeEqualTo 10L
    }
}
