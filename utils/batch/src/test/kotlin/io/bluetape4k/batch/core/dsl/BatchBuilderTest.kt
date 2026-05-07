package io.bluetape4k.batch.core.dsl

import io.bluetape4k.batch.BatchDefaults
import io.bluetape4k.batch.api.BatchReader
import io.bluetape4k.batch.api.BatchReport
import io.bluetape4k.batch.api.BatchWriter
import io.bluetape4k.batch.api.SkipPolicy
import io.bluetape4k.batch.core.BatchStep
import io.bluetape4k.batch.core.InMemoryBatchJobRepository
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.workflow.api.RetryPolicy
import io.bluetape4k.assertions.shouldBe
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds
import io.bluetape4k.assertions.assertFailsWith

/**
 * [BatchJobBuilder] 및 [BatchStepBuilder] DSL 빌더 검증 테스트.
 */
class BatchBuilderTest {

    companion object : KLogging()

    // ─── 공통 stub ────────────────────────────────────────────────────────────

    private val noopReader = object : BatchReader<String> {
        override suspend fun read(): String? = null
    }

    private val noopWriter = object : BatchWriter<String> {
        override suspend fun write(items: List<String>) {}
    }

    // ─── BatchStepBuilder 단위 테스트 ─────────────────────────────────────────

    @Test
    fun `BatchStepBuilder - 기본 빌드 성공`() {
        val step = BatchStepBuilder<String, String>("step1").apply {
            reader(noopReader)
            writer(noopWriter)
        }.build()

        step.shouldNotBeNull()
        step.name shouldBeEqualTo "step1"
        step.chunkSize shouldBeEqualTo BatchDefaults.CHUNK_SIZE
        step.processor.shouldBeNull()
        step.skipPolicy shouldBeEqualTo SkipPolicy.NONE
        step.retryPolicy shouldBeEqualTo RetryPolicy.NONE
    }

    @Test
    fun `BatchStepBuilder - chunkSize 설정`() {
        val step = BatchStepBuilder<String, String>("step1").apply {
            reader(noopReader)
            writer(noopWriter)
            chunkSize(250)
        }.build()

        step.chunkSize shouldBeEqualTo 250
    }

    @Test
    fun `BatchStepBuilder - processor 람다 설정`() {
        val step = BatchStepBuilder<Int, String>("step1").apply {
            reader(object : BatchReader<Int> {
                override suspend fun read(): Int? = null
            })
            processor { n -> n.toString() }
            writer(object : BatchWriter<String> {
                override suspend fun write(items: List<String>) {}
            })
        }.build()

        step.processor.shouldNotBeNull()
    }

    @Test
    fun `BatchStepBuilder - skipPolicy 설정`() {
        val step = BatchStepBuilder<String, String>("step1").apply {
            reader(noopReader)
            writer(noopWriter)
            skipPolicy(SkipPolicy.ALL)
        }.build()

        step.skipPolicy.shouldSkip(RuntimeException(), 0L) shouldBe true
    }

    @Test
    fun `BatchStepBuilder - retryPolicy 설정`() {
        val policy = RetryPolicy(maxAttempts = 3, delay = 100.seconds)
        val step = BatchStepBuilder<String, String>("step1").apply {
            reader(noopReader)
            writer(noopWriter)
            retryPolicy(policy)
        }.build()

        step.retryPolicy.maxAttempts shouldBeEqualTo 3
    }

    @Test
    fun `BatchStepBuilder - commitTimeout 설정`() {
        val step = BatchStepBuilder<String, String>("step1").apply {
            reader(noopReader)
            writer(noopWriter)
            commitTimeout(30.seconds)
        }.build()

        step.commitTimeout shouldBeEqualTo 30.seconds
    }

