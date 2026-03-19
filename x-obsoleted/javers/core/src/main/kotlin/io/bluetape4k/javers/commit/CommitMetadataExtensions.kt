package io.bluetape4k.javers.commit

import org.javers.core.commit.CommitId
import org.javers.core.commit.CommitMetadata

/**
 * [CommitId]의 majorId와 minorId를 [Pair]로 반환한다.
 *
 * ```kotlin
 * val (major, minor) = commitId.version
 * // major == commitId.majorId, minor == commitId.minorId
 * ```
 */
val CommitId.version: Pair<Long, Int> get() = Pair(majorId, minorId)

/**
 * 두 [CommitMetadata]를 커밋 ID 기준으로 비교한다.
 */
operator fun CommitMetadata.compareTo(that: CommitMetadata): Int =
    this.id.compareTo(that.id)

/**
 * 커밋 시각을 epoch 밀리초로 반환한다.
 *
 * ```kotlin
 * val ts = commitMetadata.commitTimestamp
 * // ts > 0
 * ```
 */
val CommitMetadata.commitTimestamp: Long get() = commitDateInstant.toEpochMilli()
