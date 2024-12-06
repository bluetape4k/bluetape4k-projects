package io.bluetape4k.javers

import io.bluetape4k.javers.repository.caffeine.CaffeineCdoSnapshotRepository
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.model.SnapshotEntity
import org.javers.core.repository.AbstractJaversCommitTest
import org.junit.jupiter.api.Test

class SnapshotToShadowTest: AbstractJaversCommitTest() {

    override fun newJavers(): Javers =
        JaversBuilder.javers()
            .registerJaversRepository(CaffeineCdoSnapshotRepository())
            .build()

    val javers by lazy { newJavers() }

    @Test
    fun `convert snapshot to shadow`() {
        val entity = SnapshotEntity(1).apply { intProperty = 1 }
        // 엔티티의 변경을 snapshot 을 생성한다
        val snapshot = javers.commit("a", entity).snapshots.first()

        // snapshot 으로부터 원본 entity를 가지고 올 수 있도록 해주는 Shadow을 만든다
        val shadow = javers.getShadow<SnapshotEntity>(snapshot)

        shadow.shouldNotBeNull()
        shadow.get() shouldBeEqualTo entity

        repeat(100) {
            javers.getShadow<SnapshotEntity>(snapshot).get() shouldBeEqualTo entity
        }
    }
}
