package io.bluetape4k.states.examples

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.states.core.on
import io.bluetape4k.states.core.suspendStateMachine
import io.bluetape4k.states.testing.arrives
import io.bluetape4k.states.testing.assertRejects
import io.bluetape4k.states.testing.verifyPath
import io.bluetape4k.states.testing.via
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class AppointmentExampleTest {
    companion object: KLogging()

    sealed class S(val n: String) {
        data object PENDING: S("PENDING")
        data object REQUESTED: S("REQUESTED")
        data object CONFIRMED: S("CONFIRMED")
        data object CHECKED_IN: S("CHECKED_IN")
        data object IN_PROGRESS: S("IN_PROGRESS")
        data object COMPLETED: S("COMPLETED")
        data object CANCELLED: S("CANCELLED")
        data object NO_SHOW: S("NO_SHOW")
        override fun toString() = n
    }

    sealed class E {
        data object Request: E()
        data object Confirm: E()
        data object CheckIn: E()
        data object StartTreatment: E()
        data object Complete: E()
        data class Cancel(val reason: String = ""): E()
        data object MarkNoShow: E()
        override fun toString() = this::class.simpleName ?: "Unknown"
    }

    private fun fsm() = suspendStateMachine<S, E> {
        initialState = S.PENDING
        finalStates = setOf(S.COMPLETED, S.CANCELLED, S.NO_SHOW)
        transition(S.PENDING, on<E.Request>(), to = S.REQUESTED)
        transition(S.PENDING, on<E.Cancel>(), to = S.CANCELLED)
        transition(S.REQUESTED, on<E.Confirm>(), to = S.CONFIRMED)
        transition(S.REQUESTED, on<E.Cancel>(), to = S.CANCELLED)
        transition(S.CONFIRMED, on<E.CheckIn>(), to = S.CHECKED_IN)
        transition(S.CONFIRMED, on<E.MarkNoShow>(), to = S.NO_SHOW)
        transition(S.CONFIRMED, on<E.Cancel>(), to = S.CANCELLED)
        transition(S.CHECKED_IN, on<E.StartTreatment>(), to = S.IN_PROGRESS)
        transition(S.CHECKED_IN, on<E.Cancel>(), to = S.CANCELLED)
        transition(S.IN_PROGRESS, on<E.Complete>(), to = S.COMPLETED)
        onTransition { p, ev, n -> log.debug { "예약: $p --[$ev]--> $n" } }
    }

    @Test fun `예약 정상 흐름 - PENDING에서 COMPLETED까지`() = runTest {
        val m = fsm()
        m.verifyPath(
            S.PENDING via E.Request arrives S.REQUESTED,
            S.REQUESTED via E.Confirm arrives S.CONFIRMED,
            S.CONFIRMED via E.CheckIn arrives S.CHECKED_IN,
            S.CHECKED_IN via E.StartTreatment arrives S.IN_PROGRESS,
            S.IN_PROGRESS via E.Complete arrives S.COMPLETED,
        )
        m.isInFinalState().shouldBeTrue()
    }

    @Test fun `예약 취소 - REQUESTED 상태에서`() = runTest {
        val m = fsm()
        m.verifyPath(
            S.PENDING via E.Request arrives S.REQUESTED,
            S.REQUESTED via E.Cancel("환자 요청") arrives S.CANCELLED,
        )
        m.isInFinalState().shouldBeTrue()
    }

    @Test fun `미내원 처리`() = runTest {
        val m = fsm()
        m.verifyPath(
            S.PENDING via E.Request arrives S.REQUESTED,
            S.REQUESTED via E.Confirm arrives S.CONFIRMED,
            S.CONFIRMED via E.MarkNoShow arrives S.NO_SHOW,
        )
        m.isInFinalState().shouldBeTrue()
    }

    @Test fun `StateFlow로 상태 변경을 관찰할 수 있다`() = runTest {
        val m = fsm()
        m.stateFlow.value shouldBeEqualTo S.PENDING
        m.transition(E.Request); m.stateFlow.value shouldBeEqualTo S.REQUESTED
        m.transition(E.Confirm); m.stateFlow.value shouldBeEqualTo S.CONFIRMED
    }

    @Test fun `IN_PROGRESS에서 취소 불가`() = runTest {
        val m = fsm()
        m.verifyPath(
            S.PENDING via E.Request arrives S.REQUESTED,
            S.REQUESTED via E.Confirm arrives S.CONFIRMED,
            S.CONFIRMED via E.CheckIn arrives S.CHECKED_IN,
            S.CHECKED_IN via E.StartTreatment arrives S.IN_PROGRESS,
        )
        m.assertRejects(E.Cancel("진료중 취소 시도"))
    }
}
