package io.bluetape4k.javers.commit

import io.bluetape4k.idgenerators.snowflake.Snowflake
import io.bluetape4k.idgenerators.snowflake.Snowflakers
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.reentrantLock
import org.javers.core.commit.CommitId
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier
import kotlin.concurrent.withLock

/**
 * [Snowflake] 알고리즘 기반의 [CommitId] 생성기.
 *
 * ## 동작/계약
 * - [get] 호출마다 Snowflake ID를 majorId로, minorId=0인 고유 [CommitId]를 생성한다
 * - 생성된 [CommitId]별 순서 번호를 [getSeq]로 조회할 수 있다
 * - 존재하지 않는 [CommitId]로 [getSeq] 호출 시 [NoSuchElementException]을 던진다
 * - [get]은 lock으로 스레드 안전성을 보장한다
 *
 * ```kotlin
 * val generator = SnowflakeCommitIdGenerator()
 * val commitId = generator.get()
 * val seq = generator.getSeq(commitId)
 * // seq == 1
 * ```
 *
 * @property snowflake ID 생성에 사용할 [Snowflake] 인스턴스
 */
class SnowflakeCommitIdGenerator(
    private val snowflake: Snowflake = Snowflakers.Default,
): Supplier<CommitId> {

    private val commits = ConcurrentHashMap<CommitId, Int>()
    private val counter = atomic(0)
    private val lock = reentrantLock()

    /**
     * 지정한 [commitId]의 순서 번호를 반환한다.
     *
     * @throws NoSuchElementException [commitId]가 생성되지 않은 경우
     */
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
