package io.bluetape4k.leader

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class LeaderGroupStateTest {

    companion object: KLogging()

    @Test
    fun `availableSlots - 최대 리더 수에서 활성 리더 수를 뺀 값을 반환한다`() {
        val state = LeaderGroupState(lockName = "job", maxLeaders = 5, activeCount = 2)
        state.availableSlots shouldBeEqualTo 3
    }

    @Test
    fun `isFull - 활성 리더가 최대에 도달하면 true 를 반환한다`() {
        val full = LeaderGroupState(lockName = "job", maxLeaders = 3, activeCount = 3)
        full.isFull.shouldBeTrue()

        val notFull = LeaderGroupState(lockName = "job", maxLeaders = 3, activeCount = 2)
        notFull.isFull.shouldBeFalse()
    }

    @Test
    fun `isEmpty - 활성 리더가 없으면 true 를 반환한다`() {
        val empty = LeaderGroupState(lockName = "job", maxLeaders = 3, activeCount = 0)
        empty.isEmpty.shouldBeTrue()

        val notEmpty = LeaderGroupState(lockName = "job", maxLeaders = 3, activeCount = 1)
        notEmpty.isEmpty.shouldBeFalse()
    }

    @Test
    fun `data class equality - 동일 값은 동등하다`() {
        val state1 = LeaderGroupState(lockName = "a", maxLeaders = 3, activeCount = 1)
        val state2 = LeaderGroupState(lockName = "a", maxLeaders = 3, activeCount = 1)

        state1 shouldBeEqualTo state2
    }

    @Test
    fun `경계 조건 - activeCount 가 0 일 때 availableSlots 는 maxLeaders 와 같다`() {
        val state = LeaderGroupState(lockName = "job", maxLeaders = 10, activeCount = 0)
        state.availableSlots shouldBeEqualTo 10
        state.isEmpty.shouldBeTrue()
        state.isFull.shouldBeFalse()
    }
}
