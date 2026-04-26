package io.bluetape4k.elasticsearch.coroutines

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester
import co.elastic.clients.elasticsearch._helpers.bulk.BulkListener
import co.elastic.clients.elasticsearch.core.BulkRequest
import co.elastic.clients.elasticsearch.core.BulkResponse
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation
import co.elastic.clients.util.ObjectBuilder
import io.bluetape4k.elasticsearch.ElasticsearchDefaults
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runInterruptible
import java.io.Closeable
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.function.Function

// ---------------------------------------------------------------------------
// BulkIngester 팩토리 함수
// ---------------------------------------------------------------------------

/**
 * [ElasticsearchAsyncClient] 기반의 [BulkIngester] 를 생성합니다.
 *
 * `maxOperations` 개수만큼 작업이 쌓이거나 `flushInterval` 이 경과하면 자동으로 Bulk 요청을 전송합니다.
 * `listener` 를 지정하면 요청 전송 전/후 이벤트를 받을 수 있습니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val ingester = bulkIngesterOf<Void>(
 *     client = asyncClient,
 *     maxOperations = 500,
 *     flushInterval = Duration.ofSeconds(3),
 * )
 * ingester.use {
 *     it.addSuspend(BulkOperation.of { op -> op.index { i -> i.index("my-index").id("1").document(doc) } })
 * }
 * ```
 *
 * @param Context 각 Bulk 작업에 연결할 애플리케이션 컨텍스트 타입
 * @param client  동기 [ElasticsearchClient] — 내부적으로 [ElasticsearchAsyncClient] 로 변환됩니다
 * @param maxOperations 한 번의 Bulk 요청에 담을 최대 작업 수 (기본값: [ElasticsearchDefaults.DEFAULT_BULK_INGESTER_MAX_OPERATIONS])
 * @param flushInterval 자동 flush 주기 (기본값: [ElasticsearchDefaults.DEFAULT_BULK_INGESTER_FLUSH_INTERVAL])
 * @param listener Bulk 요청 전/후 이벤트를 수신할 [BulkListener] (null 이면 리스너 없음)
 * @return 설정이 적용된 [BulkIngester] 인스턴스
 */
fun <Context> bulkIngesterOf(
    client: ElasticsearchClient,
    maxOperations: Int = ElasticsearchDefaults.DEFAULT_BULK_INGESTER_MAX_OPERATIONS,
    flushInterval: Duration = ElasticsearchDefaults.DEFAULT_BULK_INGESTER_FLUSH_INTERVAL,
    listener: BulkListener<Context>? = null,
): BulkIngester<Context> {
    maxOperations.requirePositiveNumber("maxOperations")

    return BulkIngester.of { builder ->
        builder.client(client)
            .maxOperations(maxOperations)
            .flushInterval(flushInterval.toMillis(), TimeUnit.MILLISECONDS)
            .apply { if (listener != null) listener(listener) }
    }
}

/**
 * [ElasticsearchAsyncClient] 기반의 [BulkIngester] 를 생성합니다.
 *
 * 비동기 클라이언트를 직접 전달하는 오버로드입니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val ingester = bulkIngesterOf<Void>(
 *     client = asyncClient,
 *     maxOperations = 1000,
 *     flushInterval = Duration.ofSeconds(5),
 * )
 * ```
 *
 * @param Context 각 Bulk 작업에 연결할 애플리케이션 컨텍스트 타입
 * @param client  [ElasticsearchAsyncClient]
 * @param maxOperations 한 번의 Bulk 요청에 담을 최대 작업 수 (기본값: [ElasticsearchDefaults.DEFAULT_BULK_INGESTER_MAX_OPERATIONS])
 * @param flushInterval 자동 flush 주기 (기본값: [ElasticsearchDefaults.DEFAULT_BULK_INGESTER_FLUSH_INTERVAL])
 * @param listener Bulk 요청 전/후 이벤트를 수신할 [BulkListener] (null 이면 리스너 없음)
 * @return 설정이 적용된 [BulkIngester] 인스턴스
 */
