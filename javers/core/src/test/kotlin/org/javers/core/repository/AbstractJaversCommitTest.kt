package org.javers.core.repository

import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.collections.eclipse.unifiedMapOf
import io.bluetape4k.collections.eclipse.unifiedSetOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.javers.core.Javers
import org.javers.core.model.CategoryC
import org.javers.core.model.PhoneWithShallowCategory
import org.javers.core.model.ShallowPhone
import org.javers.core.model.SnapshotEntity
import org.junit.jupiter.api.Test

abstract class AbstractJaversCommitTest {

    companion object: KLogging()

    abstract fun newJavers(): Javers

    @Test
    fun `ShallowReferenceType 엔티티의 snapshot은 commit하지 않습니다`() {
        val javers = newJavers()
        val reference = ShallowPhone(1L, "123", CategoryC(1, "some"))
        val entity = SnapshotEntity(id = 1).apply {
            shallowPhone = reference
            shallowPhones = unifiedSetOf(reference)
            shallowPhonesList = fastListOf(reference)
            shallowPhonesMap = unifiedMapOf("key" to reference)
        }

        // WHEN
        var commit = javers.commit("", entity)

        // THEN
        commit.snapshots.forEach { log.debug { it } }
        commit.snapshots.size shouldBeEqualTo 1

        reference.number = "other"

        commit = javers.commit("", entity)

        commit.snapshots.forEach { log.debug { it } }
        commit.snapshots.isEmpty()
    }

    @Test
    fun `@ShallowReference가 지정된 property의 변화는 snapshot으로 commit되지 않습니다`() {
        val javers = newJavers()
        val entity = PhoneWithShallowCategory(1).apply {
            shallowCategory = CategoryC(1, "old shallow")
        }

        var commit = javers.commit("", entity)

        commit.snapshots.forEach { log.debug { it } }
        commit.snapshots.size shouldBeEqualTo 1

        entity.shallowCategory?.name = "new shallow"

        commit = javers.commit("", entity)

        commit.snapshots.forEach { log.debug { it } }
        commit.snapshots.isEmpty()
    }
}
