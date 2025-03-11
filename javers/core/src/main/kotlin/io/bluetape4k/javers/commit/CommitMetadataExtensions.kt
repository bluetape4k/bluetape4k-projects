package io.bluetape4k.javers.commit

import org.javers.core.commit.CommitId
import org.javers.core.commit.CommitMetadata

val CommitId.version: Pair<Long, Int> get() = Pair(majorId, minorId)

operator fun CommitMetadata.compareTo(that: CommitMetadata): Int =
    this.id.compareTo(that.id)

val CommitMetadata.commitTimestamp: Long get() = commitDateInstant.toEpochMilli()
