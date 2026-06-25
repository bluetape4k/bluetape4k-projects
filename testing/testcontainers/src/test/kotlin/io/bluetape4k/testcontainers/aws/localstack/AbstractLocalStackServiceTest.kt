@file:Suppress("DEPRECATION")

package io.bluetape4k.testcontainers.aws.localstack

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.testcontainers.aws.LocalStackServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

/**
 * LocalStack 서비스 테스트의 기반 클래스입니다.
 *
 * `@BeforeAll`에서 [LocalStackServer]를 시작하고 `@AfterAll`에서 종료합니다.
 * 서브클래스는 [localStack] 필드를 통해 실행 중인 컨테이너에 접근할 수 있습니다.
 *
 * `@TestInstance(PER_CLASS)` 라이프사이클을 사용하므로 `@BeforeAll`/`@AfterAll`을
 * 인스턴스 메서드로 선언할 수 있습니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractLocalStackServiceTest: AbstractContainerTest() {

    companion object: KLogging()

    protected lateinit var localStack: LocalStackServer

    @BeforeAll
    fun beforeAll() {
        localStack = LocalStackServer().apply { start() }
    }

    @AfterAll
    fun afterAll() {
        if (this::localStack.isInitialized) {
            try {
                localStack.close()
            } catch (e: Exception) {
                log.warn { "LocalStack 컨테이너 종료 중 오류가 발생했습니다: ${e.message}" }
            }
        }
    }
}
