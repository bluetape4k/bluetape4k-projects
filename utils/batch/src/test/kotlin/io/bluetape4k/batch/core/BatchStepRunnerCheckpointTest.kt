package io.bluetape4k.batch.core

import io.bluetape4k.batch.api.BatchJobRepository
import io.bluetape4k.batch.api.BatchReader
import io.bluetape4k.batch.api.BatchStatus
import io.bluetape4k.batch.api.BatchWriter
import io.bluetape4k.batch.api.JobExecution
import io.bluetape4k.batch.api.StepExecution
import io.bluetape4k.batch.api.StepReport
import io.bluetape4k.logging.KLogging
import io.bluetape4k.workflow.api.RetryPolicy
import kotlinx.coroutines.test.runTest
import io.bluetape4k.assertions.shouldBe
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

/**
 * [BatchStepRunner]의 checkpoint 갱신/복원 로직 검증 테스트.
 *
 * ## 검증 시나리오
 * 1. **재시작 시 checkpoint 부터 처리**: `loadCheckpoint()`가 non-null 값을 반환하면
 *    `reader.restoreFrom(checkpoint)`가 호출되어 중복 처리 없이 진행된다.
 * 2. **chunk 단위 saveCheckpoint 호출**: 각 chunk 커밋 시점에 `saveCheckpoint()`가
 *    chunk 개수만큼 호출된다.
 * 3. **saveCheckpoint 실패 시 예외 전파**: `saveCheckpoint()`가 throw 하면 step은
 *    FAILED 상태로 종료된다 (BatchStepRunner의 catch (Throwable) 분기).
 *
 * @see BatchStepRunner
 * @see BatchJobRepository.saveCheckpoint
 * @see BatchJobRepository.loadCheckpoint
 */
class BatchStepRunnerCheckpointTest {

    companion object: KLogging()

    /**
     * 시작 인덱스를 [restoreFrom]으로 받아 그 다음 위치부터 읽는 fake reader.
     *
     * - `read()` 호출 시 현재 인덱스의 아이템을 반환하고 인덱스를 1 증가시킨다.
     * - `checkpoint()`는 마지막으로 커밋된 위치를 반환한다.
     * - `onChunkCommitted()` 호출 시 lastCommittedIndex를 현재 인덱스 -1로 전진시킨다.
     */
    private class IndexedListReader(
        private val items: List<String>,
    ): BatchReader<String> {
        private var nextIndex: Int = 0
        private var lastCommittedIndex: Int = -1
        var restoredFromValue: Any? = null
            private set
        var openCount: Int = 0
            private set

        override suspend fun open() {
            openCount++
        }

        override suspend fun restoreFrom(checkpoint: Any) {
            restoredFromValue = checkpoint
            // checkpoint는 "마지막으로 커밋된 인덱스" — 다음 read는 그 이후부터
            val cp = (checkpoint as Number).toInt()
            nextIndex = cp + 1
            lastCommittedIndex = cp
        }

        override suspend fun read(): String? {
            if (nextIndex >= items.size) return null
            return items[nextIndex++]
        }

        override suspend fun checkpoint(): Any? =
            if (lastCommittedIndex >= 0) lastCommittedIndex else null

        override suspend fun onChunkCommitted() {
            // chunk가 커밋되었으므로 가장 최근에 read한 인덱스를 commit된 것으로 표시
            lastCommittedIndex = nextIndex - 1
        }
    }

    /** 수집용 fake writer. */
    private class CollectingWriter: BatchWriter<String> {
        val collected = mutableListOf<String>()
        override suspend fun write(items: List<String>) {
            collected.addAll(items)
        }
    }

    /**
     * [InMemoryBatchJobRepository]를 위임하면서 saveCheckpoint 호출 횟수를 카운팅하는 래퍼.
     */
    private class CountingRepository(
        private val delegate: InMemoryBatchJobRepository = InMemoryBatchJobRepository(),
    ): BatchJobRepository by delegate {
        val saveCheckpointCount = AtomicInteger(0)
        val savedValues = mutableListOf<Any>()

        override suspend fun saveCheckpoint(stepExecutionId: Long, checkpoint: Any) {
            saveCheckpointCount.incrementAndGet()
            savedValues.add(checkpoint)
            delegate.saveCheckpoint(stepExecutionId, checkpoint)
        }
    }

