package io.bluetape4k.workflow.api

/**
 * 워크플로 인터페이스입니다.
 *
 * [Work]를 확장하여 워크플로 자체도 작업 단위로 합성(composite)할 수 있습니다.
 *
 * ```kotlin
 * class MyWorkFlow(private val works: List<Work>) : WorkFlow {
 *     override fun execute(context: WorkContext): WorkReport {
 *         works.forEach { it.execute(context) }
 *         return WorkReport.success(context)
 *     }
 * }
 * ```
 */
interface WorkFlow: Work
