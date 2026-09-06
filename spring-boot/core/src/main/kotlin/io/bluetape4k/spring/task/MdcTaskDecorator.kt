package io.bluetape4k.spring.task

import io.bluetape4k.logging.captureMdcContext
import io.bluetape4k.logging.withMdcContext
import org.springframework.core.task.TaskDecorator

/**
 * decorate 시점의 caller MDC를 task 실행 동안 적용하고 worker의 이전 context를 복원합니다.
 *
 * decorate한 task는 실행될 때까지 복사한 MDC map을 보유합니다. queue가 큰 executor에서는
 * MDC를 작은 low-cardinality 식별자 집합으로 유지하세요.
 *
 * executor 생성·종료와 Spring bean 등록은 이 decorator의 책임이 아닙니다.
 */
class MdcTaskDecorator : TaskDecorator {
    override fun decorate(task: Runnable): Runnable {
        val callerContext = captureMdcContext()
        return Runnable {
            withMdcContext(callerContext) {
                task.run()
            }
        }
    }
}
