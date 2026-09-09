package io.bluetape4k.openfga

import dev.openfga.sdk.api.OpenFgaApi
import dev.openfga.sdk.api.client.ApiResponse
import dev.openfga.sdk.api.configuration.ConfigurationOverride
import dev.openfga.sdk.api.model.BatchCheckItem
import dev.openfga.sdk.api.model.BatchCheckRequest
import dev.openfga.sdk.api.model.BatchCheckResponse
import dev.openfga.sdk.api.model.BatchCheckSingleResult
import dev.openfga.sdk.api.model.CheckError
import dev.openfga.sdk.api.model.CheckRequest
import dev.openfga.sdk.api.model.CheckRequestTupleKey
import dev.openfga.sdk.api.model.CheckResponse
import dev.openfga.sdk.api.model.ConsistencyPreference
import dev.openfga.sdk.api.model.ContextualTupleKeys
import dev.openfga.sdk.api.model.ReadRequest
import dev.openfga.sdk.api.model.ReadRequestTupleKey
import dev.openfga.sdk.api.model.ReadResponse
import dev.openfga.sdk.api.model.Tuple
import dev.openfga.sdk.api.model.TupleKey
import dev.openfga.sdk.api.model.TupleKeyWithoutCondition
import dev.openfga.sdk.api.model.WriteRequest
import dev.openfga.sdk.api.model.WriteRequestDeletes
import dev.openfga.sdk.api.model.WriteRequestWrites
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Duration
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.concurrent.CompletableFuture

class OpenFgaCoroutinesTest {

    @Test
    fun `공개 요청 값 객체는 직렬화 roundtrip을 지원한다`() {
        val values = listOf(
            OpenFgaScope("store", "model"),
            openFgaTuple("user:anne", "reader", "document:budget"),
        )

        values.forEach { value ->
            val bytes = ByteArrayOutputStream().use { output ->
                ObjectOutputStream(output).use { stream -> stream.writeObject(value) }
                output.toByteArray()
            }

            val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { stream -> stream.readObject() }
            restored shouldBeEqualTo value
        }
    }

    @Test
    fun `scope와 tuple builder는 공백 입력을 거부한다`() {
        assertFailsWith<IllegalArgumentException> { OpenFgaScope(" ") }
        assertFailsWith<IllegalArgumentException> { OpenFgaScope("store", " ") }
        assertFailsWith<IllegalArgumentException> { openFgaTuple("user:anne", " ", "document:budget") }
        assertFailsWith<IllegalArgumentException> { openFgaTuple(" ", "reader", "document:budget") }
    }

    @Test
    fun `checkSuspending은 명시한 store와 authorization model을 request에 고정하고 allow를 보존한다`() =
        runTest {
            val api = mockk<OpenFgaApi>()
            val scope = OpenFgaScope("store-a", "model-a")
            val request = CheckRequest()
                .tupleKey(CheckRequestTupleKey().user("user:anne").relation("reader")._object("document:budget"))
            val bodySlot = slot<CheckRequest>()
            val response = ApiResponse(200, emptyMap(), "", CheckResponse().allowed(true))
            every { api.check("store-a", capture(bodySlot), any<ConfigurationOverride>()) } returns
                CompletableFuture.completedFuture(response)

            val actual = api.checkSuspending(scope, request)

            actual.data.getAllowed().shouldBeTrue()
            bodySlot.captured.getAuthorizationModelId() shouldBeEqualTo "model-a"
            bodySlot.captured.getTupleKey().getUser() shouldBeEqualTo "user:anne"
            verify(exactly = 1) { api.check("store-a", any(), any<ConfigurationOverride>()) }
        }

