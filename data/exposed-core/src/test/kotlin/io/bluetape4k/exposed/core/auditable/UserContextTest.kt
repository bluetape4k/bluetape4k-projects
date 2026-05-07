package io.bluetape4k.exposed.core.auditable

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertFailsWith

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
    fun `withUser 중첩 시 inner 종료 후 outer 사용자명으로 복원된다`() {
        UserContext.withUser("outer") {
            UserContext.getCurrentUser() shouldBeEqualTo "outer"

            UserContext.withUser("inner") {
                UserContext.getCurrentUser() shouldBeEqualTo "inner"
            }

            UserContext.getCurrentUser() shouldBeEqualTo "outer"
        }
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

    /**
     * block 에서 예외 발생 시 ThreadLocal 이 이전 값으로 복원되는지 검증.
     *
     * withUser finally 블록이 없으면 ThreadLocal 이 오염되어 다음 요청이 잘못된 사용자로 실행된다.
     */
    @Test
    fun `withUser 블록에서 예외 발생해도 ThreadLocal은 이전 값으로 복원된다`() {
        assertFailsWith<RuntimeException> {
            UserContext.withUser("errorUser") {
                throw RuntimeException("test error")
            }
        }
        // 예외 발생 후에도 ThreadLocal 이 복원되어야 한다 (finally 보장)
        UserContext.getCurrentUser() shouldBeEqualTo UserContext.DEFAULT_USERNAME
    }

    /**
     * withUser 중첩 상태에서 inner 블록 예외 발생 시 outer 사용자명이 복원되는지 검증.
     */
    @Test
    fun `withUser 중첩에서 inner 예외 발생 시 outer 사용자명으로 복원된다`() {
        UserContext.withUser("outer") {
            runCatching {
                UserContext.withUser("inner") {
                    throw RuntimeException("inner error")
                }
            }
            // inner 가 예외로 종료되어도 outer 사용자명이 복원되어야 한다
            UserContext.getCurrentUser() shouldBeEqualTo "outer"
        }
    }

    /**
     * withThreadLocalUser 블록에서 예외 발생 시 ThreadLocal 이 복원되는지 검증.
     */
    @Test
    fun `withThreadLocalUser 블록에서 예외 발생해도 ThreadLocal은 이전 값으로 복원된다`() {
        assertFailsWith<RuntimeException> {
            UserContext.withThreadLocalUser("errorUser") {
                throw RuntimeException("test error")
            }
        }
        UserContext.getCurrentUser() shouldBeEqualTo UserContext.DEFAULT_USERNAME
    }

    /**
     * 서로 다른 스레드 간 UserContext 값 격리를 검증.
     *
     * InheritableThreadLocal 은 자식 스레드에 값이 전파되지만,
     * 서로 다른 독립 스레드에서는 각자 독립된 값을 가져야 한다.
     */
    @Test
    fun `서로 다른 스레드는 독립적인 UserContext 값을 가진다`() {
        val user1Seen = AtomicReference<String?>(null)
        val user2Seen = AtomicReference<String?>(null)
        val latch = CountDownLatch(2)

        val executor = Executors.newFixedThreadPool(2)
        try {
            executor.submit {
                UserContext.withThreadLocalUser("thread1User") {
                    Thread.sleep(50) // 두 스레드가 겹치도록 일부러 대기
                    user1Seen.set(UserContext.getCurrentUser())
                }
                latch.countDown()
            }
            executor.submit {
                UserContext.withThreadLocalUser("thread2User") {
                    Thread.sleep(50)
                    user2Seen.set(UserContext.getCurrentUser())
                }
                latch.countDown()
            }
            latch.await()
        } finally {
            executor.shutdown()
        }

        user1Seen.get() shouldBeEqualTo "thread1User"
        user2Seen.get() shouldBeEqualTo "thread2User"
    }

    /**
     * withUser 가 블록의 반환값을 올바르게 전파하는지 검증.
     */
    @Test
    fun `withUser는 블록의 반환값을 전파한다`() {
        val result = UserContext.withUser("admin") {
            42
        }
        result shouldBeEqualTo 42
    }

    /**
     * withThreadLocalUser 가 블록의 반환값을 올바르게 전파하는지 검증.
     */
    @Test
    fun `withThreadLocalUser는 블록의 반환값을 전파한다`() {
        val result = UserContext.withThreadLocalUser("admin") {
            "hello"
        }
        result shouldBeEqualTo "hello"
    }

    /**
     * SCOPED_USER 가 withUser 스코프 밖에서 바인딩되어 있지 않은지 검증.
     *
     * ScopedValue.isBound() 는 withUser 블록 외부에서 false 여야 한다.
     */
    @Test
    fun `SCOPED_USER는 withUser 스코프 밖에서 바인딩되어 있지 않다`() {
        UserContext.SCOPED_USER.isBound.shouldBeFalse()

        UserContext.withUser("admin") {
            UserContext.SCOPED_USER.isBound.shouldBeTrue()
        }

        UserContext.SCOPED_USER.isBound.shouldBeFalse()
    }
}
