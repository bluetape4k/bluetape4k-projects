package io.bluetape4k.batch.core

import io.bluetape4k.batch.api.BatchJobRepository
import io.bluetape4k.batch.api.BatchStatus
import io.bluetape4k.batch.api.JobExecution
import io.bluetape4k.batch.api.StepReport
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * [BatchStep]의 chunk 루프 실행 엔진.
 *
 * ## 책임
 * - [BatchJobRepository]와 협력하여 [io.bluetape4k.batch.api.StepExecution]을 조회/생성하고,
 *   이미 완료된 Step은 **즉시** 기존 리포트를 반환한다.
 * - `reader.open()` → (옵션) `restoreFrom(checkpoint)` → chunk 루프 → `writer.close()` /
 *   `reader.close()` 순으로 실행한다.
 * - chunk 단위로 [writeWithTimeout] 기반 write를 수행하고, [io.bluetape4k.workflow.api.RetryPolicy]에 따라
 *   재시도 및 지수 백오프를 적용한다.
 * - chunk-level retry 소진 시 [io.bluetape4k.batch.api.SkipPolicy]를 참조하여 skip 또는 Step FAILED를 결정한다.
 *
 * ## 불변 계약
 * 1. [BatchJobRepository.findOrCreateStepExecution] 결과가 `COMPLETED` 또는 `COMPLETED_WITH_SKIPS`면
 *    reader/writer를 **절대 open하지 않고**, checkpoint 복원도 수행하지 않는다.
 * 2. [CancellationException]은 **절대 삼키지 않는다** — STOPPED 상태 저장 후 항상 즉시 재던진다.
 * 3. `reader.close()` / `writer.close()`는 `finally`의 [NonCancellable] 컨텍스트에서
 *    각각 독립된 `runCatching`으로 실행된다.
 * 4. EOF 판정은 `chunk.isEmpty()`가 아닌 `eofReached` 플래그로 판단한다 — 전부 필터링된 경우와 구분한다.
 * 5. [BatchJobRepository.loadCheckpoint] 결과가 null이면 [io.bluetape4k.batch.api.BatchReader.restoreFrom]을
 *    호출하지 않는다.
 *
 * @param I Reader 출력 타입
 * @param O Writer 입력 타입
 * @property step 실행할 [BatchStep]
 * @property jobExecution 소속 [JobExecution]
 * @property repository 실행 이력을 저장할 [BatchJobRepository]
 */
