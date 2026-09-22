package io.bluetape4k.qdrant

import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.SettableFuture
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeLessThan
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.qdrant.client.grpc.pointId
import io.bluetape4k.qdrant.client.grpc.pointIdOf
import io.bluetape4k.qdrant.client.grpc.pointStruct
import io.bluetape4k.qdrant.client.grpc.queryPoints
import io.bluetape4k.qdrant.client.grpc.retrievedPoint
import io.bluetape4k.qdrant.client.grpc.scrollPoints
import io.bluetape4k.qdrant.client.grpc.scrollResponse
import io.bluetape4k.qdrant.client.grpc.scrollResponseOf
import io.bluetape4k.qdrant.client.grpc.upsertPoints
import io.bluetape4k.qdrant.client.grpc.upsertPointsOf
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.qdrant.client.QdrantClient
import io.qdrant.client.VectorsFactory.vectors
import io.qdrant.client.grpc.Common
import io.qdrant.client.grpc.Points.PointStruct
import io.qdrant.client.grpc.Points.QueryPoints
import io.qdrant.client.grpc.Points.RetrievedPoint
import io.qdrant.client.grpc.Points.ScoredPoint
import io.qdrant.client.grpc.Points.ScrollPoints
import io.qdrant.client.grpc.Points.ScrollResponse
import io.qdrant.client.grpc.Points.UpdateResult
import io.qdrant.client.grpc.Points.UpsertPoints
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class QdrantCoroutinesTest {

    companion object: KLoggingChannel()

    private val client = mockk<QdrantClient>()

    @BeforeEach
    fun beforeEach() {
        clearMocks(client)
    }

    @Test
    fun `query forwards request and deadline`() = runTest {
        val request = queryPoints { collectionName = "vector" }
        val timeout = Duration.ofSeconds(3)

        every { client.queryAsync(request, timeout) } returns Futures.immediateFuture(emptyList())

        client.querySuspending(request, timeout).shouldBeEmpty()
        verify(exactly = 1) { client.queryAsync(request, timeout) }
    }

    @Test
    fun `cancellation cancels pending future without closing client`() = runTest {
        val pending = SettableFuture.create<List<ScoredPoint>>()

        every {
            client.queryAsync(any<QueryPoints>(), any<Duration>())
        } returns pending

        val job = async {
            client.querySuspending(QueryPoints.getDefaultInstance())
        }
        runCurrent()
        job.cancelAndJoin()

        pending.isCancelled.shouldBeTrue()
        verify(exactly = 0) { client.close() }
    }

    @Test
    fun `scroll is cold and take stops before next page`() = runTest {
        val first = retrievedPoint { id = pointIdOf(1) }

        every {
            client.scrollAsync(any<ScrollPoints>(), any<Duration>())
        } returns Futures.immediateFuture(
            scrollResponse {
                addResult(first)
                nextPageOffset = pointId { num = 2 }
            }
        )

        val flow = client.scrollAsFlow(scrollPoints { collectionName = "vector"; limit = 10 })

        verify(exactly = 0) { client.scrollAsync(any<ScrollPoints>(), any<Duration>()) }
        flow.take(1).toList() shouldBeEqualTo listOf(first)

        verify(exactly = 1) { client.scrollAsync(any<ScrollPoints>(), any<Duration>()) }
        flow.take(1).toList() shouldBeEqualTo listOf(first)
    }

    @Test
    fun `scroll rejects repeated cursor`() = runTest {
        every {
            client.scrollAsync(any<ScrollPoints>(), any<Duration>())
        } returns Futures.immediateFuture(scrollResponse { nextPageOffset = pointIdOf(2) })

        assertFailsWith<IllegalStateException> {
            client.scrollAsFlow(ScrollPoints.newBuilder().setLimit(2).build()).toList()
        }

        verify(exactly = 2) { client.scrollAsync(any<ScrollPoints>(), any<Duration>()) }
    }

    @Test
    fun `upsert batches respect item and serialized byte limits`() = runTest {
        val requests = mutableListOf<UpsertPoints>()

        every {
            client.upsertAsync(capture(requests), any<Duration>())
        } returns
                Futures.immediateFuture(UpdateResult.getDefaultInstance())

        val template = upsertPoints { collectionName = "vectors"; wait = true }
        val points = List(5) { pointStruct { id = pointIdOf(it + 1L) } }
        val byteLimit = template.toBuilder().addAllPoints(points.take(2)).build().serializedSize

        client.upsertBatches(
            points.asFlow(),
            template,
            3,
            byteLimit
        ).toList() shouldHaveSize 3

        requests.map { it.pointsCount } shouldBeEqualTo listOf(2, 2, 1)
        requests.all { it.serializedSize <= byteLimit && it.wait }.shouldBeTrue()
    }

    @Test
    fun `oversized point is rejected before sending`() = runTest {
        val template = upsertPoints { collectionName = "vectors" }
        val point = pointStruct { id = pointIdOf(1) }

        assertFailsWith<IllegalArgumentException> {
            client.upsertBatches(
                listOf(point).asFlow(),
                template,
                maxBatchBytes = 1
            ).toList()
        }

        verify(exactly = 0) { client.upsertAsync(any<UpsertPoints>(), any<Duration>()) }
    }

    @Test
    fun `deadline failure is unwrapped and timeout cancels future`() = runTest {
        val failure = Status.DEADLINE_EXCEEDED.asRuntimeException()

        every {
            client.queryAsync(any<QueryPoints>(), any<Duration>())
        } returns Futures.immediateFailedFuture(failure)

        assertFailsWith<StatusRuntimeException> {
            client.querySuspending(QueryPoints.getDefaultInstance())
        } shouldBeSameInstanceAs failure

        val pending = SettableFuture.create<List<ScoredPoint>>()

        every {
            client.queryAsync(any<QueryPoints>(), any<Duration>())
        } returns pending

        assertFailsWith<kotlinx.coroutines.TimeoutCancellationException> {
            withTimeout(10.milliseconds) {
                client.querySuspending(QueryPoints.getDefaultInstance())
            }
        }

        pending.isCancelled.shouldBeTrue()
    }

    @Test
    fun `scroll preserves options and bounds longer cursor cycles`() = runTest {
        val requests = mutableListOf<ScrollPoints>()
        val timeout = Duration.ofSeconds(2)

        every {
            client.scrollAsync(capture(requests), timeout)
        } answers {
            val offset = pointIdOf(if (requests.size % 2 == 1) 1 else 2)
            Futures.immediateFuture(scrollResponseOf(offset))
        }
        val request = scrollPoints {
            collectionName = "vectors"
            limit = 5
            setFilter(Common.Filter.newBuilder())
        }

        assertFailsWith<IllegalStateException> {
            client.scrollAsFlow(request, timeout, maxPages = 3).toList()
        }

        requests shouldHaveSize 3

        requests.all {
            it.collectionName == request.collectionName && it.limit == 5 && it.filter == request.filter
        }.shouldBeTrue()
    }

    @Test
    fun `partial batch successes survive later failure and input failure does not flush`() = runTest {
        val failure = IllegalStateException("server failure")

        every {
            client.upsertAsync(any<UpsertPoints>(), any<Duration>())
        } returnsMany listOf(
            Futures.immediateFuture(UpdateResult.getDefaultInstance()),
            Futures.immediateFailedFuture(failure)
        )
        val template = upsertPoints { collectionName = "vectors" }
        val points = List(3) { pointStruct { id = pointIdOf(it + 1L) } }
        val results = mutableListOf<UpdateResult>()

        val batchFailure = assertFailsWith<IllegalStateException> {
            client
                .upsertBatches(points.asFlow(), template, maxBatchItems = 1)
                .collect { results.add(it) }
        }

        batchFailure.message shouldBeEqualTo failure.message
        results shouldHaveSize 1

        verify(exactly = 2) { client.upsertAsync(any<UpsertPoints>(), any<Duration>()) }

        val brokenInput = flow {
            emit(points.first())
            throw failure
        }

        val inputFailure = assertFailsWith<IllegalStateException> {
            client.upsertBatches(brokenInput, template)
                .toList()
        }

        inputFailure.message shouldBeEqualTo failure.message

        verify(exactly = 2) {
            client.upsertAsync(any<UpsertPoints>(), any<Duration>())
        }
    }

    @Test
    fun `future completion races cancellation safely`() = runTest {
        repeat(20) {
            val pending = SettableFuture.create<List<ScoredPoint>>()

            every { client.queryAsync(any<QueryPoints>(), any<Duration>()) } returns pending

            val job = launch {
                client.querySuspending(QueryPoints.getDefaultInstance())
            }
            runCurrent()

            MultithreadingTester()
                .workers(2)
                .rounds(1)
                .add { pending.set(emptyList()) }
                .add { job.cancel() }
                .run()

            job.join()
            pending.isDone.shouldBeTrue()
        }
    }

    @Test
    fun `slow collector delays next page and pending page is cancelled`() = runTest {
        val pending = SettableFuture.create<ScrollResponse>()
        val first = scrollResponse {
            addResult(RetrievedPoint.getDefaultInstance())
            nextPageOffset = pointIdOf(2)
        }

        every {
            client.scrollAsync(any<ScrollPoints>(), any<Duration>())
        } returnsMany listOf(
            Futures.immediateFuture(first),
            pending
        )

        val gate = CompletableDeferred<Unit>()
        val job = launch {
            client
                .scrollAsFlow(scrollPoints { limit = 1 })
                .collect { gate.await() }
        }
        runCurrent()

        verify(exactly = 1) { client.scrollAsync(any<ScrollPoints>(), any<Duration>()) }
        gate.complete(Unit)

        runCurrent()

        verify(exactly = 2) { client.scrollAsync(any<ScrollPoints>(), any<Duration>()) }
        job.cancelAndJoin()
        pending.isCancelled.shouldBeTrue()

        verify(exactly = 2) { client.scrollAsync(any<ScrollPoints>(), any<Duration>()) }
    }

    @Test
    fun `invalid bounds reject without RPC`() {
        val points = emptyList<PointStruct>().asFlow()
        val request = UpsertPoints.getDefaultInstance()
        val invalidCalls = listOf<() -> Any>(
            { client.upsertBatches(points, request, maxBatchItems = 0) },
            { client.upsertBatches(points, request, maxBatchBytes = 0) },
            { client.upsertBatches(points, request.toBuilder().addPoints(PointStruct.getDefaultInstance()).build()) },
            { client.scrollAsFlow(ScrollPoints.newBuilder().setLimit(0).build()) },
            { client.scrollAsFlow(ScrollPoints.newBuilder().setLimit(1001).build()) },
            { client.scrollAsFlow(ScrollPoints.newBuilder().setLimit(1).build(), maxPages = 0) },
        )

        invalidCalls.forEach { call ->
            assertFailsWith<IllegalArgumentException> {
                call()
            }
        }
        verify { client wasNot Called }
    }

    @Test
    fun `batch requests wait for completion and collector cancellation stops dispatch`() = runTest {
        val pending = SettableFuture.create<UpdateResult>()

        every {
            client.upsertAsync(any<UpsertPoints>(), any<Duration>())
        } returns pending

        val template = upsertPointsOf("vectors")
        val points = List(5) {
            pointStruct { id = pointIdOf(it + 1L) }
        }.asFlow()

        val result = async {
            client.upsertBatches(points, template, maxBatchItems = 1)
                .take(1)
                .toList()
        }
        runCurrent()
        verify(exactly = 1) { client.upsertAsync(any<UpsertPoints>(), any<Duration>()) }

        pending.set(UpdateResult.getDefaultInstance())
        result.await().size shouldBeEqualTo 1

        verify(exactly = 1) { client.upsertAsync(any<UpsertPoints>(), any<Duration>()) }
        verify(exactly = 0) { client.close() }
    }

    @Test
    fun `large batches complete within a bounded construction time`() {
        every {
            client.upsertAsync(any<UpsertPoints>(), any<Duration>())
        } returns Futures.immediateFuture(UpdateResult.getDefaultInstance())

        val template = upsertPointsOf("vectors")
        val points = List(50_000) { number ->
            pointStruct {
                id = pointIdOf(number + 1L)
                this.vectors = vectors(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f)
            }
        }

        assertTimeoutPreemptively(Duration.ofSeconds(10)) {
            val result = runBlocking {
                client
                    .upsertBatches(
                        points.asFlow(),
                        template,
                        maxBatchItems = points.size
                    )
                    .toList()
            }
            result shouldHaveSize 1
        }
    }

    @Test
    fun `varint length boundaries keep the exact serialized byte limit`() = runTest {
        val template = upsertPoints { collectionName = "vectors"; wait = true }
        val vectorData = FloatArray(32) { it.toFloat() }
        val points = listOf(127L, 128L).map { number ->
            pointStruct {
                id = pointIdOf(number)
                vectors = vectors(*vectorData)
            }
        }
        points[0].serializedSize shouldBeLessThan points[1].serializedSize
        points.all { it.serializedSize >= 128 }.shouldBeTrue()

        val exactLimit = template.toBuilder().addAllPoints(points).build().serializedSize
        val requestsAtExactLimit = mutableListOf<UpsertPoints>()


        every {
            client.upsertAsync(capture(requestsAtExactLimit), any<Duration>())
        } returns Futures.immediateFuture(UpdateResult.getDefaultInstance())

        client
            .upsertBatches(
                points.asFlow(),
                template,
                maxBatchItems = points.size,
                maxBatchBytes = exactLimit,
            )
            .toList()

        requestsAtExactLimit.map { it.pointsCount } shouldBeEqualTo listOf(2)
        requestsAtExactLimit.single().serializedSize shouldBeEqualTo exactLimit

        val requestsBelowLimit = mutableListOf<UpsertPoints>()
        val belowLimitClient = mockk<QdrantClient>()

        every {
            belowLimitClient.upsertAsync(capture(requestsBelowLimit), any<Duration>())
        } returns Futures.immediateFuture(UpdateResult.getDefaultInstance())

        belowLimitClient
            .upsertBatches(
                points.asFlow(),
                template,
                maxBatchItems = points.size,
                maxBatchBytes = exactLimit - 1,
            )
            .toList()

        requestsBelowLimit.map { it.pointsCount } shouldBeEqualTo listOf(1, 1)
        requestsBelowLimit.all { it.serializedSize <= exactLimit - 1 }.shouldBeTrue()
    }
}
