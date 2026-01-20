package io.bluetape4k.javers.commit

import io.bluetape4k.idgenerators.snowflake.Snowflake
import io.bluetape4k.idgenerators.snowflake.Snowflakers
import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock
import org.javers.core.commit.CommitId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier

class SnowflakeCommitIdGenerator(
    private val snowflake: Snowflake = Snowflakers.Default,
): Supplier<CommitId> {

    private val commits = ConcurrentHashMap<CommitId, Int>()
    private val counter = AtomicInteger(0)
    private val lock = ReentrantLock()

    fun getSeq(commitId: CommitId): Int =
        commits[commitId] ?: throw NoSuchElementException("Not found commitId [$commitId]")

    override fun get(): CommitId = lock.withLock {
        counter.incrementAndGet()
        val next = CommitId(nextId(), 0)
        commits[next] = counter.get()
        next
    }

    private fun nextId(): Long = snowflake.nextId()
}
