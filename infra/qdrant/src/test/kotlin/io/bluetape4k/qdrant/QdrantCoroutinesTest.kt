package io.bluetape4k.qdrant

import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.SettableFuture
import io.mockk.every
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.qdrant.client.QdrantClient
import io.qdrant.client.grpc.Common.PointId
import io.qdrant.client.grpc.Points.QueryPoints
import io.qdrant.client.grpc.Points.ScoredPoint
import io.qdrant.client.grpc.Points.ScrollPoints
import io.qdrant.client.grpc.Points.ScrollResponse
import io.qdrant.client.grpc.Points.RetrievedPoint
import io.qdrant.client.grpc.Points.UpsertPoints
import io.qdrant.client.grpc.Points.UpdateResult
import io.qdrant.client.grpc.Points.PointStruct
import io.qdrant.client.VectorFactory.vector
import io.qdrant.client.VectorsFactory.vectors
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import io.grpc.Status
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class QdrantCoroutinesTest {

    @Test
    fun `query forwards request and deadline`() = runTest {
        val client = mockk<QdrantClient>()
        val request = QueryPoints.newBuilder().setCollectionName("vectors").build()
        val timeout = Duration.ofSeconds(3)
        every { client.queryAsync(request, timeout) } returns Futures.immediateFuture(emptyList())
        (client.querySuspending(request, timeout)) shouldBeEqualTo emptyList<ScoredPoint>()
        verify(exactly = 1) { client.queryAsync(request, timeout) }
    }

    @Test
    fun `cancellation cancels pending future without closing client`() = runTest {
        val client = mockk<QdrantClient>()
        val pending = SettableFuture.create<List<ScoredPoint>>()
        every { client.queryAsync(any<QueryPoints>(), any<Duration>()) } returns pending
        val job = async { client.querySuspending(QueryPoints.getDefaultInstance()) }
        runCurrent()
        job.cancelAndJoin()
        (pending.isCancelled).shouldBeTrue()
        verify(exactly = 0) { client.close() }
    }

    @Test
    fun `scroll is cold and take stops before next page`() = runTest {
        val client = mockk<QdrantClient>()
        val first = RetrievedPoint.newBuilder().setId(id(1)).build()
        every { client.scrollAsync(any<ScrollPoints>(), any<Duration>()) } returns
            Futures.immediateFuture(ScrollResponse.newBuilder().addResult(first).setNextPageOffset(id(2)).build())
        val flow = client.scrollAsFlow(ScrollPoints.newBuilder().setCollectionName("vectors").setLimit(10).build())
        verify(exactly = 0) { client.scrollAsync(any<ScrollPoints>(), any<Duration>()) }
        (flow.take(1).toList()) shouldBeEqualTo listOf(first)
        verify(exactly = 1) { client.scrollAsync(any<ScrollPoints>(), any<Duration>()) }
        (flow.take(1).toList()) shouldBeEqualTo listOf(first)
    }

    @Test
    fun `scroll rejects repeated cursor`() = runTest {
        val client = mockk<QdrantClient>()
        every { client.scrollAsync(any<ScrollPoints>(), any<Duration>()) } returns
            Futures.immediateFuture(ScrollResponse.newBuilder().setNextPageOffset(id(2)).build())
        val error = runCatching {
            client.scrollAsFlow(ScrollPoints.newBuilder().setLimit(2).build()).toList()
        }.exceptionOrNull()
        assertInstanceOf(IllegalStateException::class.java, error)
        verify(exactly = 2) { client.scrollAsync(any<ScrollPoints>(), any<Duration>()) }
    }

    @Test
    fun `upsert batches respect item and serialized byte limits`() = runTest {
        val client = mockk<QdrantClient>()
        val requests = mutableListOf<UpsertPoints>()
        every { client.upsertAsync(capture(requests), any<Duration>()) } returns
            Futures.immediateFuture(UpdateResult.getDefaultInstance())
        val template = UpsertPoints.newBuilder().setCollectionName("vectors").setWait(true).build()
        val points = (1L..5L).map { PointStruct.newBuilder().setId(id(it)).build() }
        val byteLimit = template.toBuilder().addAllPoints(points.take(2)).build().serializedSize
        (client.upsertBatches(points.asFlow(), template, 3, byteLimit).toList().size) shouldBeEqualTo 3
        (requests.map { it.pointsCount }) shouldBeEqualTo listOf(2, 2, 1)
        (requests.all { it.serializedSize <= byteLimit && it.wait }).shouldBeTrue()
    }

    @Test
    fun `oversized point is rejected before sending`() = runTest {
        val client = mockk<QdrantClient>()
        val template = UpsertPoints.newBuilder().setCollectionName("vectors").build()
        val point = PointStruct.newBuilder().setId(id(1)).build()
        val error = runCatching {
            client.upsertBatches(listOf(point).asFlow(), template, maxBatchBytes = 1).toList()
        }.exceptionOrNull()
        assertInstanceOf(IllegalArgumentException::class.java, error)
        verify(exactly = 0) { client.upsertAsync(any<UpsertPoints>(), any<Duration>()) }
    }

    @Test
    fun `deadline failure is unwrapped and timeout cancels future`() = runTest {
        val client = mockk<QdrantClient>()
        val failure = Status.DEADLINE_EXCEEDED.asRuntimeException()
        every { client.queryAsync(any<QueryPoints>(), any<Duration>()) } returns Futures.immediateFailedFuture(failure)
        assertSame(failure, runCatching { client.querySuspending(QueryPoints.getDefaultInstance()) }.exceptionOrNull())
        val pending = SettableFuture.create<List<ScoredPoint>>()
        every { client.queryAsync(any<QueryPoints>(), any<Duration>()) } returns pending
        val error = runCatching {
            withTimeout(10) { client.querySuspending(QueryPoints.getDefaultInstance()) }
        }.exceptionOrNull()
        assertInstanceOf(kotlinx.coroutines.TimeoutCancellationException::class.java, error)
        (pending.isCancelled).shouldBeTrue()
    }

    @Test
    fun `scroll preserves options and bounds longer cursor cycles`() = runTest {
        val client = mockk<QdrantClient>()
        val requests = mutableListOf<ScrollPoints>()
        val timeout = Duration.ofSeconds(2)
        every { client.scrollAsync(capture(requests), timeout) } answers {
            val offset = id(if (requests.size % 2 == 1) 1 else 2)
            Futures.immediateFuture(ScrollResponse.newBuilder().setNextPageOffset(offset).build())
        }
        val request = ScrollPoints.newBuilder().setCollectionName("vectors").setLimit(5)
            .setFilter(io.qdrant.client.grpc.Common.Filter.newBuilder()).build()
        assertInstanceOf(IllegalStateException::class.java,
            runCatching { client.scrollAsFlow(request, timeout, maxPages = 3).toList() }.exceptionOrNull())
        (requests.size) shouldBeEqualTo 3
        (requests.all {
            it.collectionName == request.collectionName && it.limit == 5 && it.filter == request.filter
        }).shouldBeTrue()
    }

    @Test
    fun `partial batch successes survive later failure and input failure does not flush`() = runTest {
        val client = mockk<QdrantClient>()
        val failure = IllegalStateException("server failure")
        every { client.upsertAsync(any<UpsertPoints>(), any<Duration>()) } returnsMany listOf(
            Futures.immediateFuture(UpdateResult.getDefaultInstance()), Futures.immediateFailedFuture(failure))
        val template = UpsertPoints.newBuilder().setCollectionName("vectors").build()
        val points = (1L..3L).map { PointStruct.newBuilder().setId(id(it)).build() }
        val results = mutableListOf<UpdateResult>()
        val batchFailure = runCatching {
            client.upsertBatches(points.asFlow(), template, maxBatchItems = 1).collect { results.add(it) }
        }.exceptionOrNull()
        assertInstanceOf(IllegalStateException::class.java, batchFailure)
        (batchFailure?.message) shouldBeEqualTo failure.message
        (results.size) shouldBeEqualTo 1
        verify(exactly = 2) { client.upsertAsync(any<UpsertPoints>(), any<Duration>()) }
        val brokenInput = flow { emit(points.first()); throw failure }
        val inputFailure = runCatching { client.upsertBatches(brokenInput, template).toList() }.exceptionOrNull()
        assertInstanceOf(IllegalStateException::class.java, inputFailure)
        (inputFailure?.message) shouldBeEqualTo failure.message
        verify(exactly = 2) { client.upsertAsync(any<UpsertPoints>(), any<Duration>()) }
    }

    private fun id(value: Long): PointId = PointId.newBuilder().setNum(value).build()

    @Test
    fun `future completion races cancellation safely`() = runTest {
        val client = mockk<QdrantClient>()
        repeat(20) {
            val pending = SettableFuture.create<List<ScoredPoint>>()
            every { client.queryAsync(any<QueryPoints>(), any<Duration>()) } returns pending
            val job = launch { client.querySuspending(QueryPoints.getDefaultInstance()) }
            runCurrent()
            MultithreadingTester().workers(2).rounds(1)
                .add { pending.set(emptyList()) }
                .add { job.cancel() }
                .run()
            job.join()
            (pending.isDone).shouldBeTrue()
        }
    }

    @Test
    fun `slow collector delays next page and pending page is cancelled`() = runTest {
        val client = mockk<QdrantClient>()
        val pending = SettableFuture.create<ScrollResponse>()
        val first = ScrollResponse.newBuilder().addResult(RetrievedPoint.getDefaultInstance())
            .setNextPageOffset(id(2)).build()
        every { client.scrollAsync(any<ScrollPoints>(), any<Duration>()) } returnsMany
            listOf(Futures.immediateFuture(first), pending)
        val gate = CompletableDeferred<Unit>()
        val job = launch {
            client.scrollAsFlow(ScrollPoints.newBuilder().setLimit(1).build()).collect { gate.await() }
        }
        runCurrent()
        verify(exactly = 1) { client.scrollAsync(any<ScrollPoints>(), any<Duration>()) }
        gate.complete(Unit)
        runCurrent()
        verify(exactly = 2) { client.scrollAsync(any<ScrollPoints>(), any<Duration>()) }
        job.cancelAndJoin()
        (pending.isCancelled).shouldBeTrue()
        verify(exactly = 2) { client.scrollAsync(any<ScrollPoints>(), any<Duration>()) }
    }

    @Test
    fun `invalid bounds reject without RPC`() {
        val client = mockk<QdrantClient>()
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
            assertInstanceOf(IllegalArgumentException::class.java, runCatching(call).exceptionOrNull())
        }
        verify { client wasNot Called }
    }

    @Test
    fun `batch requests wait for completion and collector cancellation stops dispatch`() = runTest {
        val client = mockk<QdrantClient>()
        val pending = SettableFuture.create<UpdateResult>()
        every { client.upsertAsync(any<UpsertPoints>(), any<Duration>()) } returns pending
        val template = UpsertPoints.newBuilder().setCollectionName("vectors").build()
        val points = (1L..5L).map { PointStruct.newBuilder().setId(id(it)).build() }.asFlow()
        val result = async { client.upsertBatches(points, template, maxBatchItems = 1).take(1).toList() }
        runCurrent()
        verify(exactly = 1) { client.upsertAsync(any<UpsertPoints>(), any<Duration>()) }
        pending.set(UpdateResult.getDefaultInstance())
        result.await().size shouldBeEqualTo 1
        verify(exactly = 1) { client.upsertAsync(any<UpsertPoints>(), any<Duration>()) }
        verify(exactly = 0) { client.close() }
    }

    @Test
    fun `large batches complete within a bounded construction time`() {
        val client = mockk<QdrantClient>()
        every { client.upsertAsync(any<UpsertPoints>(), any<Duration>()) } returns
            Futures.immediateFuture(UpdateResult.getDefaultInstance())
        val template = UpsertPoints.newBuilder().setCollectionName("vectors").build()
        val points = (1L..50_000L).map { number ->
            PointStruct.newBuilder().setId(id(number)).setVectors(
                vectors(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f)
            ).build()
        }

        assertTimeoutPreemptively(Duration.ofSeconds(10)) {
            val result = runBlocking {
                client.upsertBatches(points.asFlow(), template, maxBatchItems = points.size).toList()
            }
            result.size shouldBeEqualTo 1
        }
    }

    @Test
    fun `varint length boundaries keep the exact serialized byte limit`() = runTest {
        val template = UpsertPoints.newBuilder().setCollectionName("vectors").setWait(true).build()
        val vectorData = FloatArray(32) { it.toFloat() }
        val points = listOf(127L, 128L).map { number ->
            PointStruct.newBuilder().setId(id(number)).setVectors(vectors(*vectorData)).build()
        }
        (points[0].serializedSize < points[1].serializedSize).shouldBeTrue()
        assertTrue(points.all { it.serializedSize >= 128 })

        val exactLimit = template.toBuilder().addAllPoints(points).build().serializedSize
        val requestsAtExactLimit = mutableListOf<UpsertPoints>()
        val exactClient = mockk<QdrantClient>()
        every { exactClient.upsertAsync(capture(requestsAtExactLimit), any<Duration>()) } returns
            Futures.immediateFuture(UpdateResult.getDefaultInstance())

        exactClient.upsertBatches(
            points.asFlow(),
            template,
            maxBatchItems = points.size,
            maxBatchBytes = exactLimit,
        ).toList()

        (requestsAtExactLimit.map { it.pointsCount }) shouldBeEqualTo listOf(2)
        (requestsAtExactLimit.single().serializedSize) shouldBeEqualTo exactLimit

        val requestsBelowLimit = mutableListOf<UpsertPoints>()
        val belowLimitClient = mockk<QdrantClient>()
        every { belowLimitClient.upsertAsync(capture(requestsBelowLimit), any<Duration>()) } returns
            Futures.immediateFuture(UpdateResult.getDefaultInstance())

        belowLimitClient.upsertBatches(
            points.asFlow(),
            template,
            maxBatchItems = points.size,
            maxBatchBytes = exactLimit - 1,
        ).toList()

        (requestsBelowLimit.map { it.pointsCount }) shouldBeEqualTo listOf(1, 1)
        (requestsBelowLimit.all { it.serializedSize <= exactLimit - 1 }).shouldBeTrue()
    }
}
