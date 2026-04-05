package io.bluetape4k.workflow.api

/**
 * 동기 작업 단위 인터페이스입니다.
 *
 * SAM 인터페이스이므로 람다로 간단히 생성할 수 있습니다.
 * 작업 이름이 필요한 경우 [NamedWork] 래퍼 또는 [Work] 팩토리 함수를 사용하세요.
 *
 * ```kotlin
 * // SAM 변환 — 이름 없이 간단히 생성
 * val work = Work { ctx ->
 *     ctx["result"] = compute()
 *     WorkReport.Success(ctx)
 * }
 *
 * // 이름 지정 — 팩토리 함수 사용
 * val namedWork = Work("validate-order") { ctx ->
 *     ctx["valid"] = true
 *     WorkReport.Success(ctx)
 * }
 * ```
 */
fun interface Work {
    /**
     * 작업을 실행합니다.
     *
     * @param context 실행 컨텍스트
     * @return 실행 결과
     */
    fun execute(context: WorkContext): WorkReport
}
