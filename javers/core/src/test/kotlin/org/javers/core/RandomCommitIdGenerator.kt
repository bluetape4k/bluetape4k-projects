package org.javers.core

import io.bluetape4k.idgenerators.snowflake.Snowflakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.atomicfu.locks.ReentrantLock
import org.javers.core.commit.CommitId
import java.util.function.Supplier
import kotlin.concurrent.withLock

class RandomCommitIdGenerator: Supplier<CommitId> {

    companion object: KLogging()

    private val commits = hashMapOf<CommitId, Int>()
    private val snowflake = Snowflakers.Global
    private var counter = 0
    private val lock = ReentrantLock()

    fun getSeq(commitId: CommitId): Int =
        commits[commitId] ?: throw NoSuchElementException("Not found commitId[$commitId]")

    override fun get(): CommitId {
        lock.withLock {
            counter++
            val next = CommitId(snowflake.nextId(), 0)
            commits[next] = counter

            log.debug { "Generate random CommitId. next commitId=$next, counter=$counter" }
            return next
        }
    }
}
