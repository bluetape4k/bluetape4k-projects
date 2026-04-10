package io.bluetape4k.batch.core.dsl

import io.bluetape4k.batch.api.BatchReader
import io.bluetape4k.batch.api.BatchReport
import io.bluetape4k.batch.api.BatchWriter
import io.bluetape4k.batch.api.SkipPolicy
import io.bluetape4k.batch.core.InMemoryBatchJobRepository
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.workflow.api.RetryPolicy
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldThrow
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

/**
 * [batchJob] DSL 단위 테스트.
 *
 * DSL 구성, 검증 오류, step 파이프라인 조합을 검증한다.
 */
class BatchDslTest {

    // ─── 기본 reader/writer stubs ────────────────────────────────────────────

    private val noopReader = object : BatchReader<String> {
        override suspend fun read(): String? = null
    }

    private val noopWriter = object : BatchWriter<String> {
        override suspend fun write(items: List<String>) {}
    }

    // ─── 1. 기본 DSL 구성 ────────────────────────────────────────────────────

    @Test
    fun `기본 DSL - batchJob과 단일 step 구성`() {
        val job = batchJob("myJob") {
            step<String, String>("step1") {
                reader(noopReader)
                writer(noopWriter)
            }
        }

        job.shouldNotBeNull()
        job.name shouldBeEqualTo "myJob"
        job.steps.size shouldBeEqualTo 1
        job.steps[0].name shouldBeEqualTo "step1"
    }

    // ─── 2. 다중 step ─────────────────────────────────────────────────────────

    @Test
    fun `다중 step - 순서대로 등록됨`() {
        val job = batchJob("multiStepJob") {
            step<String, String>("step1") {
                reader(noopReader)
                writer(noopWriter)
            }
            step<String, String>("step2") {
                reader(noopReader)
                writer(noopWriter)
            }
            step<String, String>("step3") {
                reader(noopReader)
                writer(noopWriter)
            }
        }

        job.steps.size shouldBeEqualTo 3
        job.steps.map { it.name } shouldBeEqualTo listOf("step1", "step2", "step3")
    }

    // ─── 3. params 설정 ──────────────────────────────────────────────────────

    @Test
    fun `params vararg - Job 파라미터 설정`() {
        val job = batchJob("paramJob") {
            params("date" to "2026-04-10", "env" to "prod")
            step<String, String>("step1") {
                reader(noopReader)
                writer(noopWriter)
            }
        }

        job.params["date"] shouldBeEqualTo "2026-04-10"
        job.params["env"] shouldBeEqualTo "prod"
    }

    @Test
    fun `params map - Job 파라미터 Map으로 설정`() {
        val job = batchJob("paramJob") {
            params(mapOf("k1" to 42, "k2" to true))
            step<String, String>("step1") {
                reader(noopReader)
                writer(noopWriter)
            }
        }

        job.params["k1"] shouldBeEqualTo 42
        job.params["k2"] shouldBeEqualTo true
    }

    // ─── 4. repository 설정 ──────────────────────────────────────────────────

    @Test
    fun `repository - 커스텀 repository 주입`() {
        val customRepo = InMemoryBatchJobRepository()
        val job = batchJob("repoJob") {
            repository(customRepo)
            step<String, String>("step1") {
                reader(noopReader)
                writer(noopWriter)
            }
        }

        job.shouldNotBeNull()  // 빌드 성공으로 충분
    }

    // ─── 5. chunkSize 설정 ──────────────────────────────────────────────────

    @Test
    fun `chunkSize - step 청크 크기 설정`() {
        val job = batchJob("chunkJob") {
            step<String, String>("step1") {
                reader(noopReader)
                writer(noopWriter)
                chunkSize(500)
            }
        }

        job.steps[0].chunkSize shouldBeEqualTo 500
    }

    // ─── 6. skipPolicy 설정 ──────────────────────────────────────────────────

    @Test
    fun `skipPolicy - ALL 정책 설정`() {
        val job = batchJob("skipJob") {
            step<String, String>("step1") {
                reader(noopReader)
                writer(noopWriter)
                skipPolicy(SkipPolicy.ALL)
            }
        }

        job.steps[0].skipPolicy.shouldSkip(RuntimeException("x"), 0L) shouldBe true
    }

    // ─── 7. processor 람다 ───────────────────────────────────────────────────

    @Test
    fun `processor 람다 - I에서 O로 변환`() {
        val job = batchJob("processorJob") {
            step<Int, String>("step1") {
                reader(object : BatchReader<Int> {
                    override suspend fun read(): Int? = null
                })
                processor { n -> n.toString() }
                writer(object : BatchWriter<String> {
                    override suspend fun write(items: List<String>) {}
                })
            }
        }

        job.steps[0].processor.shouldNotBeNull()
    }

    // ─── 8. retryPolicy 설정 ─────────────────────────────────────────────────

    @Test
    fun `retryPolicy - 재시도 정책 설정`() {
        val job = batchJob("retryJob") {
            step<String, String>("step1") {
                reader(noopReader)
                writer(noopWriter)
                retryPolicy(RetryPolicy(maxAttempts = 3, delay = 100.seconds))
            }
        }

        job.steps[0].retryPolicy.maxAttempts shouldBeEqualTo 3
    }

    // ─── 9. 검증 오류 - name blank ───────────────────────────────────────────

    @Test
    fun `빈 job name - IllegalArgumentException 발생`() {
        invoking {
            batchJob("") {
                step<String, String>("step1") {
                    reader(noopReader)
                    writer(noopWriter)
                }
            }
        } shouldThrow IllegalArgumentException::class
    }

    // ─── 10. 검증 오류 - step 없음 ──────────────────────────────────────────

    @Test
    fun `step 없음 - IllegalArgumentException 발생`() {
        invoking {
            batchJob("noStepJob") {
                // step 블록 없음
            }
        } shouldThrow IllegalArgumentException::class
    }

    // ─── 11. 검증 오류 - reader 없음 ────────────────────────────────────────

    @Test
    fun `reader 없음 - IllegalArgumentException 발생`() {
        invoking {
            batchJob("noReaderJob") {
                step<String, String>("step1") {
                    writer(noopWriter)
                    // reader 없음
                }
            }
        } shouldThrow IllegalArgumentException::class
    }

    // ─── 12. 검증 오류 - writer 없음 ────────────────────────────────────────

    @Test
    fun `writer 없음 - IllegalArgumentException 발생`() {
        invoking {
            batchJob("noWriterJob") {
                step<String, String>("step1") {
                    reader(noopReader)
                    // writer 없음
                }
            }
        } shouldThrow IllegalArgumentException::class
    }

    // ─── 13. 실제 실행 - DSL로 빌드한 Job 실행 ──────────────────────────────

    @Test
    fun `DSL 빌드 Job 실행 - COMPLETED 반환`() = runSuspendIO {
        val items = listOf("a", "b", "c")
        val collected = mutableListOf<String>()

        val job = batchJob("runJob") {
            step<String, String>("step1") {
                reader(object : BatchReader<String> {
                    private val queue = ArrayDeque(items)
                    override suspend fun read(): String? = queue.removeFirstOrNull()
                })
                writer(object : BatchWriter<String> {
                    override suspend fun write(chunks: List<String>) { collected.addAll(chunks) }
                })
                chunkSize(2)
            }
        }

        val report = job.run()

        report shouldBeInstanceOf BatchReport.Success::class
        report.stepReports.size shouldBeEqualTo 1
        report.stepReports[0].readCount shouldBeEqualTo 3L
        collected shouldBeEqualTo items
    }
}
