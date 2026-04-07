package io.bluetape4k.workflow.api

import io.bluetape4k.logging.KLogging
import kotlin.time.Duration

abstract class AbstractWorkflowTest {

    companion object: KLogging()

    protected val context = WorkContext()

    protected fun successWork(name: String = "success-work"): Work =
        Work(name) { ctx -> WorkReport.success(ctx) }

    protected fun failWork(name: String = "fail-work", msg: String = "실패"): Work =
        Work(name) { ctx -> WorkReport.failure(ctx, RuntimeException(msg)) }

    protected fun abortWork(name: String = "abort-work"): Work =
        Work(name) { ctx -> WorkReport.aborted(ctx, "중단") }

    /**
     * N ms 대기 후 성공하는 [Work]를 생성합니다. ANY 정책 테스트에 활용합니다.
     *
     * @param delayMs 대기 시간(밀리초)
     * @param name 작업 이름
     */
    protected fun delayedSuccessWork(delayMs: Long, name: String = "delayed-success"): Work =
        Work(name) { ctx ->
            Thread.sleep(delayMs)
            WorkReport.success(ctx)
        }

    protected fun successSuspendWork(name: String = "success-suspend-work"): SuspendWork =
        SuspendWork(name) { ctx -> WorkReport.success(ctx) }

    protected fun failSuspendWork(name: String = "fail-suspend-work", msg: String = "실패"): SuspendWork =
        SuspendWork(name) { ctx -> WorkReport.failure(ctx, RuntimeException(msg)) }

    protected fun abortSuspendWork(name: String = "abort-suspend-work"): SuspendWork =
        SuspendWork(name) { ctx -> WorkReport.aborted(ctx, "중단") }

    /**
     * 지정된 시간 delay 후 성공하는 [SuspendWork]를 생성합니다. ANY 정책 테스트에 활용합니다.
     *
     * @param delay 대기 시간
     * @param name 작업 이름
     */
    protected fun delayedSuccessSuspendWork(delay: Duration, name: String = "delayed-success"): SuspendWork =
        SuspendWork(name) { ctx ->
            kotlinx.coroutines.delay(delay)
            WorkReport.success(ctx)
        }
}
