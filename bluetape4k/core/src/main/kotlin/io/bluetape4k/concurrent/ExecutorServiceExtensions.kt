package io.bluetape4k.concurrent

import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * 함수형 작업을 JDK timed `invokeAll`로 실행하고 입력 순서의 Future 목록을 반환합니다.
 *
 * timeout까지 끝나지 않은 작업은 JDK 계약에 따라 취소를 요청합니다. task 함수 목록을 `Callable`로
 * 바꾸는 O(n) 변환은 timed 호출 전에 수행되므로 이 Duration은 변환 비용까지 포함한 엄격한
 * wall-clock 상한이 아닙니다. interrupt와 executor 거부는 호출자에게 전달됩니다.
 *
 * @param tasks 실행할 작업 목록입니다.
 * @param timeout 작업 완료를 기다릴 최대 시간입니다.
 * @return 입력 순서와 대응하는 `Future` 목록입니다.
 */
fun <T> ExecutorService.invokeAll(
    tasks: Collection<() -> T>,
    timeout: kotlin.time.Duration,
): List<Future<T>> =
    invokeAll(tasks.map { task -> Callable { task() } }, timeout.inWholeNanoseconds, TimeUnit.NANOSECONDS)

/**
 * 함수형 작업 중 하나가 성공하면 그 결과를 반환하는 JDK timed `invokeAny`입니다.
 *
 * 성공 결과 하나를 반환하면 남은 작업에는 취소를 요청합니다. timeout 시 [TimeoutException],
 * 모든 작업이 실패하면 [ExecutionException]을 전달하며 interrupt와 executor 거부도 전파합니다.
 * task 함수 목록을 `Callable`로 바꾸는 O(n) 변환은 timed 호출 전에 수행되므로 이 Duration은
 * 변환 비용까지 포함한 엄격한 wall-clock 상한이 아닙니다. 작업이 interrupt에 협력하지 않으면
 * 취소 요청 후에도 실행을 계속할 수 있습니다.
 *
 * @param tasks 실행할 작업 목록입니다.
 * @param timeout 성공 결과를 기다릴 최대 시간입니다.
 * @return 먼저 성공한 작업의 결과입니다.
 */
fun <T> ExecutorService.invokeAny(
    tasks: Collection<() -> T>,
    timeout: kotlin.time.Duration,
): T = invokeAny(tasks.map { task -> Callable { task() } }, timeout.inWholeNanoseconds, TimeUnit.NANOSECONDS)

/**
 * executor 종료를 지정한 시간 동안 기다립니다.
 *
 * @param timeout 종료를 기다릴 최대 시간입니다.
 * @return 제한 시간 안에 executor가 종료되면 `true`입니다.
 */
fun ExecutorService.awaitTermination(timeout: kotlin.time.Duration) =
    awaitTermination(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