    @Test
    fun `checkSuspending은 서로 다른 scope를 격리하고 request 옵션을 모두 보존한다`() = runTest {
        val api = mockk<OpenFgaApi>()
        val captured = mutableListOf<Pair<String, CheckRequest>>()
        val response = ApiResponse(200, emptyMap(), "", CheckResponse().allowed(true))
        every { api.check(any(), any(), any<ConfigurationOverride>()) } answers {
            captured += firstArg<String>() to secondArg<CheckRequest>()
            CompletableFuture.completedFuture(response)
        }

        val firstContextual = ContextualTupleKeys().addTupleKeysItem(
            TupleKey().user("user:first").relation("reader")._object("document:first"),
        )
        val firstContext = mapOf("tenant" to "first")
        val firstRequest = CheckRequest(true)
            .tupleKey(CheckRequestTupleKey().user("user:first").relation("reader")._object("document:first"))
            .contextualTuples(firstContextual)
            .context(firstContext)
            .consistency(ConsistencyPreference.HIGHER_CONSISTENCY)
            .authorizationModelId("caller-model")
        val secondRequest = CheckRequest(false)
            .tupleKey(CheckRequestTupleKey().user("user:second").relation("reader")._object("document:second"))
            .context(mapOf("tenant" to "second"))
            .consistency(ConsistencyPreference.MINIMIZE_LATENCY)
        val firstScope = OpenFgaScope("store-first", "model-first")
        val secondScope = OpenFgaScope("store-second", "model-second")

        api.checkSuspending(firstScope, firstRequest)
        api.checkSuspending(secondScope, secondRequest)

        captured.map { it.first } shouldBeEqualTo listOf("store-first", "store-second")
        captured[0].second.getAuthorizationModelId() shouldBeEqualTo "model-first"
        captured[0].second.getTrace() shouldBeEqualTo true
        captured[0].second.getContextualTuples() shouldBeEqualTo firstContextual
        captured[0].second.getContext() shouldBeEqualTo firstContext
        captured[0].second.getConsistency() shouldBeEqualTo ConsistencyPreference.HIGHER_CONSISTENCY
        captured[1].second.getAuthorizationModelId() shouldBeEqualTo "model-second"
        captured[1].second.getTrace() shouldBeEqualTo false
        captured[1].second.getContext() shouldBeEqualTo mapOf("tenant" to "second")
        captured[1].second.getConsistency() shouldBeEqualTo ConsistencyPreference.MINIMIZE_LATENCY
        firstRequest.getAuthorizationModelId() shouldBeEqualTo "caller-model"
        secondRequest.getAuthorizationModelId() shouldBeEqualTo null
    }

    @Test
    fun `checkSuspending은 호출자가 전달한 configuration override를 그대로 사용한다`() = runTest {
        val api = mockk<OpenFgaApi>()
        val override = ConfigurationOverride().readTimeout(Duration.ofSeconds(2))
        val response = ApiResponse(200, emptyMap(), "", CheckResponse().allowed(false))
        every { api.check("store", any(), refEq(override)) } returns CompletableFuture.completedFuture(response)

        api.checkSuspending(OpenFgaScope("store", "model"), CheckRequest(), override)

        verify(exactly = 1) { api.check("store", any(), refEq(override)) }
    }

    @Test
    fun `checkSuspending은 원격 오류를 권한 허용으로 바꾸지 않는다`() = runTest {
        val api = mockk<OpenFgaApi>()
        val failure = IllegalStateException("remote failure")
        every { api.check(any(), any(), any<ConfigurationOverride>()) } returns
            CompletableFuture.failedFuture(failure)

        val actual = assertFailsWith<IllegalStateException> {
            api.checkSuspending(OpenFgaScope("store", "model"), CheckRequest())
        }

        actual shouldBeEqualTo failure
    }

    @Test
    fun `batchCheckSuspending은 항목별 허용과 오류를 모두 보존한다`() = runTest {
        val api = mockk<OpenFgaApi>()
        val itemError = BatchCheckSingleResult().error(CheckError().message("tuple rejected"))
        val response = ApiResponse(
            200,
            emptyMap(),
            "",
            BatchCheckResponse()
                .putResultItem("allowed", BatchCheckSingleResult().allowed(true))
                .putResultItem("rejected", itemError),
        )
        every { api.batchCheck(any(), any(), any<ConfigurationOverride>()) } returns
            CompletableFuture.completedFuture(response)

        val request = BatchCheckRequest()
            .addChecksItem(
                BatchCheckItem()
                    .tupleKey(CheckRequestTupleKey().user("user:anne").relation("reader")._object("document:budget"))
                    .correlationId("allowed"),
            )
            .addChecksItem(
                BatchCheckItem()
                    .tupleKey(CheckRequestTupleKey().user("user:bob").relation("reader")._object("document:budget"))
                    .correlationId("rejected"),
            )
        val actual = api.batchCheckSuspending(
            OpenFgaScope("store", "model"),
            request,
        )

        val result = requireNotNull(actual.data.getResult())
        result["allowed"]?.getAllowed().shouldBeTrue()
        result["rejected"]?.getError()?.getMessage() shouldBeEqualTo "tuple rejected"
    }

    @Test
    fun `batchCheckSuspending은 빈 batch를 원격 호출 전에 거부한다`() = runTest {
        val api = mockk<OpenFgaApi>()

        assertFailsWith<IllegalArgumentException> {
            api.batchCheckSuspending(OpenFgaScope("store", "model"), BatchCheckRequest())
        }

        verify(exactly = 0) { api.batchCheck(any(), any(), any<ConfigurationOverride>()) }
    }

