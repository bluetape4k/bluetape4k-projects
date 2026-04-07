package io.bluetape4k.workflow.api

/**
 * 이름을 가진 [Work] 래퍼입니다.
 *
 * [Work]가 `fun interface`이므로 default property를 가질 수 없습니다.
 * 작업 이름이 필요한 경우(로깅, 디버깅 등) 이 클래스를 사용하세요.
 *
 * ```kotlin
 * val work = NamedWork("validate") { ctx ->
 *     ctx["valid"] = true
 *     WorkReport.Success(ctx)
 * }
 * println(work.name) // "validate"
 * ```
 *
 * @property name 작업 이름
 */
class NamedWork(
    val name: String,
    private val delegate: Work,
): Work {
    override fun execute(context: WorkContext): WorkReport = delegate.execute(context)
    override fun toString(): String = "NamedWork($name)"
}

/**
 * 이름 지정 [Work] 팩토리 함수입니다.
 *
 * ```kotlin
 * val work = Work("validate-order") { ctx ->
 *     ctx["valid"] = true
 *     WorkReport.Success(ctx)
 * }
 * ```
 *
 * @param name 작업 이름
 * @param block 작업 실행 로직
 * @return 이름이 부여된 [NamedWork]
 */
fun Work(name: String, block: (WorkContext) -> WorkReport): NamedWork =
    NamedWork(name, block)
