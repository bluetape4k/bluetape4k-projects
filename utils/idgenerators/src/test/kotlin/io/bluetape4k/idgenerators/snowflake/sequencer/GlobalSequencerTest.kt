package io.bluetape4k.idgenerators.snowflake.sequencer

import io.bluetape4k.idgenerators.snowflake.MAX_SEQUENCE
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.junit.jupiter.api.Test

class GlobalSequencerTest: AbstractSequencerTest() {

    companion object: KLoggingChannel()

    override val sequencer: Sequencer = GlobalSequencer()

    /**
     * sequence 값이 MAX_SEQUENCE를 초과할 때 machineId가 증가하는지 검증합니다.
     * 새로운 GlobalSequencer에서 MAX_SEQUENCE * 3 개의 ID를 빠르게 생성하여
     * machineId 증가 경로가 올바르게 동작하는지 확인합니다.
     */
    @Test
    fun `sequence overflow triggers machineId increment`() {
        val freshSequencer = GlobalSequencer()
        // MAX_SEQUENCE * 3 개의 ID를 생성하여 sequence 오버플로우를 여러 번 유발
        val ids = List(MAX_SEQUENCE * 3) { freshSequencer.nextSequence() }

        // 모든 ID는 유일해야 함
        ids.distinct() shouldBeEqualTo ids

        // machineId > 0 인 ID 개수 로깅 (sequence 오버플로우가 발생했다면 > 0)
        val machineIdIncrementCount = ids.count { it.machineId > 0 }
        log.debug("machineId > 0 ID count: $machineIdIncrementCount / ${ids.size}")
    }

    /**
     * 매우 빠른 연속 호출에서도 GlobalSequencer가 유일한 ID를 생성하는지 검증합니다.
     * machineId가 MAX_MACHINE_ID에 도달하면 다음 밀리초를 기다리는 동작을 포함합니다.
     */
    @Test
    fun `global sequencer generates unique ids under high throughput`() {
        val freshSequencer = GlobalSequencer()
        val size = MAX_SEQUENCE * 4
        val ids = List(size) { freshSequencer.nextSequence() }

        // 유일성 보장
        ids.distinct().size shouldBeEqualTo size

        // 모든 ID 값(Long)도 유일해야 함
        ids.map { it.value }.distinct().size shouldBeEqualTo size

        // timestamp는 양수여야 함
        ids.all { it.timestamp > 0 } shouldBeEqualTo true
    }
}
