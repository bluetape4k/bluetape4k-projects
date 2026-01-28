package io.bluetape4k.javers.commit

import io.bluetape4k.idgenerators.snowflake.Snowflake
import io.bluetape4k.idgenerators.snowflake.Snowflakers
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.reentrantLock
import org.javers.core.commit.CommitId
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier
import kotlin.concurrent.withLock

class SnowflakeCommitIdGenerator(
    private val snowflake: Snowflake = Snowflakers.Default,
): Supplier<CommitId> {

    private val commits = ConcurrentHashMap<CommitId, Int>()
    private val counter = atomic(0)
    private val lock = reentrantLock()

    fun getSeq(commitId: CommitId): Int =
        commits[commitId] ?: throw NoSuchElementException("Not found commitId [$commitId]")

    override fun get(): CommitId = lock.withLock {
        counter.incrementAndGet()
        val next = CommitId(nextId(), 0)
        commits[next] = counter.value
        next
    }

    private fun nextId(): Long = snowflake.nextId()
}