    @Test
    fun `BatchStepBuilder - reader 없으면 IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            BatchStepBuilder<String, String>("step1").apply {
                writer(noopWriter)
            }.build()
        }
    }

    @Test
    fun `BatchStepBuilder - writer 없으면 IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            BatchStepBuilder<String, String>("step1").apply {
                reader(noopReader)
            }.build()
        }
    }

    @Test
    fun `BatchStepBuilder - chunkSize 0 이하면 IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            BatchStepBuilder<String, String>("step1").apply {
                reader(noopReader)
                writer(noopWriter)
                chunkSize(0)
            }.build()
        }
    }

    @Test
    fun `BatchStepBuilder - name이 blank이면 IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            BatchStepBuilder<String, String>("   ").apply {
                reader(noopReader)
                writer(noopWriter)
            }.build()
        }
    }

    // ─── BatchStep 직접 생성 테스트 ──────────────────────────────────────────

    @Test
    fun `BatchStep - 직접 생성`() {
        val step = BatchStep(
            name = "directStep",
            chunkSize = 100,
            reader = noopReader,
            writer = noopWriter,
        )

        step.name shouldBeEqualTo "directStep"
        step.chunkSize shouldBeEqualTo 100
        step.processor.shouldBeNull()
    }

    @Test
    fun `BatchStep - name blank이면 init에서 IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            BatchStep(
                name = "",
                chunkSize = 100,
                reader = noopReader,
                writer = noopWriter,
            )
        }
    }

    @Test
    fun `BatchStep - chunkSize 0 이하이면 init에서 IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            BatchStep(
                name = "step",
                chunkSize = -1,
                reader = noopReader,
                writer = noopWriter,
            )
        }
    }

    // ─── BatchJobBuilder 단위 테스트 ─────────────────────────────────────────

    @Test
    fun `BatchJobBuilder - 기본 빌드 성공`() {
        val job = BatchJobBuilder("myJob").apply {
            step<String, String>("step1") {
                reader(noopReader)
                writer(noopWriter)
            }
        }.build()

        job.shouldNotBeNull()
        job.name shouldBeEqualTo "myJob"
        job.steps.size shouldBeEqualTo 1
    }

    @Test
    fun `BatchJobBuilder - params vararg 설정`() {
        val job = BatchJobBuilder("paramJob").apply {
            params("date" to "2026-04-24", "env" to "test")
            step<String, String>("step1") {
                reader(noopReader)
                writer(noopWriter)
            }
        }.build()

        job.params["date"] shouldBeEqualTo "2026-04-24"
        job.params["env"] shouldBeEqualTo "test"
    }

    @Test
    fun `BatchJobBuilder - params map 설정`() {
        val job = BatchJobBuilder("paramJob").apply {
            params(mapOf("key" to 99, "flag" to true))
            step<String, String>("step1") {
                reader(noopReader)
                writer(noopWriter)
            }
        }.build()

        job.params["key"] shouldBeEqualTo 99
        job.params["flag"] shouldBeEqualTo true
    }

    @Test
    fun `BatchJobBuilder - 커스텀 repository 설정`() {
        val repo = InMemoryBatchJobRepository()
        val job = BatchJobBuilder("repoJob").apply {
            repository(repo)
            step<String, String>("step1") {
                reader(noopReader)
                writer(noopWriter)
            }
        }.build()

        job.shouldNotBeNull()
    }

    @Test
    fun `BatchJobBuilder - 다중 step 등록`() {
        val job = BatchJobBuilder("multiJob").apply {
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
        }.build()

        job.steps.size shouldBeEqualTo 3
        job.steps.map { it.name } shouldBeEqualTo listOf("step1", "step2", "step3")
    }

    @Test
    fun `BatchJobBuilder - addStep으로 미리 생성된 step 등록`() {
        val step = BatchStep(
            name = "prebuiltStep",
            chunkSize = 50,
            reader = noopReader,
            writer = noopWriter,
        )

        val job = BatchJobBuilder("addStepJob").apply {
            addStep(step)
        }.build()

        job.steps.size shouldBeEqualTo 1
        job.steps[0].name shouldBeEqualTo "prebuiltStep"
    }

    @Test
    fun `BatchJobBuilder - name blank이면 IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            BatchJobBuilder("").apply {
                step<String, String>("step1") {
                    reader(noopReader)
                    writer(noopWriter)
                }
            }.build()
        }
    }

    @Test
    fun `BatchJobBuilder - step 없으면 IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            BatchJobBuilder("emptyJob").build()
        }
    }

    // ─── batchJob DSL 함수 통합 테스트 ──────────────────────────────────────

    @Test
    fun `batchJob DSL - 실행 결과 Success 반환`() = runSuspendIO {
        val items = listOf("x", "y", "z")
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

    @Test
    fun `batchJob DSL - 빈 reader는 Success에 readCount=0`() = runSuspendIO {
        val job = batchJob("emptyReadJob") {
            step<String, String>("step1") {
                reader(noopReader)  // 항상 null 반환
                writer(noopWriter)
            }
        }

        val report = job.run()

        report shouldBeInstanceOf BatchReport.Success::class
        report.stepReports[0].readCount shouldBeEqualTo 0L
    }
}

private fun Any?.shouldBeNull() {
    if (this != null) throw AssertionError("Expected null but was $this")
}