internal class BatchStepRunner<I : Any, O : Any>(
    private val step: BatchStep<I, O>,
    private val jobExecution: JobExecution,
    private val repository: BatchJobRepository,
) {
    companion object : KLoggingChannel()

    /**
     * [BatchStep]을 실행하고 결과 [StepReport]를 반환한다.
     *
     * @return 실행 결과 [StepReport]
     */
    suspend fun run(): StepReport {
        val stepExecution = repository.findOrCreateStepExecution(jobExecution, step.name)

        // (1) 이미 완료된 Step은 즉시 기존 리포트 반환 — reader/writer open 및 checkpoint 복원 금지
        if (stepExecution.status == BatchStatus.COMPLETED ||
            stepExecution.status == BatchStatus.COMPLETED_WITH_SKIPS
        ) {
            log.debug { "Step 이미 완료됨 — 즉시 skip: step=${step.name}, status=${stepExecution.status}" }
            return StepReport(
                stepName = step.name,
                status = stepExecution.status,
                readCount = stepExecution.readCount,
                writeCount = stepExecution.writeCount,
                skipCount = stepExecution.skipCount,
                checkpoint = stepExecution.checkpoint,
            )
        }

        var readCount = stepExecution.readCount
        var writeCount = stepExecution.writeCount
        var skipCount = stepExecution.skipCount

        try {
            step.reader.open()
            step.writer.open()

            // (2) checkpoint 조회 — null이 아닐 때만 restoreFrom 호출
            val checkpoint = repository.loadCheckpoint(stepExecution.id)
            if (checkpoint != null) {
                log.debug { "체크포인트 복원: step=${step.name}, checkpoint=$checkpoint" }
                step.reader.restoreFrom(checkpoint)
            }

            var eofReached = false

            mainLoop@ while (!eofReached) {
                val chunk = mutableListOf<O>()

                // chunk 수집 (reader → processor → chunk)
                collectLoop@ for (i in 0 until step.chunkSize) {
                    val item = step.reader.read()
                    if (item == null) {
                        eofReached = true
                        break@collectLoop
                    }
                    readCount++

                    val processed: O? = if (step.processor == null) {
                        @Suppress("UNCHECKED_CAST")
                        item as O
                    } else {
                        try {
                            step.processor.process(item)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Throwable) {
                            if (step.skipPolicy.shouldSkip(e, skipCount)) {
                                skipCount++
                                log.warn(e) {
                                    "processor.process() 실패 — skip: step=${step.name}, skipCount=$skipCount"
                                }
                                null
                            } else {
                                throw e
                            }
                        }
                    }

                    if (processed != null) {
                        chunk.add(processed)
                    }
                }

                // EOF + 빈 chunk → 루프 종료
                if (chunk.isEmpty() && eofReached) break@mainLoop
                // 전부 필터링되었지만 EOF는 아님 → 다음 윈도우
                if (chunk.isEmpty()) continue@mainLoop

                // (3) writer + retry 루프
                var attempts = 0
                var currentDelay = step.retryPolicy.delay
                writerLoop@ while (true) {
                    attempts++
                    try {
                        writeWithTimeout(step.writer, chunk, step.commitTimeout)
                        step.reader.onChunkCommitted()
                        step.reader.checkpoint()?.let { cp ->
                            repository.saveCheckpoint(stepExecution.id, cp)
                        }
                        writeCount += chunk.size
                        break@writerLoop
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        if (attempts < step.retryPolicy.maxAttempts) {
                            log.warn(e) {
                                "writer.write() 실패 — 재시도 예정: step=${step.name}, " +
                                    "attempt=$attempts/${step.retryPolicy.maxAttempts}, delay=$currentDelay"
                            }
                            if (currentDelay.isPositive()) {
                                delay(currentDelay)
                            }
                            currentDelay = minOf(
                                currentDelay * step.retryPolicy.backoffMultiplier,
                                step.retryPolicy.maxDelay,
                            )
                            continue@writerLoop
                        }

                        // retry 소진 → chunk-level skipPolicy 평가
                        if (step.skipPolicy.shouldSkip(e, skipCount)) {
                            skipCount += chunk.size
                            log.warn(e) {
                                "writer.write() retry 소진 — chunk skip: step=${step.name}, " +
                                    "chunkSize=${chunk.size}, skipCount=$skipCount"
                            }
                            break@writerLoop
                        }
                        throw e
                    }
                }
            }

            val finalStatus = if (skipCount > 0) BatchStatus.COMPLETED_WITH_SKIPS
            else BatchStatus.COMPLETED

            val stepReport = StepReport(
                stepName = step.name,
                status = finalStatus,
                readCount = readCount,
                writeCount = writeCount,
                skipCount = skipCount,
                checkpoint = step.reader.checkpoint(),
            )
            repository.completeStepExecution(stepExecution, stepReport)
            return stepReport
        } catch (e: CancellationException) {
            // 취소 → STOPPED 저장 후 즉시 재던짐 — 절대 삼키지 않음
            withContext(NonCancellable) {
                val stoppedReport = StepReport(
                    stepName = step.name,
                    status = BatchStatus.STOPPED,
                    readCount = readCount,
                    writeCount = writeCount,
                    skipCount = skipCount,
                    checkpoint = runCatching { step.reader.checkpoint() }.getOrNull(),
                )
                runCatching { repository.completeStepExecution(stepExecution, stoppedReport) }
                    .onFailure { t ->
                        log.warn(t) { "STOPPED 상태 저장 실패 — step=${step.name}" }
                    }
            }
            throw e
        } catch (e: Throwable) {
            val failedReport = StepReport(
                stepName = step.name,
                status = BatchStatus.FAILED,
                readCount = readCount,
                writeCount = writeCount,
                skipCount = skipCount,
                error = e,
            )
            runCatching { repository.completeStepExecution(stepExecution, failedReport) }
                .onFailure { t ->
                    log.warn(t) { "FAILED 상태 저장 실패 — step=${step.name}" }
                }
            return failedReport
        } finally {
            withContext(NonCancellable) {
                runCatching { step.reader.close() }
                    .onFailure { t -> log.warn(t) { "reader close 실패 — step=${step.name}" } }
                runCatching { step.writer.close() }
                    .onFailure { t -> log.warn(t) { "writer close 실패 — step=${step.name}" } }
            }
        }
    }
}
