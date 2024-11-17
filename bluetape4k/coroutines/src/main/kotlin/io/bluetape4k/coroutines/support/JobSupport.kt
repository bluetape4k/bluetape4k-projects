package io.bluetape4k.coroutines.support

import io.bluetape4k.support.requireNotEmpty
import kotlinx.coroutines.Job
import kotlinx.coroutines.selects.select

/**
 * Job 정보를 계층적으로 표현한 트리를 출력합니다. 테스트 시에 사용하기 위해 제작되었습니다.
 *
 * ```
 * job.printDebugTree()  // 출력 예시
 * ```
 */
fun Job.printDebugTree(offset: Int = 0) {
    println(" ".repeat(offset) + this)

    children.forEach {
        it.printDebugTree(offset + 2)
    }

    if (offset == 0) println()
}

/**
 * Job들 중 먼저 완료되는 것이 나온다면 끝낸다. (나머지 Job들은 취소하지 않음)
 *
 * ```
 * joinAny(job1, job2, job3)
 * ```
 *
 * @param jobs Job들
 * @throws IllegalArgumentException [jobs] 가 비어있을 때
 */
suspend fun joinAny(vararg jobs: Job) {
    jobs.requireNotEmpty("jobs")

    select {
        jobs.forEach {
            it.onJoin { }
        }
    }
}

/**
 * [Job] 들 중 먼저 완료되는 것이 나온다면 끝낸다. (나머지 Job들은 취소하지 않음)
 *
 * ```
 * val jobs = listOf(job1, job2, job3)
 * jobs.joinAny()
 * ```
 * @receiver [Job] 리스트
 * @throws IllegalArgumentException Collection<Job> 이 비어있을 때
 */
suspend fun Collection<Job>.joinAny() {
    requireNotEmpty("jobs")

    select {
        forEach {
            it.onJoin { }
        }
    }
}

/**
 * 첫번째 완료된 Job을 기다리고, 나머지 Job들을 취소합니다.
 *
 * ```
 * val jobs = listOf(job1, job2, job3)
 * jobs.joinAnyAndCancelOthers()
 *
 * yield()
 *
 * job1.isCompleted shouldBe true
 * job2.isCancelled shouldBe true
 * job3.isCancelled shouldBe true
 * ```
 *
 * @receiver [Job] 리스트
 * @throws IllegalArgumentException Collection<Job> 이 비어있을 때
 */
suspend fun Collection<Job>.joinAnyAndCancelOthers() {
    requireNotEmpty("jobs")

    // 처음으로 완료된 Job의 인덱스를 가져옵니다.
    val firstCompletedIndex: Int = select {
        forEachIndexed { index, job ->
            job.onJoin { index }
        }
    }

    // 처음으로 완료된 Job을 제외하고 나머지 Job들을 취소합니다.
    filterIndexed { index, _ -> index != firstCompletedIndex }
        .forEach {
            runCatching { it.cancel(null) }
        }
}
