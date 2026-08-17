package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.support.requirePositiveNumber
import java.io.InvalidObjectException
import java.io.ObjectInputStream
import java.io.Serializable

/** `getAll`의 back hit를 front에 저장할지 결정하는 정책입니다. */
sealed interface BulkFrontPopulationPolicy: Serializable {

    /** back hit를 반환하지만 front에는 저장하지 않습니다. */
    data object BypassFront: BulkFrontPopulationPolicy {
        private const val serialVersionUID: Long = 1L
    }

    /**
     * 실제 back hit 수가 [maximumEntryCount] 이하일 때만 batch 전체를 front에 저장합니다.
     *
     * @property maximumEntryCount front에 저장할 수 있는 최대 back hit 수
     */
    data class PopulateIfAtMost(
        val maximumEntryCount: Int,
    ): BulkFrontPopulationPolicy {
        init {
            maximumEntryCount.requirePositiveNumber("maximumEntryCount")
        }

        /** Java serialization이 constructor 검증을 우회하지 못하도록 동일한 불변식을 복원합니다. */
        private fun readObject(input: ObjectInputStream) {
            input.defaultReadObject()
            if (maximumEntryCount <= 0) {
                throw InvalidObjectException("Invalid bulk front population limit")
            }
        }

        companion object {
            private const val serialVersionUID: Long = 1L
        }
    }
}

internal fun BulkFrontPopulationPolicy.shouldPopulate(backValueCount: Int): Boolean = when (this) {
    BulkFrontPopulationPolicy.BypassFront -> false
    is BulkFrontPopulationPolicy.PopulateIfAtMost -> backValueCount <= maximumEntryCount
}
