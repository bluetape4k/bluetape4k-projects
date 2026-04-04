package io.bluetape4k.states.examples

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.states.api.StateMachineException
import io.bluetape4k.states.core.on
import io.bluetape4k.states.core.suspendStateMachine
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * clinic-appointment 마이그레이션 데모 테스트입니다.
 *
 * 기존 AppointmentStateMachine 패턴을 bluetape4k-states DSL로 재구현한 예제입니다.
 */
class AppointmentExampleTest {

    companion object : KLogging()

    sealed class AppointmentState(val stateName: String) {
        data object PENDING : AppointmentState("PENDING")
        data object REQUESTED : AppointmentState("REQUESTED")
        data object CONFIRMED : AppointmentState("CONFIRMED")
        data object CHECKED_IN : AppointmentState("CHECKED_IN")
        data object IN_PROGRESS : AppointmentState("IN_PROGRESS")
        data object COMPLETED : AppointmentState("COMPLETED")
        data object CANCELLED : AppointmentState("CANCELLED")
        data object NO_SHOW : AppointmentState("NO_SHOW")

        override fun toString(): String = stateName
    }

    sealed class AppointmentEvent {
        data object Request : AppointmentEvent()
        data object Confirm : AppointmentEvent()
        data object CheckIn : AppointmentEvent()
        data object StartTreatment : AppointmentEvent()
        data object Complete : AppointmentEvent()
        data class Cancel(val reason: String = "") : AppointmentEvent()
        data object MarkNoShow : AppointmentEvent()

        override fun toString(): String = this::class.simpleName ?: "Unknown"
    }

    private fun createAppointmentFsm() = suspendStateMachine<AppointmentState, AppointmentEvent> {
        initialState = AppointmentState.PENDING
        finalStates = setOf(AppointmentState.COMPLETED, AppointmentState.CANCELLED, AppointmentState.NO_SHOW)

        // PENDING -> REQUESTED
        transition(AppointmentState.PENDING, on<AppointmentEvent.Request>(), to = AppointmentState.REQUESTED)
        // PENDING -> CANCELLED
        transition(AppointmentState.PENDING, on<AppointmentEvent.Cancel>(), to = AppointmentState.CANCELLED)

        // REQUESTED -> CONFIRMED
        transition(AppointmentState.REQUESTED, on<AppointmentEvent.Confirm>(), to = AppointmentState.CONFIRMED)
        // REQUESTED -> CANCELLED
        transition(AppointmentState.REQUESTED, on<AppointmentEvent.Cancel>(), to = AppointmentState.CANCELLED)

        // CONFIRMED -> CHECKED_IN
        transition(AppointmentState.CONFIRMED, on<AppointmentEvent.CheckIn>(), to = AppointmentState.CHECKED_IN)
        // CONFIRMED -> NO_SHOW
        transition(AppointmentState.CONFIRMED, on<AppointmentEvent.MarkNoShow>(), to = AppointmentState.NO_SHOW)
        // CONFIRMED -> CANCELLED
        transition(AppointmentState.CONFIRMED, on<AppointmentEvent.Cancel>(), to = AppointmentState.CANCELLED)

        // CHECKED_IN -> IN_PROGRESS
        transition(
            AppointmentState.CHECKED_IN,
            on<AppointmentEvent.StartTreatment>(),
            to = AppointmentState.IN_PROGRESS
        )
        // CHECKED_IN -> CANCELLED
        transition(AppointmentState.CHECKED_IN, on<AppointmentEvent.Cancel>(), to = AppointmentState.CANCELLED)

        // IN_PROGRESS -> COMPLETED
        transition(AppointmentState.IN_PROGRESS, on<AppointmentEvent.Complete>(), to = AppointmentState.COMPLETED)

        onTransition { prev, event, next ->
            log.debug { "예약 상태 전이: $prev --[$event]--> $next" }
        }
    }

    @Test
    fun `예약 정상 흐름 - PENDING에서 COMPLETED까지`() = runTest {
        val fsm = createAppointmentFsm()

        fsm.transition(AppointmentEvent.Request)
        fsm.currentState shouldBeEqualTo AppointmentState.REQUESTED

        fsm.transition(AppointmentEvent.Confirm)
        fsm.currentState shouldBeEqualTo AppointmentState.CONFIRMED

        fsm.transition(AppointmentEvent.CheckIn)
        fsm.currentState shouldBeEqualTo AppointmentState.CHECKED_IN

        fsm.transition(AppointmentEvent.StartTreatment)
        fsm.currentState shouldBeEqualTo AppointmentState.IN_PROGRESS

        fsm.transition(AppointmentEvent.Complete)
        fsm.currentState shouldBeEqualTo AppointmentState.COMPLETED

        fsm.isInFinalState().shouldBeTrue()
    }

    @Test
    fun `예약 취소 - REQUESTED 상태에서`() = runTest {
        val fsm = createAppointmentFsm()

        fsm.transition(AppointmentEvent.Request)
        fsm.transition(AppointmentEvent.Cancel("환자 요청"))

        fsm.currentState shouldBeEqualTo AppointmentState.CANCELLED
        fsm.isInFinalState().shouldBeTrue()
    }

    @Test
    fun `미내원 처리`() = runTest {
        val fsm = createAppointmentFsm()

        fsm.transition(AppointmentEvent.Request)
        fsm.transition(AppointmentEvent.Confirm)
        fsm.transition(AppointmentEvent.MarkNoShow)

        fsm.currentState shouldBeEqualTo AppointmentState.NO_SHOW
        fsm.isInFinalState().shouldBeTrue()
    }

    @Test
    fun `StateFlow로 상태 변경을 관찰할 수 있다`() = runTest {
        val fsm = createAppointmentFsm()

        fsm.stateFlow.value shouldBeEqualTo AppointmentState.PENDING

        fsm.transition(AppointmentEvent.Request)
        fsm.stateFlow.value shouldBeEqualTo AppointmentState.REQUESTED

        fsm.transition(AppointmentEvent.Confirm)
        fsm.stateFlow.value shouldBeEqualTo AppointmentState.CONFIRMED
    }

    @Test
    fun `IN_PROGRESS에서 취소 불가`() = runTest {
        val fsm = createAppointmentFsm()

        fsm.transition(AppointmentEvent.Request)
        fsm.transition(AppointmentEvent.Confirm)
        fsm.transition(AppointmentEvent.CheckIn)
        fsm.transition(AppointmentEvent.StartTreatment)

        assertThrows<StateMachineException> {
            fsm.transition(AppointmentEvent.Cancel("진료중 취소 시도"))
        }
    }
}
