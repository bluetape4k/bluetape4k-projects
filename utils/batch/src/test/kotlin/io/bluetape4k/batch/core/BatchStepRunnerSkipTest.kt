package io.bluetape4k.batch.core

import io.bluetape4k.batch.api.BatchProcessor
import io.bluetape4k.batch.api.BatchReader
import io.bluetape4k.batch.api.BatchStatus
import io.bluetape4k.batch.api.BatchWriter
import io.bluetape4k.batch.api.JobExecution
import io.bluetape4k.batch.api.SkipPolicy
import io.bluetape4k.batch.api.StepReport
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.workflow.api.RetryPolicy
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

/**
 * [BatchStepRunner]의 skip 정책(`SkipPolicy`) 동작 검증 테스트.
 *
 * ## 검증 시나리오
 * 1. `maxSkips(n)` 정책 — n개의 skip 허용 후 다음 청크 실패 시 FAILED 전환.
 * 2. 모든 청크 skip — `SkipPolicy.ALL` 적용 시 `COMPLETED_WITH_SKIPS` + 누적 skipCount.
 * 3. processor 예외 격리 — processor에서 예외가 발생해도 reader는 모든 아이템을 읽어야 한다.
 *
 * ## 핵심 동작 메모
 * - **processor skip**: `skipCount += 1` (개별 아이템).
 * - **writer retry 소진 후 skip**: `skipCount += chunk.size` (청크 전체).
 * - `maxSkips(n)`은 `skipCount < n` 으로 평가되므로 skipCount 가 n에 도달하면 추가 skip 불가 → 다음 실패는 FAILED.
 */
class BatchStepRunnerSkipTest {

