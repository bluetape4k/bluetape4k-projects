package io.bluetape4k.coroutines.support

import io.bluetape4k.support.requireNotEmpty
import kotlinx.coroutines.Job
import kotlinx.coroutines.selects.select

/**
 * `Job` 트리를 들여쓰기 형태로 표준 출력에 출력합니다.
 *
 * ## 동작/계약
 * - 현재 `Job`을 출력한 뒤 모든 자식 `Job`에 대해 재귀적으로 같은 출력을 수행합니다.
 * - `offset`만큼 공백을 앞에 붙여 계층 구조를 표현합니다.
 * - 루트 호출(`offset == 0`)일 때 마지막에 빈 줄을 한 번 추가합니다.
 *
 * ```kotlin
 * job.printDebugTree()
 * // 출력 예: JobImpl{Active}@... / 자식 Job 들이 들여쓰기되어 출력됨
 * ```
 * @param offset 현재 노드 출력 시 사용할 좌측 공백 수입니다.
 */
fun Job.printDebugTree(offset: Int = 0) {
    println(" ".repeat(offset) + this)

    children.forEach {
        it.printDebugTree(offset + 2)
    }

    if (offset == 0) println()
}

/**
 * 전달된 `Job` 중 하나라도 완료될 때까지 대기합니다.
 *
 * ## 동작/계약
 * - `jobs.requireNotEmpty("jobs")`로 빈 입력을 허용하지 않습니다.
 * - `select`를 사용해 가장 먼저 완료되는 `Job`이 나타나면 즉시 반환합니다.
 * - 다른 `Job`은 취소하지 않고 그대로 둡니다.
 *
 * ```kotlin
 * joinAny(job1, job2, job3)
 * // result == 첫 완료 Job 감지 후 즉시 반환(Unit)
 * ```
 * @param jobs 대기 대상 `Job` 목록입니다. 최소 1개 이상이어야 합니다.
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
 * 컬렉션 확장 버전의 [joinAny]입니다.
 *
 * ## 동작/계약
 * - `requireNotEmpty("jobs")`로 빈 컬렉션 입력을 허용하지 않습니다.
 * - 컬렉션 내에서 가장 먼저 완료된 `Job`을 감지하면 반환합니다.
 * - 다른 `Job`은 취소하지 않습니다.
 *
 * ```kotlin
 * jobs.joinAny()
 * // result == 첫 완료 Job 감지 후 즉시 반환(Unit)
 * ```
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
 * 컬렉션에서 가장 먼저 완료된 `Job`을 기다린 뒤 나머지 `Job`을 취소합니다.
 *
 * ## 동작/계약
 * - `requireNotEmpty("jobs")`로 빈 컬렉션 입력을 허용하지 않습니다.
 * - `select`로 첫 완료 `Job`의 인덱스를 구한 뒤, 나머지 `Job`에 `cancel(null)`을 시도합니다.
 * - 개별 취소 실패는 `runCatching`으로 무시하고 계속 진행합니다.
 *
 * ```kotlin
 * jobs.joinAnyAndCancelOthers()
 * // result == 첫 완료 이후 나머지 Job 취소 시도 완료(Unit)
 * ```
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
