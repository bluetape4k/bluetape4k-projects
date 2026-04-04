package io.bluetape4k.rule.api

/**
 * 코루틴 기반의 Rule 실행부분을 표현하는 함수형 인터페이스입니다.
 *
 * ```kotlin
 * val action = SuspendAction { facts -> facts["result"] = fetchData() }
 * ```
 */
fun interface SuspendAction {

    /**
     * Rule의 [SuspendCondition]을 만족했을 때 비동기로 실행되는 작업
     *
     * @param facts Rule 실행에 필요한 데이터
     */
    suspend fun execute(facts: Facts)
}
