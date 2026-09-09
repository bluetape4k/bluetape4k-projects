package io.bluetape4k.openfga

import dev.openfga.sdk.api.OpenFgaApi
import dev.openfga.sdk.api.client.ApiResponse
import dev.openfga.sdk.api.configuration.ConfigurationOverride
import dev.openfga.sdk.api.model.BatchCheckRequest
import dev.openfga.sdk.api.model.BatchCheckResponse
import dev.openfga.sdk.api.model.CheckRequest
import dev.openfga.sdk.api.model.CheckResponse
import dev.openfga.sdk.api.model.ReadRequest
import dev.openfga.sdk.api.model.ReadResponse
import dev.openfga.sdk.api.model.Tuple
import dev.openfga.sdk.api.model.WriteRequest
import dev.openfga.sdk.constants.FgaConstants
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireInRange
import io.bluetape4k.support.requireLe
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.future.await

private object OpenFgaLog : KLoggingChannel()

/**
 * 공식 OpenFGA Check API를 취소 가능한 suspend 함수로 호출합니다.
 *
 * 요청 body는 복사하여 scope의 store와 authorization model을 적용하며, 호출자가 전달한 SDK 옵션은
 * 그대로 넘깁니다. 이 함수는 주입받은 [OpenFgaApi]를 닫지 않습니다.
 *
 * @param scope 요청 store와 authorization model 범위
 * @param request 공식 Check 요청. tuple, contextual tuple, consistency 등 모든 옵션을 보존합니다.
 * @param configurationOverride 공식 SDK의 요청별 timeout·header·retry 설정
 */
suspend fun OpenFgaApi.checkSuspending(
    scope: OpenFgaScope,
    request: CheckRequest,
    configurationOverride: ConfigurationOverride = ConfigurationOverride(),
): ApiResponse<CheckResponse> = openFgaCall("check") {
    check(scope.storeId, request.withAuthorizationModel(scope), configurationOverride).await()
}

/**
 * 공식 OpenFGA Batch Check API를 취소 가능한 suspend 함수로 호출합니다.
 *
 * SDK가 정의한 단일 batch 최대 항목 수를 넘는 요청은 서버 호출 전에 거부합니다. 응답의 항목별 allow와
 * 오류는 공식 [BatchCheckResponse] 그대로 반환합니다.
 */
suspend fun OpenFgaApi.batchCheckSuspending(
    scope: OpenFgaScope,
    request: BatchCheckRequest,
    configurationOverride: ConfigurationOverride = ConfigurationOverride(),
): ApiResponse<BatchCheckResponse> = openFgaCall("batchCheck") {
    request.getChecks().size.requireInRange(
        1,
        FgaConstants.CLIENT_MAX_BATCH_SIZE,
        "checks.size",
    )
    batchCheck(scope.storeId, request.withAuthorizationModel(scope), configurationOverride).await()
}

/**
 * 공식 OpenFGA Read API를 한 페이지 읽는 suspend 함수로 호출합니다.
 *
 * [pageSize]와 [continuationToken]을 지정하면 request의 해당 값을 복사해 덮어쓰며, 그 외의 tuple filter와
 * consistency 옵션은 보존합니다. Read API는 authorization model을 요구하지 않으므로 scope의 model ID는
 * 선택 사항입니다.
 */
suspend fun OpenFgaApi.readSuspending(
    scope: OpenFgaScope,
    request: ReadRequest = ReadRequest(),
    pageSize: Int? = request.getPageSize(),
    continuationToken: String? = request.getContinuationToken(),
    configurationOverride: ConfigurationOverride = ConfigurationOverride(),
): ApiResponse<ReadResponse> = openFgaCall("read") {
    pageSize?.requirePageSize()
    read(
        scope.storeId,
        request.withPage(pageSize, continuationToken),
        configurationOverride,
    ).await()
}