    companion object: KLogging() {
        private const val MAX_SKIP_COUNT_FOR_FAILURE: Long = 3L
        private const val SKIP_TEST_ITEM_COUNT = 5
        private const val ACCUMULATE_TEST_ITEM_COUNT = 3
        private const val PROCESSOR_TEST_ITEM_COUNT = 4
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    /**
     * read 호출 횟수를 기록하는 fake reader.
     *
     * @param items 사전 적재 아이템 시퀀스
     */
    class CountingListReader<T: Any>(items: List<T>): BatchReader<T> {
        private val queue = ArrayDeque(items)
        private val _readCount = AtomicInteger(0)

        /** 지금까지 read()로 반환한 non-null 아이템 수. */
        val readCount: Int get() = _readCount.get()

        override suspend fun open() = Unit
        override suspend fun close() = Unit

        override suspend fun read(): T? = queue.removeFirstOrNull()?.also {
            _readCount.incrementAndGet()
        }

        override suspend fun checkpoint(): Any? = null
        override suspend fun onChunkCommitted() = Unit
    }

    /** 호출될 때마다 항상 예외를 던지는 writer. */
    class AlwaysFailWriter<T: Any>: BatchWriter<T> {
        override suspend fun write(items: List<T>) {
            throw RuntimeException("항상 실패")
        }
    }

    /** 어떤 아이템이든 수집하는 fake writer. */
    class CollectingWriter<T: Any>: BatchWriter<T> {
        val collected = mutableListOf<T>()
        override suspend fun write(items: List<T>) {
            collected.addAll(items)
        }
    }

    /** 기본 JobExecution 팩토리. */
    private fun makeJobExecution(): JobExecution = JobExecution(
        id = 1L,
        jobName = "skipTestJob",
        params = emptyMap(),
        status = BatchStatus.RUNNING,
        startTime = Instant.now(),
    )

    /** 단발성 BatchStepRunner 실행 헬퍼. */
    private suspend fun <I: Any, O: Any> runStep(step: BatchStep<I, O>): StepReport {
        val repo = InMemoryBatchJobRepository()
        val je = makeJobExecution()
        return BatchStepRunner(step, je, repo).run()
    }

    // ─── 1. maxSkips(n) 초과 시 FAILED ────────────────────────────────────────

    /**
     * `maxSkips(3)` 정책 + `chunkSize = 1` + `AlwaysFailWriter` + 5개 아이템 시나리오.
     *
     * 동작:
     * - 청크 1~3 (각각 1 아이템): writer 실패 → `shouldSkip(e, 0/1/2) = (n < 3)` true → skip → skipCount 누적 1, 2, 3
     * - 청크 4: writer 실패 → `shouldSkip(e, 3) = (3 < 3)` false → 예외 throw → step FAILED
     *
     * 기대 결과:
     * - status = `FAILED`
     * - skipCount = 3 (실패 직전까지 누적)
     */
    @Test
    fun `maxSkips(n) 초과 시 step status 는 FAILED`() = runSuspendIO {
        val items = (1..SKIP_TEST_ITEM_COUNT).map { "item-$it" }
        val reader = CountingListReader(items)
        val writer = AlwaysFailWriter<String>()

        val step = BatchStep(
            name = "maxSkipsStep",
            chunkSize = 1,
            reader = reader,
            writer = writer,
            skipPolicy = SkipPolicy.maxSkips(MAX_SKIP_COUNT_FOR_FAILURE),
            retryPolicy = RetryPolicy.NONE,
            commitTimeout = 5.seconds,
        )

        val report = runStep(step)

        report.status shouldBe BatchStatus.FAILED
        report.skipCount shouldBeEqualTo MAX_SKIP_COUNT_FOR_FAILURE
        report.error.shouldNotBeNull()
    }

    // ─── 2. 청크 단위 skip 카운트 누적 ────────────────────────────────────────

    /**
     * `SkipPolicy.ALL` + `chunkSize = 1` + 3개 아이템 + `AlwaysFailWriter` 시나리오.
     *
     * 동작:
     * - 모든 청크에서 writer 실패 → 항상 skip 허용 → skipCount += chunk.size(=1) × 3
     *
     * 기대 결과:
     * - status = `COMPLETED_WITH_SKIPS`
     * - skipCount = chunkSize(1) × chunks(3) = 3
     * - writeCount = 0
     */
    @Test
    fun `skip 카운트가 step report 에 누적된다`() = runSuspendIO {
        val items = (1..ACCUMULATE_TEST_ITEM_COUNT).map { "row-$it" }
        val reader = CountingListReader(items)
        val writer = AlwaysFailWriter<String>()

        val step = BatchStep(
            name = "skipAccumulateStep",
            chunkSize = 1,
            reader = reader,
            writer = writer,
            skipPolicy = SkipPolicy.ALL,
            retryPolicy = RetryPolicy.NONE,
            commitTimeout = 5.seconds,
        )

        val report = runStep(step)

        report.status shouldBe BatchStatus.COMPLETED_WITH_SKIPS
        report.skipCount shouldBeEqualTo ACCUMULATE_TEST_ITEM_COUNT.toLong()
        report.writeCount shouldBeEqualTo 0L
        report.readCount shouldBeEqualTo ACCUMULATE_TEST_ITEM_COUNT.toLong()
    }

    // ─── 3. processor 예외 시 reader 는 영향받지 않음 ──────────────────────────

    /**
     * processor가 특정 아이템(2번째)에서 예외 + `SkipPolicy.ALL` 시나리오.
     *
     * 동작:
     * - 4개 아이템을 chunkSize=4로 한 청크에 수집
     * - processor: 2번째 아이템에서 예외 → skip 허용 → skipCount = 1
     * - writer는 살아남은 3개 아이템 수신
     * - reader는 모든 아이템(4개)을 read 함 → 예외에 영향받지 않음
     */
    @Test
    fun `processor 예외 시 reader 는 영향받지 않음`() = runSuspendIO {
        val items = (1..PROCESSOR_TEST_ITEM_COUNT).map { "src-$it" }
        val reader = CountingListReader(items)
        val writer = CollectingWriter<String>()

        val processor = BatchProcessor<String, String> { item ->
            if (item == "src-2") {
                throw IllegalStateException("bad item: $item")
            }
            item
        }

        val step = BatchStep(
            name = "processorSkipStep",
            chunkSize = PROCESSOR_TEST_ITEM_COUNT,
            reader = reader,
            writer = writer,
            processor = processor,
            skipPolicy = SkipPolicy.ALL,
            retryPolicy = RetryPolicy.NONE,
            commitTimeout = 5.seconds,
        )

        val report = runStep(step)

        // reader는 모든 아이템을 읽어야 함 — processor 예외에 영향받지 않음
        reader.readCount shouldBeEqualTo PROCESSOR_TEST_ITEM_COUNT
        report.readCount shouldBeEqualTo PROCESSOR_TEST_ITEM_COUNT.toLong()

        // 1개 skip → 3개 write
        report.status shouldBe BatchStatus.COMPLETED_WITH_SKIPS
        report.skipCount shouldBeEqualTo 1L
        report.writeCount shouldBeEqualTo (PROCESSOR_TEST_ITEM_COUNT - 1).toLong()

        // writer는 예외 발생 아이템(src-2) 외 모든 아이템 수신
        writer.collected shouldBeEqualTo listOf("src-1", "src-3", "src-4")
    }
}
