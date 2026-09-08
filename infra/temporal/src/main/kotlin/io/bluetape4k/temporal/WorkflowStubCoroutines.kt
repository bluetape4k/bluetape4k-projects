package io.bluetape4k.temporal

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.requireNotBlank
import io.temporal.api.common.v1.WorkflowExecution
import io.temporal.client.WorkflowStub
import io.temporal.client.getResultAsync
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.coroutineContext
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

private object TemporalLog : KLoggingChannel()

/**
 * [WorkflowStub]의 동기 start 호출을 IO dispatcher에서 실행합니다.
 *
 * 호출자의 코루틴이 취소되면 취소를 그대로 전파하며, 이미 시작된 workflow를 자동으로 취소하지
 * 않습니다.
 *
 * @param args workflow 시작 인자
 * @return 시작된 workflow execution
 */
suspend fun WorkflowStub.startSuspending(vararg args: Any?): WorkflowExecution =
    temporalBlocking("workflow.start") { start(*args) }

/**
 * [WorkflowStub]의 동기 signal 호출을 IO dispatcher에서 실행합니다.
 *
 * @param signalName signal handler 이름
 * @param args signal 인자
 */
suspend fun WorkflowStub.signalSuspending(signalName: String, vararg args: Any?) {
    val validSignalName = signalName.requireNotBlank("signalName")
    temporalBlocking("workflow.signal") { signal(validSignalName, *args) }
}

/**
 * [WorkflowStub]의 동기 query 호출을 IO dispatcher에서 실행합니다.
 *
 * 공식 Kotlin DSL과 동일하게 reified 타입의 Java generic type 정보를 전달합니다.
 *
 * @param queryType query handler 이름
 * @param args query 인자
 * @return query 결과
 */
suspend inline fun <reified T> WorkflowStub.querySuspending(
    queryType: String,
    vararg args: Any?,
): T {
    val validQueryType = queryType.requireNotBlank("queryType")
    return temporalBlocking("workflow.query") {
        query(validQueryType, T::class.java, typeOf<T>().javaType, *args)
    }
}

/**
 * workflow 결과 Future를 suspend 환경에서 기다립니다.
 *
 * 대기는 non-blocking Future await를 사용합니다. 로컬 coroutine 취소나 [kotlinx.coroutines.withTimeout]
 * 만으로 원격 workflow에 cancel 요청을 보내지 않습니다.
 *
 * @return workflow 결과
 */
suspend inline fun <reified T> WorkflowStub.awaitResult(): T {
    coroutineContext.ensureActive()
    return awaitTemporalResult(getResultAsync<T>())
}

/**
 * workflow에 명시적인 원격 cancel 요청을 보냅니다.
 *
 * [awaitResult]의 로컬 대기 취소와 달리 이 함수는 Temporal service에 cancel을 요청합니다.
 *
 * @param reason 취소 사유
 */
suspend fun WorkflowStub.cancelSuspending(reason: String? = null) {
    temporalBlocking("workflow.cancel") {
        if (reason == null) cancel() else cancel(reason)
    }
}

/**
 * workflow에 명시적인 원격 terminate 요청을 보냅니다.
 *
 * @param reason 종료 사유
 * @param details 종료 상세 정보
 */
suspend fun WorkflowStub.terminateSuspending(reason: String? = null, vararg details: Any?) {
    temporalBlocking("workflow.terminate") { terminate(reason, *details) }
}

// SDK 예외 원문은 로그에 남기지 않고 호출자에게 그대로 전달합니다.
@Suppress("TooGenericExceptionCaught")
@PublishedApi
internal suspend fun <T> temporalBlocking(operation: String, block: () -> T): T =
    withContext(Dispatchers.IO) {
        try {
            block().also { logTemporalStatus(operation, "success") }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logTemporalStatus(operation, "failure")
            throw e
        }
    }

// SDK 결과 예외 원문은 로그에 남기지 않고 호출자에게 그대로 전달합니다.
@Suppress("TooGenericExceptionCaught")
@PublishedApi
internal suspend fun <T> awaitTemporalResult(future: CompletableFuture<T>): T =
    try {
        future.await().also { logTemporalStatus("workflow.result", "success") }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logTemporalStatus("workflow.result", "failure")
        throw e
    }

private fun logTemporalStatus(operation: String, status: String) {
    TemporalLog.log.debug("Temporal operation={} status={}", operation, status)
}