    /**
     * saveCheckpoint 호출 시 [RuntimeException]을 throw 하는 래퍼.
     */
    private class FailingCheckpointRepository(
        private val delegate: InMemoryBatchJobRepository = InMemoryBatchJobRepository(),
    ): BatchJobRepository by delegate {
        override suspend fun saveCheckpoint(stepExecutionId: Long, checkpoint: Any) {
            throw RuntimeException("checkpoint save failed")
        }
    }

    /**
     * 미리 checkpoint를 주입할 수 있는 [InMemoryBatchJobRepository] 래퍼.
     *
     * `loadCheckpoint(stepExecutionId)` 호출 시 첫 번째 호출에 한해 [preloadedCheckpoint]를
     * 반환한다. (실제로는 stepExecution이 동적으로 생성되어 ID를 미리 알 수 없으므로
     * 인터셉트 방식 사용.)
     */
    private class PreloadedCheckpointRepository(
        private val delegate: InMemoryBatchJobRepository = InMemoryBatchJobRepository(),
        private val preloadedCheckpoint: Any?,
    ): BatchJobRepository by delegate {
        override suspend fun loadCheckpoint(stepExecutionId: Long): Any? {
            // delegate 가 가진 값 우선, 없으면 preloaded 사용
            return delegate.loadCheckpoint(stepExecutionId) ?: preloadedCheckpoint
        }
    }

    private fun makeJobExecution(): JobExecution = JobExecution(
        id = 1L,
        jobName = "checkpointTestJob",
        params = emptyMap(),
        status = BatchStatus.RUNNING,
        startTime = Instant.now(),
    )

    private fun makeStep(
        chunkSize: Int,
        reader: BatchReader<String>,
        writer: BatchWriter<String>,
        name: String = "checkpointStep",
    ): BatchStep<String, String> = BatchStep(
        name = name,
        chunkSize = chunkSize,
        reader = reader,
        writer = writer,
        processor = null,
        skipPolicy = io.bluetape4k.batch.api.SkipPolicy.NONE,
        retryPolicy = RetryPolicy.NONE,
        commitTimeout = 5.seconds,
    )

    /**
     * checkpoint 가 저장된 상태에서 재시작하면, reader.restoreFrom() 이 호출되어
     * checkpoint 이후 위치부터 처리된다.
     *
     * ### 시나리오
     * - 5 아이템 ("a","b","c","d","e"), chunkSize=2 로 첫 실행 → checkpoint=3 저장
     * - 두 번째 실행: loadCheckpoint() 가 3 반환 → restoreFrom(3) 호출
     * - 두 번째 실행은 인덱스 4 ("e") 만 처리 → readCount=1, writeCount=1
     */
    @Test
    fun `checkpoint 에서 재시작 시 lastOffset 부터 처리`() = runTest(timeout = 30.seconds) {
        val sourceItems = listOf("a", "b", "c", "d", "e")
        val sharedDelegate = InMemoryBatchJobRepository()

        // ── 첫 실행: 5 아이템 모두 처리되며 checkpoint 가 저장된다. ──
        val firstReader = IndexedListReader(sourceItems)
        val firstWriter = CollectingWriter()
        val firstStep = makeStep(chunkSize = 2, reader = firstReader, writer = firstWriter)
        val firstJe = sharedDelegate.findOrCreateJobExecution("checkpointTestJob")
        val firstReport = BatchStepRunner(firstStep, firstJe, sharedDelegate).run()

        firstReport.status shouldBe BatchStatus.COMPLETED
        firstReport.readCount shouldBeEqualTo 5L
        firstReport.checkpoint.shouldNotBeNull()
        // 마지막 커밋 인덱스는 4 (0-based, "e")
        (firstReport.checkpoint as Number).toInt() shouldBeEqualTo 4

        // ── 두 번째 실행: 동일 step 을 재시작했다고 가정. 새 StepExecution 을 RUNNING 으로 강제. ──
        // findOrCreateStepExecution 이 COMPLETED 면 즉시 skip 하므로 우회 위해 새 stepName 사용 +
        // 사전에 checkpoint 를 해당 stepExecutionId 에 등록한다.
        val secondReader = IndexedListReader(sourceItems)
        val secondWriter = CollectingWriter()
        val secondStep = makeStep(
            chunkSize = 2,
            reader = secondReader,
            writer = secondWriter,
            name = "checkpointStepResume",
        )
        val secondJe = sharedDelegate.findOrCreateJobExecution("checkpointTestJob")
        // secondStep 의 StepExecution 을 미리 생성하고 checkpoint 를 주입
        val seededSe: StepExecution = sharedDelegate.findOrCreateStepExecution(secondJe, secondStep.name)
        sharedDelegate.saveCheckpoint(seededSe.id, 3) // 인덱스 3 까지 커밋된 것으로 가정

        val secondReport = BatchStepRunner(secondStep, secondJe, sharedDelegate).run()

        secondReport.status shouldBe BatchStatus.COMPLETED
        // restoreFrom(3) 호출되어 인덱스 4 ("e") 만 처리됨
        secondReader.restoredFromValue shouldBeEqualTo 3
        secondReport.readCount shouldBeEqualTo 1L
        secondReport.writeCount shouldBeEqualTo 1L
        secondWriter.collected shouldBeEqualTo listOf("e")
    }

