package io.bluetape4k.resilience4j

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

/**
 * 비동기 resilience4j 확장 함수가 사용할 스케줄러와 소유권을 함께 보관합니다.
 *
 * 호출자가 스케줄러를 제공하지 않으면 호출마다 전용 스케줄러를 만들고 terminal completion 뒤에 종료합니다.
 * 호출자가 제공한 스케줄러는 호출자가 계속 소유하므로 이 객체는 종료하지 않습니다.
 */
@PublishedApi
internal class SchedulerHandle private constructor(
    val scheduler: ScheduledExecutorService,
    private val owned: Boolean,
) {

    fun close() {
        if (owned) {
            scheduler.shutdown()
        }
    }

    @PublishedApi
    @Suppress("TooGenericExceptionCaught")
    internal fun <T> execute(block: (ScheduledExecutorService) -> T): T =
        try {
            block(scheduler)
        } catch (cause: Throwable) {
            close()
            throw cause
        }

    companion object {
        fun acquire(scheduler: ScheduledExecutorService?): SchedulerHandle =
            scheduler?.let { SchedulerHandle(it, owned = false) }
                ?: SchedulerHandle(Executors.newSingleThreadScheduledExecutor(), owned = true)
    }
}