    @Test
    fun `readSuspending은 read filter와 consistency를 보존하고 page 옵션만 복사해 덮어쓴다`() = runTest {
        val api = mockk<OpenFgaApi>()
        val request = ReadRequest()
            .tupleKey(ReadRequestTupleKey().user("user:anne").relation("reader")._object("document:budget"))
            .consistency(ConsistencyPreference.HIGHER_CONSISTENCY)
        val captured = slot<ReadRequest>()
        every { api.read("store-read", capture(captured), any<ConfigurationOverride>()) } returns
            CompletableFuture.completedFuture(readResponse(emptyList(), ""))

        api.readSuspending(
            OpenFgaScope("store-read"),
            request,
            pageSize = 7,
            continuationToken = "cursor-2",
        )

        val copiedTuple = requireNotNull(captured.captured.getTupleKey())
        copiedTuple.getUser() shouldBeEqualTo "user:anne"
        copiedTuple.getRelation() shouldBeEqualTo "reader"
        copiedTuple.getObject() shouldBeEqualTo "document:budget"
        captured.captured.getPageSize() shouldBeEqualTo 7
        captured.captured.getContinuationToken() shouldBeEqualTo "cursor-2"
        captured.captured.getConsistency() shouldBeEqualTo ConsistencyPreference.HIGHER_CONSISTENCY
        request.getPageSize() shouldBeEqualTo null
        request.getContinuationToken() shouldBeEqualTo null
    }

    @Test
    fun `readTuplesFlow는 수집할 때만 요청하고 page를 순차적으로 읽는다`() = runTest {
        val api = mockk<OpenFgaApi>()
        val requests = mutableListOf<ReadRequest>()
        every { api.read(any(), any(), any<ConfigurationOverride>()) } answers {
            val request = secondArg<ReadRequest>()
            requests += request
            val tuple = Tuple().key(
                TupleKey().user("user:anne").relation("reader")._object("document:${requests.size}"),
            )
            CompletableFuture.completedFuture(
                readResponse(listOf(tuple), if (requests.size == 1) "page-2" else ""),
            )
        }

        val flow = api.readTuplesFlow(OpenFgaScope("store", "model"), ReadRequest(), pageSize = 1)
        verify(exactly = 0) { api.read(any(), any(), any<ConfigurationOverride>()) }

        val tuples = flow.toList()

        tuples shouldHaveSize 2
        requests shouldHaveSize 2
        requests[0].getPageSize() shouldBeEqualTo 1
        requests[0].getContinuationToken() shouldBeEqualTo null
        requests[1].getContinuationToken() shouldBeEqualTo "page-2"
    }

    @Test
    fun `readTuplesFlow는 take로 중단하면 다음 page를 요청하지 않는다`() = runTest {
        val api = mockk<OpenFgaApi>()
        every { api.read(any(), any(), any<ConfigurationOverride>()) } returns
            CompletableFuture.completedFuture(
                readResponse(
                    listOf(
                        Tuple().key(TupleKey().user("user:anne").relation("reader")._object("document:budget")),
                    ),
                    "next",
                ),
            )

        api.readTuplesFlow(OpenFgaScope("store", "model"), pageSize = 1)
            .take(1)
            .toList()

        verify(exactly = 1) { api.read(any(), any(), any<ConfigurationOverride>()) }
    }

