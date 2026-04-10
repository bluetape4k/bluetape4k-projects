package io.bluetape4k.spring.batch.exposed.support

import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.core.task.TaskExecutor

/**
 * VirtualThread 기반 [TaskExecutor]를 생성하는 팩토리 함수.
 *
 * [org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler]에 주입하여
 * 각 파티션을 VirtualThread에서 병렬 실행한다.
 *
 * 사용 예시:
 * ```kotlin
 * val partitionHandler = TaskExecutorPartitionHandler().apply {
 *     setStep(workerStep)
 *     setTaskExecutor(virtualThreadPartitionTaskExecutor(concurrencyLimit = 16))
 *     gridSize = 16
 * }
 * ```
 *
 * @param threadNamePrefix VirtualThread 이름 접두사
 * @param concurrencyLimit 동시 실행 파티션 수 (기본값: availableProcessors * 2)
 */
fun virtualThreadPartitionTaskExecutor(
    threadNamePrefix: String = "batch-partition-",
    concurrencyLimit: Int = Runtime.getRuntime().availableProcessors() * 2,
): TaskExecutor = SimpleAsyncTaskExecutor(threadNamePrefix).apply {
    setVirtualThreads(true)
    setConcurrencyLimit(concurrencyLimit)
}