    /**
     * chunkSize 단위로 chunk 가 커밋될 때마다 [BatchJobRepository.saveCheckpoint] 가 호출된다.
     *
     * ### 시나리오
     * - 6 아이템, chunkSize=2 → 3 chunks
     * - 각 chunk 커밋 시 reader.checkpoint() 가 non-null 이므로 saveCheckpoint() 가 3회 호출됨
     */
    @Test
    fun `checkpoint 가 chunk 단위로 saveCheckpoint 를 호출`() = runTest(timeout = 30.seconds) {
        val items = listOf("a", "b", "c", "d", "e", "f")
        val reader = IndexedListReader(items)
        val writer = CollectingWriter()
        val countingRepo = CountingRepository()

        val step = makeStep(chunkSize = 2, reader = reader, writer = writer)
        val je = countingRepo.findOrCreateJobExecution("checkpointCountJob")

        val report = BatchStepRunner(step, je, countingRepo).run()

        report.status shouldBe BatchStatus.COMPLETED
        report.readCount shouldBeEqualTo 6L
        report.writeCount shouldBeEqualTo 6L

        // 6 / 2 = 3 chunks → saveCheckpoint 3회
        countingRepo.saveCheckpointCount.get() shouldBeEqualTo 3
        countingRepo.savedValues.size shouldBeEqualTo 3
        // 저장된 checkpoint 들은 단조 증가해야 함 (인덱스 1, 3, 5)
        countingRepo.savedValues.forEachIndexed { idx, cp ->
            val cpInt = (cp as Number).toInt()
            cpInt shouldBeGreaterOrEqualTo (idx * 2 + 1)
        }
    }

    /**
     * [BatchJobRepository.saveCheckpoint] 가 throw 하면 BatchStepRunner 는 catch(Throwable)
     * 분기로 진입하여 [StepReport.status] = FAILED, [StepReport.error] non-null 로 종료한다.
     *
     * BatchStepRunner.run() 의 동작: 일반 Throwable 은 try 블록 밖의 `catch (e: Throwable)` 에서
     * `failedReport` 를 만들어 **return** 한다 (예외 재던지지 않음).
     */
    @Test
    fun `saveCheckpoint 실패 시 예외 전파`() = runTest(timeout = 30.seconds) {
        val items = listOf("a", "b")
        val reader = IndexedListReader(items)
        val writer = CollectingWriter()
        val failingRepo = FailingCheckpointRepository()

        val step = makeStep(chunkSize = 2, reader = reader, writer = writer)
        val je = failingRepo.findOrCreateJobExecution("checkpointFailJob")

        val report = BatchStepRunner(step, je, failingRepo).run()

        // BatchStepRunner.run() 은 RuntimeException 을 catch 하여 FAILED 리포트를 반환한다.
        report.status shouldBe BatchStatus.FAILED
        report.error.shouldNotBeNull()
        report.error!!.message shouldBeEqualTo "checkpoint save failed"
        // chunk 자체는 writer 까지 도달했으므로 writer 는 데이터를 받았을 수 있음
        // 단, writeCount 는 saveCheckpoint 실패 직전에는 아직 증가하지 않았다 (BatchStepRunner.run 의
        // try 블록 순서: write → onChunkCommitted → checkpoint → saveCheckpoint → writeCount += chunk.size)
        report.writeCount shouldBeEqualTo 0L
    }
}
