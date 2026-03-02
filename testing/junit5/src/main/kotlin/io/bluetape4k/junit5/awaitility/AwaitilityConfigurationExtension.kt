package io.bluetape4k.junit5.awaitility

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.awaitility.Awaitility
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.time.Duration

/**
 * Awaitility 전역 기본값을 테스트 시작 전에 설정하는 JUnit5 확장입니다.
 *
 * ## 동작/계약
 * - `beforeAll`에서 Awaitility static 설정을 변경합니다.
 * - 기본 최대 대기 시간은 5초, poll interval/delay는 10ms로 설정됩니다.
 * - 설정은 JVM 전역 상태이므로 같은 JVM의 다른 테스트에도 영향을 줄 수 있습니다.
 *
 * ```kotlin
 * @ExtendWith(AwaitilityConfigurationExtension::class)
 * class MyTest
 * // Awaitility 기본 waitAtMost == 5s
 * ```
 */
class AwaitilityConfigurationExtension: BeforeAllCallback {

    companion object: KLogging()

    /**
     * 테스트 클래스 실행 전에 Awaitility 기본 설정을 적용합니다.
     *
     * ## 동작/계약
     * - uncaught exception 캡처를 활성화합니다.
     * - 최대 대기/폴링 간격/초기 지연을 고정 값으로 설정합니다.
     */
    override fun beforeAll(context: ExtensionContext) {
        log.trace { "Setup Awaitility configuration ..." }
        Awaitility.catchUncaughtExceptions()
        Awaitility.waitAtMost(Duration.ofSeconds(5))
        Awaitility.setDefaultPollInterval(Duration.ofMillis(10))
        Awaitility.setDefaultPollDelay(Duration.ofMillis(10))
        // Awaitility.pollInSameThread()
    }
}
