package io.bluetape4k.workflow.api

/**
 * 이름을 가진 [SuspendWork] 래퍼입니다.
 *
 * [SuspendWork]가 `fun interface`이므로 default property를 가질 수 없습니다.
 * 작업 이름이 필요한 경우(로깅, 디버깅 등) 이 클래스를 사용하세요.
 *
 * ```kotlin
 * val work = NamedSuspendWork("fetch-data") { ctx ->
 *     val data = httpClient.fetchAsync(ctx.get<String>("url")!!)
 *     ctx["data"] = data
 *     WorkReport.Success(ctx)
 * }
 * println(work.name) // "fetch-data"
 * ```
 *
 * @property name 작업 이름
 */
class NamedSuspendWork(
    val name: String,
    private val delegate: SuspendWork,
): SuspendWork {
    override suspend fun execute(context: WorkContext): WorkReport = delegate.execute(context)
    override fun toString(): String = "NamedSuspendWork($name)"
}

/**
 * 이름 지정 [SuspendWork] 팩토리 함수입니다.
 *
 * ```kotlin
 * val work = SuspendWork("fetch-data") { ctx ->
 *     val data = httpClient.fetchAsync(ctx.get<String>("url")!!)
 *     ctx["data"] = data
 *     WorkReport.Success(ctx)
 * }
 * ```
 *
 * @param name 작업 이름
 * @param block 작업 실행 로직
 * @return 이름이 부여된 [NamedSuspendWork]
 */
fun SuspendWork(name: String, block: suspend (WorkContext) -> WorkReport): NamedSuspendWork =
    NamedSuspendWork(name, block)
