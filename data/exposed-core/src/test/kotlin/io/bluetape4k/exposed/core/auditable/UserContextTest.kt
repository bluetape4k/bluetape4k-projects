package io.bluetape4k.exposed.core.auditable

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE

/**
 * [UserContext] 단위 테스트입니다.
 *
 * Virtual Thread / Structured Concurrency 환경(Java 21+)에서만 실행됩니다.
 */
@EnabledForJreRange(min = JRE.JAVA_21)
class UserContextTest {

    @Test
    fun `getCurrentUser는 기본값으로 system을 반환한다`() {
        UserContext.getCurrentUser() shouldBeEqualTo UserContext.DEFAULT_USERNAME
    }

    @Test
    fun `withUser 내부에서 getCurrentUser는 지정된 사용자명을 반환한다`() {
        UserContext.withUser("admin") {
            UserContext.getCurrentUser() shouldBeEqualTo "admin"
        }
    }

    @Test
    fun `withUser 종료 후 getCurrentUser는 기본값 system으로 복원된다`() {
        UserContext.withUser("admin") {
            // 블록 내부 검증
        }
        UserContext.getCurrentUser() shouldBeEqualTo UserContext.DEFAULT_USERNAME
    }

    @Test
    fun `withThreadLocalUser 내부에서 getCurrentUser는 지정된 사용자명을 반환한다`() {
        UserContext.withThreadLocalUser("coroutineUser") {
            UserContext.getCurrentUser() shouldBeEqualTo "coroutineUser"
        }
    }

    @Test
    fun `withThreadLocalUser 중첩 시 inner 종료 후 outer 사용자명으로 복원된다`() {
        UserContext.withThreadLocalUser("outer") {
            UserContext.getCurrentUser() shouldBeEqualTo "outer"

            UserContext.withThreadLocalUser("inner") {
                UserContext.getCurrentUser() shouldBeEqualTo "inner"
            }

            UserContext.getCurrentUser() shouldBeEqualTo "outer"
        }
    }

    @Test
    fun `withUser 내부에서 SCOPED_USER get은 지정된 사용자명을 반환한다`() {
        UserContext.withUser("scopedUser") {
            UserContext.SCOPED_USER.get() shouldBeEqualTo "scopedUser"
        }
    }
}
