package io.bluetape4k.qdrant

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireInRange
import io.bluetape4k.support.requirePositiveNumber
import io.qdrant.client.QdrantClient
import io.qdrant.client.grpc.Points.DeletePoints
import io.qdrant.client.grpc.Points.PointStruct
import io.qdrant.client.grpc.Points.QueryPoints
import io.qdrant.client.grpc.Points.RetrievedPoint
import io.qdrant.client.grpc.Points.ScoredPoint
import io.qdrant.client.grpc.Points.ScrollPoints
import io.qdrant.client.grpc.Points.UpdateResult
import io.qdrant.client.grpc.Points.UpsertPoints
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Duration
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private object QdrantLog: KLoggingChannel()
private const val MAX_SCROLL_LIMIT = 1000
private const val DEFAULT_BATCH_BYTES = 4_194_304

/**
 * 공식 query 요청과 RPC 기한을 그대로 전달하고 결과를 중단 가능하게 기다립니다.
 * 취소 시 요청 Future를 취소하며, 호출자가 소유한 클라이언트와 채널은 닫지 않습니다.
 */
suspend fun QdrantClient.querySuspending(request: QueryPoints, timeout: Duration? = null): List<ScoredPoint> =
    qdrantCall("query") { queryAsync(request, timeout) }

/** 포인트를 저장합니다. 대기 취소는 서버에 이미 반영된 쓰기를 되돌리지 않습니다. */
suspend fun QdrantClient.upsertSuspending(request: UpsertPoints, timeout: Duration? = null): UpdateResult =
    qdrantCall("upsert") { upsertAsync(request, timeout) }

/** 공식 ID·필터 선택 요청을 보존하여 포인트를 삭제합니다. */
suspend fun QdrantClient.deleteSuspending(request: DeletePoints, timeout: Duration? = null): UpdateResult =
    qdrantCall("delete") { deleteAsync(request, timeout) }

/**
 * 수집할 때마다 독립된 cursor로 한 페이지씩 읽는 cold Flow입니다.
 * 소비자가 중단하면 다음 페이지를 요청하지 않습니다. 페이지 응답의 바이트 상한은
 * 호출자가 설정한 gRPC 채널의 최대 수신 크기에 따릅니다.
 *
 * @param request limit가 1..1000인 공식 요청. 필터·payload·vector 옵션을 보존합니다.
 * @param timeout 각 페이지 RPC에 적용할 기한
 * @param maxPages 반복 cursor의 장주기를 제한하는 최대 페이지 수
 */
fun QdrantClient.scrollAsFlow(
    request: ScrollPoints,
    timeout: Duration? = null,
    maxPages: Int = 10_000,
): Flow<RetrievedPoint> {
    request.limit.requireInRange(1, MAX_SCROLL_LIMIT, "request.limit")
    maxPages.requirePositiveNumber("maxPages")
    return flow {
        var pageRequest = request
        var pages = 0
        while (true) {
            val page = qdrantCall("scroll") { scrollAsync(pageRequest, timeout) }
            page.resultList.forEach { emit(it) }
            if (!page.hasNextPageOffset()) break
            check(!pageRequest.hasOffset() || page.nextPageOffset != pageRequest.offset) {
                "Qdrant scroll cursor did not advance"
            }
            check(++pages < maxPages) { "Qdrant scroll exceeded maxPages" }
            pageRequest = request.toBuilder().setOffset(page.nextPageOffset).build()
        }
    }
}

/**
 * 입력을 항목 수와 직렬화된 전체 요청 크기로 제한하여 순차 저장합니다.
 * 각 성공 응답을 즉시 전달하며 다음 배치는 소비 이후 전송합니다. 상한을 넘는 단일 항목은
 * 요청 전에 거부합니다. 입력 오류·취소 때 남은 버퍼를 전송하지 않으며 성공한 이전 배치를
 * 롤백하거나 재시도하지 않습니다. request에는 points가 없어야 합니다.
 *
 * @param maxBatchItems 배치당 최대 포인트 수
 * @param maxBatchBytes 컬렉션·옵션을 포함한 protobuf 요청의 최대 직렬화 바이트 수
 */
fun QdrantClient.upsertBatches(
    points: Flow<PointStruct>,
    request: UpsertPoints,
    maxBatchItems: Int = 256,
    maxBatchBytes: Int = DEFAULT_BATCH_BYTES,
    timeout: Duration? = null,
): Flow<UpdateResult> {
    maxBatchItems.requirePositiveNumber("maxBatchItems")
    maxBatchBytes.requirePositiveNumber("maxBatchBytes")
    require(request.pointsCount == 0) { "request must not contain points" }
    require(request.serializedSize <= maxBatchBytes) { "request exceeds maxBatchBytes" }
    return flow {
        var batch = request.toBuilder()
        points.collect { point ->
            val single = request.toBuilder().addPoints(point).build()
            require(single.serializedSize <= maxBatchBytes) { "point exceeds maxBatchBytes" }
            val candidate = batch.clone().addPoints(point).build()
            if (batch.pointsCount > 0 && candidate.serializedSize > maxBatchBytes) {
                emit(upsertSuspending(batch.build(), timeout))
                batch = request.toBuilder()
            }
            batch.addPoints(point)
            if (batch.pointsCount == maxBatchItems) {
                emit(upsertSuspending(batch.build(), timeout))
                batch = request.toBuilder()
            }
        }
        if (batch.pointsCount > 0) emit(upsertSuspending(batch.build(), timeout))
    }
}

// SDK 실패 형식은 제한하지 않되 민감한 예외 원문을 로그에 남기지 않습니다.
@Suppress("TooGenericExceptionCaught")
private suspend fun <T> qdrantCall(operation: String, call: () -> ListenableFuture<T>): T {
    currentCoroutineContext().ensureActive()
    return try {
        call().awaitQdrant()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        QdrantLog.log.warn { "Qdrant operation=$operation failed" }
        throw failure
    }
}

// 완료 콜백에서만 get을 호출하므로 대기 중인 RPC마다 블로킹 스레드가 필요하지 않습니다.
private suspend fun <T> ListenableFuture<T>.awaitQdrant(): T = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel(true) }
    addListener({
        try {
            continuation.resume(get())
        } catch (failure: ExecutionException) {
            continuation.resumeWithException(failure.cause ?: failure)
        } catch (failure: CancellationException) {
            continuation.cancel(failure)
        } catch (failure: InterruptedException) {
            Thread.currentThread().interrupt()
            continuation.resumeWithException(failure)
        }
    }, MoreExecutors.directExecutor())
}
