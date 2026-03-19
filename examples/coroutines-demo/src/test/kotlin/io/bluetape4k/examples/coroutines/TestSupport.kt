package io.bluetape4k.examples.coroutines

import io.bluetape4k.coroutines.support.getOrCurrent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext


/**
 * 정수가 짝수인지 판별합니다.
 */
fun Int.isEven(): Boolean = this % 2 == 0

/**
 * 정수가 홀수인지 판별합니다.
 *
 * NOTE: `this % 2 == 1` 은 음수 홀수(-3 % 2 == -1)에서 실패하므로 `!= 0` 으로 비교합니다.
 */
fun Int.isOdd(): Boolean = this % 2 != 0

/**
 * 테스트 시 과부하를 주기 위해 [action]을 [times] * [times] 만큼 반복적으로 수행합니다.
 *
 * [times]개의 코루틴을 launch 하고, 각 코루틴에서 [times]번 [action]을 호출합니다.
 *
 * @param coroutineContext 실행할 코루틴 컨텍스트 (기본: [Dispatchers.Default])
 * @param times 반복 횟수 (최소 1)
 * @param action 반복 실행할 suspend 함수
 */
suspend fun massiveRun(
    coroutineContext: CoroutineContext = Dispatchers.Default,
    times: Int = 1000,
    action: suspend () -> Unit,
) {
    val repeatSize = times.coerceAtLeast(1)

    // withContext 이므로 내부의 Job을 완료한 후 반환합니다
    withContext(coroutineContext.getOrCurrent()) {
        repeat(repeatSize) {
            launch {
                repeat(repeatSize) {
                    action()
                }
            }
        }
    }
}
