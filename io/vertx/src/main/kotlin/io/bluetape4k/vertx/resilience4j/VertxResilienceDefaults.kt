package io.bluetape4k.vertx.resilience4j

import io.bluetape4k.utils.Runtimex
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

/**
 * Vertx Resilience4j 에서 사용하는 기본 공유 [ScheduledExecutorService] 인스턴스.
 *
 * 매 호출마다 새 스케줄러를 생성하지 않고, 모듈 레벨에서 하나의 인스턴스를 공유합니다.
 * JVM 종료 시 자동으로 shutdown 됩니다.
 *
 * ```kotlin
 * val retry = Retry.ofDefaults("test")
 * val future = retry.executeVertxFuture(defaultRetryScheduler) { service.call() }
 * ```
 */
@PublishedApi
internal val defaultRetryScheduler: ScheduledExecutorService by lazy {
    Executors.newSingleThreadScheduledExecutor { r ->
        Thread.ofVirtual().name("vertx-retry-scheduler").unstarted(r)
    }.also { exec ->
        Runtimex.addShutdownHook { runCatching { exec.shutdown() } }
    }
}

/**
 * Vertx TimeLimiter 에서 사용하는 기본 공유 [ScheduledExecutorService] 인스턴스.
 *
 * 매 호출마다 새 스케줄러를 생성하지 않고, 모듈 레벨에서 하나의 인스턴스를 공유합니다.
 * JVM 종료 시 자동으로 shutdown 됩니다.
 */
@PublishedApi
internal val defaultTimeLimiterScheduler: ScheduledExecutorService by lazy {
    Executors.newSingleThreadScheduledExecutor { r ->
        Thread.ofVirtual().name("vertx-timelimiter-scheduler").unstarted(r)
    }.also { exec ->
        Runtimex.addShutdownHook { runCatching { exec.shutdown() } }
    }
}