fun <Context> bulkIngesterOf(
    client: ElasticsearchAsyncClient,
    maxOperations: Int = ElasticsearchDefaults.DEFAULT_BULK_INGESTER_MAX_OPERATIONS,
    flushInterval: Duration = ElasticsearchDefaults.DEFAULT_BULK_INGESTER_FLUSH_INTERVAL,
    listener: BulkListener<Context>? = null,
): BulkIngester<Context> {
    maxOperations.requirePositiveNumber("maxOperations")

    return BulkIngester.of { builder ->
        builder.client(client)
            .maxOperations(maxOperations)
            .flushInterval(flushInterval.toMillis(), TimeUnit.MILLISECONDS)
            .apply { if (listener != null) listener(listener) }
    }
}

// ---------------------------------------------------------------------------
// BulkIngester suspend 확장함수
// ---------------------------------------------------------------------------

/**
 * [BulkOperation] 을 [BulkIngester] 에 추가합니다 (suspend 버전).
 *
 * [BulkIngester.add] 는 내부적으로 `FnCondition.whenReady()` 를 사용하여
 * 버퍼가 가득 찬 경우 호출 스레드를 블로킹합니다.
 * Coroutines 환경에서 메인 스레드를 블로킹하지 않도록 `withContext(Dispatchers.IO)` 로 감쌉니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val operation = BulkOperation.of { op ->
 *     op.index { i -> i.index("my-index").id("1").document(doc) }
 * }
 * ingester.addSuspend(operation)
 * ```
 *
 * @param Context 각 Bulk 작업에 연결할 애플리케이션 컨텍스트 타입
 * @param operation 추가할 [BulkOperation]
 * @param context 이 작업에 연결할 애플리케이션 컨텍스트 (null 허용)
 */
suspend fun <Context> BulkIngester<Context>.addSuspend(
    operation: BulkOperation,
    context: Context? = null,
) = runInterruptible(Dispatchers.IO) {
    add(operation, context)
}

/**
 * 빌더 람다로 [BulkOperation] 을 구성하여 [BulkIngester] 에 추가합니다 (suspend 버전).
 *
 * [BulkIngester.add] 는 내부적으로 `FnCondition.whenReady()` 를 사용하여
 * 버퍼가 가득 찬 경우 호출 스레드를 블로킹합니다.
 * Coroutines 환경에서 메인 스레드를 블로킹하지 않도록 `withContext(Dispatchers.IO)` 로 감쌉니다.
 *
 * ## 사용 예시
 * ```kotlin
 * ingester.addSuspend { op ->
 *     op.index { i -> i.index("my-index").id("1").document(doc) }
 * }
 * ```
 *
 * @param Context 각 Bulk 작업에 연결할 애플리케이션 컨텍스트 타입
 * @param block [BulkOperation.Builder] 설정 람다
 * @param context 이 작업에 연결할 애플리케이션 컨텍스트 (null 허용)
 */
suspend fun <Context> BulkIngester<Context>.addSuspend(
    block: Function<BulkOperation.Builder, ObjectBuilder<BulkOperation>>,
    context: Context? = null,
) = runInterruptible(Dispatchers.IO) {
    add(block, context)
}

// ---------------------------------------------------------------------------
// BulkListener → Flow 변환
// ---------------------------------------------------------------------------

/**
 * [BulkIngester] 의 Bulk 작업 진행 이벤트를 나타내는 sealed interface.
 *
 * [bulkProgressListener] 를 통해 생성된 [BulkListener] 이벤트가
 * [Flow] 형태로 스트리밍됩니다.
 *
 * @param Context 각 Bulk 작업에 연결된 애플리케이션 컨텍스트 타입
 */
sealed interface BulkProgressEvent<Context> {
    /**
     * Bulk 요청이 전송되기 직전에 발생하는 이벤트.
     *
     * @property executionId 이 요청의 고유 ID
     * @property request 전송될 [BulkRequest]
     * @property contexts 각 작업에 연결된 컨텍스트 목록
     */
    data class Before<Context>(
        val executionId: Long,
        val request: BulkRequest,
        val contexts: List<Context>,
    ) : BulkProgressEvent<Context>

    /**
     * Bulk 요청이 성공적으로 처리된 후 발생하는 이벤트.
     *
     * [BulkResponse.errors] 가 `true` 이면 일부 항목에서 오류가 발생했을 수 있습니다.
     *
     * @property executionId 이 요청의 고유 ID
     * @property request 전송된 [BulkRequest]
     * @property response Elasticsearch 로부터 받은 [BulkResponse]
     * @property contexts 각 작업에 연결된 컨텍스트 목록
     */
    data class After<Context>(
        val executionId: Long,
        val request: BulkRequest,
        val response: BulkResponse,
        val contexts: List<Context>,
    ) : BulkProgressEvent<Context>

