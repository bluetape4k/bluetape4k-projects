package io.bluetape4k.workflow.api

/**
 * 코루틴 기반 비동기 작업 단위 인터페이스입니다.
 *
 * SAM 인터페이스이므로 람다로 간단히 생성할 수 있습니다.
 * 작업 이름이 필요한 경우 [NamedSuspendWork] 래퍼 또는 [SuspendWork] 팩토리 함수를 사용하세요.
 *
 * ```kotlin
 * // SAM 변환
 * val work = SuspendWork { ctx ->
 *     val data = httpClient.fetchAsync(ctx.get<String>("url")!!)
 *     ctx["data"] = data
 *     WorkReport.Success(ctx)
 * }
 *
 * // 이름 지정
 * val namedWork = SuspendWork("fetch-data") { ctx ->
 *     val data = httpClient.fetchAsync(ctx.get<String>("url")!!)
 *     ctx["data"] = data
 *     WorkReport.Success(ctx)
 * }
 * ```
 */
fun interface SuspendWork {
    /**
     * 작업을 비동기로 실행합니다.
     *
     * @param context 실행 컨텍스트
     * @return 실행 결과
     */
    suspend fun execute(context: WorkContext): WorkReport
}
