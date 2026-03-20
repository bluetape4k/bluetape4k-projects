package io.bluetape4k.spring4.virtualthread

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Virtual Thread Executor를 제공하는 컨트롤러 베이스 클래스입니다.
 *
 * Spring Boot 4에서는 기본으로 VT가 활성화되므로,
 * 명시적 VT Executor가 필요한 경우에만 이 클래스를 상속합니다.
 */
abstract class AbstractVirtualThreadController {
    companion object {
        /** Virtual Thread Per Task Executor */
        val virtualThreadExecutor: ExecutorService =
            Executors.newVirtualThreadPerTaskExecutor()
    }
}