/**
 * OpenFGA tuple을 수집 시점에 한 페이지씩 읽는 cold [Flow]로 반환합니다.
 *
 * 요청은 collector가 실제로 시작할 때만 전송됩니다. 각 page의 tuple을 즉시 emit하고 다음 continuation
 * token을 요청하므로 느린 consumer의 backpressure를 유지합니다. 빈 token에서 정상 종료하며 cursor가
 * 진행하지 않거나 page 상한을 넘으면 [IllegalStateException]을 던집니다.
 *
 * @param scope 요청 store 범위
 * @param request tuple filter와 consistency를 담은 공식 Read 요청
 * @param pageSize page당 tuple 수, 1..100
 * @param maxPages 허용할 최대 page 수, 양수
 * @param configurationOverride 각 page 요청에 적용할 공식 SDK 설정
 */
fun OpenFgaApi.readTuplesFlow(
    scope: OpenFgaScope,
    request: ReadRequest = ReadRequest(),
    pageSize: Int = request.getPageSize() ?: DEFAULT_PAGE_SIZE,
    maxPages: Int = DEFAULT_MAX_PAGES,
    configurationOverride: ConfigurationOverride = ConfigurationOverride(),
): Flow<Tuple> {
    pageSize.requirePageSize()
    maxPages.requirePositiveNumber("maxPages")

    return flow {
        var continuationToken = request.getContinuationToken()
        var pages = 0

        while (true) {
            currentCoroutineContext().ensureActive()
            check(pages < maxPages) { "OpenFGA read exceeded maxPages" }

            val response = readSuspending(
                scope = scope,
                request = request,
                pageSize = pageSize,
                continuationToken = continuationToken,
                configurationOverride = configurationOverride,
            ).data
            pages++

            response.getTuples().orEmpty().forEach { tuple ->
                currentCoroutineContext().ensureActive()
                emit(tuple)
            }

            val nextToken = response.getContinuationToken()
            if (nextToken.isNullOrEmpty()) {
                break
            }
            check(nextToken != continuationToken) {
                "OpenFGA read continuation token did not advance"
            }
            continuationToken = nextToken
        }
    }
}

/**
 * 공식 OpenFGA Write API를 취소 가능한 suspend 함수로 호출합니다.
 *
 * body는 복사하여 scope의 store와 authorization model을 적용합니다. Future 취소는 local await를 중단하고
 * 반환된 CompletableFuture의 취소를 요청하지만 SDK transport·retry 중단이나 원격 write 롤백을 보장하지
 * 않습니다. 네트워크 기한은 호출자가 SDK request timeout/deadline으로 설정해야 하며, 기본 서버 제한인
 * writes와 deletes 합계 100개를 넘는 요청은 호출 전에 거부합니다. 호출자가 소유한 API 객체도 닫지 않습니다.
 */
suspend fun OpenFgaApi.writeSuspending(
    scope: OpenFgaScope,
    request: WriteRequest,
    configurationOverride: ConfigurationOverride = ConfigurationOverride(),
): ApiResponse<Any> = openFgaCall("write") {
    request.writeTupleCount.requireLe(MAX_WRITE_TUPLES_PER_REQUEST, "write tuple count")
    write(scope.storeId, request.withAuthorizationModel(scope), configurationOverride).await()
}

// 생성된 SDK 예외 유형을 모두 수용하되 status만 로그에 남기고 원래 예외를 다시 던집니다.
@Suppress("TooGenericExceptionCaught")
private suspend fun <T> openFgaCall(operation: String, call: suspend () -> T): T {
    currentCoroutineContext().ensureActive()
    return try {
        call().also {
            OpenFgaLog.log.info { "OpenFGA operation=$operation status=success" }
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        // 인증 정보, tuple, request payload, SDK 예외 원문을 로그에 포함하지 않습니다.
        OpenFgaLog.log.warn { "OpenFGA operation=$operation status=failure" }
        throw failure
    }
}

private fun Int.requirePageSize() {
    this.requireInRange(MIN_PAGE_SIZE, MAX_PAGE_SIZE, "pageSize")
}

private val WriteRequest.writeTupleCount: Int
    get() = (getWrites()?.getTupleKeys()?.size ?: 0) + (getDeletes()?.getTupleKeys()?.size ?: 0)

private const val DEFAULT_PAGE_SIZE = 100
private const val DEFAULT_MAX_PAGES = 10_000
private const val MIN_PAGE_SIZE = 1
private const val MAX_PAGE_SIZE = 100
private const val MAX_WRITE_TUPLES_PER_REQUEST = 100