    /**
     * Bulk 요청 전송 중 예외가 발생했을 때 발생하는 이벤트.
     *
     * @property executionId 이 요청의 고유 ID
     * @property request 전송 시도한 [BulkRequest]
     * @property exception 발생한 예외
     * @property contexts 각 작업에 연결된 컨텍스트 목록
     */
    data class Error<Context>(
        val executionId: Long,
        val request: BulkRequest,
        val exception: Throwable,
        val contexts: List<Context>,
    ) : BulkProgressEvent<Context>
}

/**
 * [bulkProgressListener] 의 반환값입니다.
 *
 * - [listener]: [BulkIngester] 에 등록할 [BulkListener]
 * - [events]: 이벤트 [Flow] (collect 하지 않으면 채널에 쌓입니다)
 * - [close]: 채널을 닫아 메모리 누수를 방지합니다 — 사용 완료 후 반드시 호출하세요
 *
 * 구조 분해 (`val (listener, events) = bulkProgressListener<Void>()`) 도 지원합니다.
 */
data class BulkListenerHandle<Context>(
    val listener: BulkListener<Context>,
    val events: Flow<BulkProgressEvent<Context>>,
    private val closeAction: () -> Unit,
) : Closeable {
    override fun close() = closeAction()
}

/**
 * [BulkListener] 콜백 이벤트를 [Flow] 로 변환하는 리스너-Flow 쌍을 생성합니다.
 *
 * 반환된 [BulkListener] 를 [BulkIngester] 에 등록하면,
 * `beforeBulk`, `afterBulk(성공)`, `afterBulk(실패)` 이벤트가
 * [Flow] 로 스트리밍됩니다.
 *
 * Channel 은 `UNLIMITED` 버퍼를 사용하여 리스너 콜백이 블로킹되지 않도록 합니다.
 * Flow 를 소비하지 않으면 이벤트가 채널에 쌓이므로, 반드시 collect 하거나 `close()` 를 호출해야 합니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val (listener, events) = bulkProgressListener<Void>()
 * val ingester = bulkIngesterOf<Void>(asyncClient, listener = listener)
 *
 * // 이벤트 소비
 * val job = launch {
 *     events.collect { event ->
 *         when (event) {
 *             is BulkProgressEvent.Before -> println("요청 전송 전: ${event.executionId}")
 *             is BulkProgressEvent.After  -> println("요청 완료: ${event.response.took()} ms")
 *             is BulkProgressEvent.Error  -> println("오류 발생: ${event.exception.message}")
 *         }
 *     }
 * }
 *
 * ingester.use {
 *     // ... 작업 추가 ...
 * }
 * job.cancelAndJoin()
 * ```
 *
 * @param Context 각 Bulk 작업에 연결할 애플리케이션 컨텍스트 타입
 * @return [BulkListenerHandle] — listener, events Flow, 그리고 close() 를 포함합니다
 */
fun <Context> bulkProgressListener(): BulkListenerHandle<Context> {
    val channel = Channel<BulkProgressEvent<Context>>(Channel.UNLIMITED)

    val listener = object : BulkListener<Context> {
        override fun beforeBulk(
            executionId: Long,
            request: BulkRequest,
            contexts: List<Context>,
        ) {
            channel.trySend(
                BulkProgressEvent.Before(
                    executionId = executionId,
                    request = request,
                    contexts = contexts,
                )
            )
        }

        override fun afterBulk(
            executionId: Long,
            request: BulkRequest,
            contexts: List<Context>,
            response: BulkResponse,
        ) {
            channel.trySend(
                BulkProgressEvent.After(
                    executionId = executionId,
                    request = request,
                    response = response,
                    contexts = contexts,
                )
            )
        }

        override fun afterBulk(
            executionId: Long,
            request: BulkRequest,
            contexts: List<Context>,
            failure: Throwable,
        ) {
            channel.trySend(
                BulkProgressEvent.Error(
                    executionId = executionId,
                    request = request,
                    exception = failure,
                    contexts = contexts,
                )
            )
        }
    }

    return BulkListenerHandle(
        listener = listener,
        events = channel.receiveAsFlow(),
        closeAction = channel::close,
    )
}