    @Test
    fun `readTuplesFlow는 page 크기와 page 상한을 검증한다`() {
        val api = mockk<OpenFgaApi>()

        assertFailsWith<IllegalArgumentException> {
            api.readTuplesFlow(OpenFgaScope("store"), pageSize = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            api.readTuplesFlow(OpenFgaScope("store"), maxPages = 0)
        }
    }

    @Test
    fun `readTuplesFlow는 같은 continuation token 반복을 거부한다`() = runTest {
        val api = mockk<OpenFgaApi>()
        every { api.read(any(), any(), any<ConfigurationOverride>()) } returns
            CompletableFuture.completedFuture(readResponse(emptyList(), "same"))

        assertFailsWith<IllegalStateException> {
            api.readTuplesFlow(OpenFgaScope("store"), pageSize = 1, maxPages = 3).toList()
        }
    }

    @Test
    fun `readTuplesFlow는 장주기 cursor를 maxPages에서 중단한다`() = runTest {
        val api = mockk<OpenFgaApi>()
        every { api.read(any(), any(), any<ConfigurationOverride>()) } answers {
            val token = secondArg<ReadRequest>().getContinuationToken()
            val next = when (token) {
                null -> "a"
                "a" -> "b"
                else -> "a"
            }
            CompletableFuture.completedFuture(readResponse(emptyList(), next))
        }

        assertFailsWith<IllegalStateException> {
            api.readTuplesFlow(OpenFgaScope("store"), pageSize = 1, maxPages = 3).toList()
        }

        verify(exactly = 3) { api.read(any(), any(), any<ConfigurationOverride>()) }
    }

    @Test
    fun `future 취소는 SDK future 취소로 전파된다`() = runTest {
        val future = CompletableFuture<ApiResponse<CheckResponse>>()
        val api = mockk<OpenFgaApi>()
        every { api.check(any(), any(), any<ConfigurationOverride>()) } returns future

        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            api.checkSuspending(OpenFgaScope("store", "model"), CheckRequest())
        }
        job.cancelAndJoin()

        future.isCancelled.shouldBeTrue()
    }

    @Test
    fun `readTuplesFlow의 pending page 취소는 SDK future 취소로 전파된다`() = runTest {
        val future = CompletableFuture<ApiResponse<ReadResponse>>()
        val api = mockk<OpenFgaApi>()
        every { api.read(any(), any(), any<ConfigurationOverride>()) } returns future

        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            api.readTuplesFlow(OpenFgaScope("store"), pageSize = 1).toList()
        }
        job.cancelAndJoin()

        future.isCancelled.shouldBeTrue()
        verify(exactly = 1) { api.read(any(), any(), any<ConfigurationOverride>()) }
    }

    @Test
    fun `writeSuspending은 model scope와 write delete 옵션을 보존하고 원본 request를 변경하지 않는다`() = runTest {
        val api = mockk<OpenFgaApi>()
        val request = WriteRequest()
            .authorizationModelId("caller-model")
            .writes(
                WriteRequestWrites()
                    .onDuplicate(WriteRequestWrites.OnDuplicateEnum.IGNORE)
                    .addTupleKeysItem(TupleKey().user("user:anne").relation("reader")._object("document:budget")),
            )
            .deletes(
                WriteRequestDeletes()
                    .onMissing(WriteRequestDeletes.OnMissingEnum.IGNORE)
                    .addTupleKeysItem(
                        TupleKeyWithoutCondition()
                            .user("user:anne")
                            .relation("reader")
                            ._object("document:old"),
                    ),
            )
        val captured = slot<WriteRequest>()
        val response = ApiResponse<Any>(200, emptyMap(), "", Any())
        every { api.write("store-a", capture(captured), any<ConfigurationOverride>()) } returns
            CompletableFuture.completedFuture(response)

        api.writeSuspending(OpenFgaScope("store-a", "model-a"), request)

        captured.captured.getAuthorizationModelId() shouldBeEqualTo "model-a"
        captured.captured.getWrites()?.getOnDuplicate() shouldBeEqualTo WriteRequestWrites.OnDuplicateEnum.IGNORE
        captured.captured.getDeletes()?.getOnMissing() shouldBeEqualTo WriteRequestDeletes.OnMissingEnum.IGNORE
        request.getAuthorizationModelId() shouldBeEqualTo "caller-model"
        request.getWrites()?.getOnDuplicate() shouldBeEqualTo WriteRequestWrites.OnDuplicateEnum.IGNORE
        request.getDeletes()?.getOnMissing() shouldBeEqualTo WriteRequestDeletes.OnMissingEnum.IGNORE
    }

    @Test
    fun `writeSuspending은 writes와 deletes 합계가 서버 기본 상한을 넘으면 거부한다`() = runTest {
        val api = mockk<OpenFgaApi>()
        val request = WriteRequest()
            .writes(WriteRequestWrites().tupleKeys(List(60) { TupleKey() }))
            .deletes(
                WriteRequestDeletes().tupleKeys(
                    List(41) { TupleKeyWithoutCondition() },
                ),
            )

        assertFailsWith<IllegalArgumentException> {
            api.writeSuspending(OpenFgaScope("store", "model"), request)
        }

        verify(exactly = 0) { api.write(any(), any(), any<ConfigurationOverride>()) }
    }

    private fun readResponse(tuples: List<Tuple>, continuationToken: String): ApiResponse<ReadResponse> =
        ApiResponse(200, emptyMap(), "", ReadResponse().tuples(tuples).continuationToken(continuationToken))
}
