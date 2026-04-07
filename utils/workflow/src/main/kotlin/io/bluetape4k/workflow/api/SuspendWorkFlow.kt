package io.bluetape4k.workflow.api

/**
 * 코루틴 워크플로 인터페이스입니다.
 *
 * [SuspendWork]를 확장하여 코루틴 워크플로도 합성(composite)할 수 있습니다.
 *
 * ```kotlin
 * class MySuspendWorkFlow(private val works: List<SuspendWork>) : SuspendWorkFlow {
 *     override suspend fun execute(context: WorkContext): WorkReport {
 *         works.forEach { it.execute(context) }
 *         return WorkReport.success(context)
 *     }
 * }
 * ```
 */
interface SuspendWorkFlow: SuspendWork
