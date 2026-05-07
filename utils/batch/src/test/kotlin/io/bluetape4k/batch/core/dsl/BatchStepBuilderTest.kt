package io.bluetape4k.batch.core.dsl

import io.bluetape4k.batch.api.BatchReader
import io.bluetape4k.batch.api.BatchWriter
import io.bluetape4k.batch.api.SkipPolicy
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.workflow.api.RetryPolicy
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds
import io.bluetape4k.assertions.assertFailsWith

/**
 * [BatchStepBuilder] 단위 테스트.
 *
 * DSL 빌드, 검증, suspend 람다 오버로드, 복합 설정을 검증한다.
 */
class BatchStepBuilderTest {

    companion object : KLogging()

    // ─── 기본 reader/writer stubs ────────────────────────────────────────────

    private val noopReader = object : BatchReader<String> {
        override suspend fun read(): String? = null
    }

    private val noopWriter = object : BatchWriter<String> {
        override suspend fun write(items: List<String>) {}
    }

    // ─── 1. chunkSize(0) 설정 시 IllegalArgumentException ──────────────────

    /**
     * chunkSize=0 설정 시 build() 호출 시 IllegalArgumentException 발생 검증.
     *
     * [BatchStepBuilder.chunkSize]에서 양수 검증을 수행하므로
     * 0 값 설정 시 즉시 예외가 발생해야 한다.
     */
    @Test
    fun `chunkSize 0 설정 시 IllegalArgumentException 발생`() {
        assertFailsWith<IllegalArgumentException> {
            val builder = BatchStepBuilder<String, String>("testStep")
            builder.reader(noopReader)
            builder.writer(noopWriter)
            builder.chunkSize(0)  // 양수 검증 실패
            builder.build()
        }
    }

    // ─── 2. processor suspend 람다 오버로드 정상 동작 ──────────────────────

    /**
     * processor { item: T => O? } suspend 람다로 설정 후 step 실행 검증.
     *
     * suspend 람다 오버로드를 통해 processor를 설정하고
     * 실제 BatchStepRunner.run()으로 실행하여 아이템 변환이 정상 동작함을 검증한다.
     */
    @Test
    fun `processor suspend 람다로 Int 변환 정상 동작`() = runSuspendIO {
        val items = listOf(1, 2, 3)
        val collected = mutableListOf<String>()

        val job = batchJob("processorLambdaJob") {
            step<Int, String>("step1") {
                reader(object : BatchReader<Int> {
                    private val queue = ArrayDeque(items)
                    override suspend fun read(): Int? = queue.removeFirstOrNull()
                })
                processor { item: Int -> item.toString() }
                writer(object : BatchWriter<String> {
                    override suspend fun write(chunks: List<String>) { collected.addAll(chunks) }
                })
                chunkSize(2)
            }
        }

        val report = job.run()

        report.shouldNotBeNull()
        collected shouldBeEqualTo listOf("1", "2", "3")
    }

    // ─── 3. step 2개 + retryPolicy + skipPolicy + commitTimeout 결합 설정 ─

    /**
     * 복합 설정: 2개 step + retryPolicy + skipPolicy + commitTimeout 모두 설정.
     *
     * DSL 내에서 여러 정책을 동시에 설정하고 각 설정 값이 정확히 반영되었는지 검증한다.
     */
    @Test
    fun `복합 설정 - 두 step + retryPolicy + skipPolicy + commitTimeout`() {
        val customRetry = RetryPolicy(maxAttempts = 5, delay = 200.seconds)
        val customSkip = SkipPolicy.ALL
        val customTimeout = 120.seconds

        val job = batchJob("complexConfigJob") {
            step<String, String>("step1") {
                reader(noopReader)
                writer(noopWriter)
                chunkSize(250)
                retryPolicy(customRetry)
                skipPolicy(customSkip)
                commitTimeout(customTimeout)
            }
            step<String, String>("step2") {
                reader(noopReader)
                writer(noopWriter)
                chunkSize(500)
                retryPolicy(customRetry)
                skipPolicy(customSkip)
                commitTimeout(customTimeout)
            }
        }

        job.steps.size shouldBeEqualTo 2

        // step1 검증
        job.steps[0].apply {
            name shouldBeEqualTo "step1"
            chunkSize shouldBeEqualTo 250
            retryPolicy.maxAttempts shouldBeEqualTo 5
            retryPolicy.delay shouldBeEqualTo 200.seconds
            skipPolicy shouldBeEqualTo customSkip
            commitTimeout shouldBeEqualTo customTimeout
        }

        // step2 검증
        job.steps[1].apply {
            name shouldBeEqualTo "step2"
            chunkSize shouldBeEqualTo 500
            retryPolicy.maxAttempts shouldBeEqualTo 5
            skipPolicy shouldBeEqualTo customSkip
            commitTimeout shouldBeEqualTo customTimeout
        }
    }
}
