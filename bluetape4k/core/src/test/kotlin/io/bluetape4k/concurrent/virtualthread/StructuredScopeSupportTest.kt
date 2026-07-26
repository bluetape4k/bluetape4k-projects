package io.bluetape4k.concurrent.virtualthread

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE

@EnabledForJreRange(min = JRE.JAVA_21)
class StructuredScopeSupportTest {

    companion object: KLoggingChannel()

    @Nested
    inner class WithAny {
        @Suppress("DEPRECATION")
        @Test
        fun `첫번째 완료된 작업의 결과를 얻는다`() {
            val result = structuredTaskScopeAny { scope ->
                scope.fork {
                    Thread.sleep(10)
                    "result1"
                }
                scope.fork {
                    Thread.sleep(100)
                    "result2"
                }

                scope.join()

                // 작업들이 완료되지 전에 예외가 발생한다면, 예외를 던진다.
                scope.result { IllegalStateException(it) }
            } // 먼저 완료되는 작업의 결과를 반환한다.
            result shouldBeEqualTo "result1"
        }

        @Suppress("DEPRECATION")
        @Test
        fun `첫번째 성공한 결과를 반환한다`() {
            val result = structuredTaskScopeAny { scope ->
                scope.fork {
                    Thread.sleep(10)
                    throw RuntimeException("Boom!")
                }
                scope.fork {
                    Thread.sleep(100)
                    "result2"
                }

                // 작업들이 완료되지 전에 예외가 발생한다면, 예외를 던진다.
                scope.join().result { IllegalStateException(it) }
            } // 먼저 완료되는 작업의 결과를 반환한다.

            result shouldBeEqualTo "result2"
        }

        @Suppress("DEPRECATION")
        @Test
        fun `모든 작업이 실패한다면, 첫번째 예외를 반환한다`() {
            assertFailsWith<RuntimeException> {
                structuredTaskScopeAny<String> { scope ->
                    scope.fork {
                        Thread.sleep(10)
                        throw RuntimeException("Boom 1")
                    }
                    scope.fork {
                        Thread.sleep(100)
                        throw IllegalArgumentException("Boom 2")
                    }

                    // 작업들이 완료되지 전에 예외가 발생한다면, 예외를 던진다.
                    scope.join().result { IllegalStateException(it) }
                }
            }.cause shouldBeInstanceOf RuntimeException::class
        }

        @Test
        fun `firstSuccess - 가장 먼저 완료된 작업의 결과를 반환한다`() {
            val result = structuredTaskScopeFirstSuccess<String> { scope ->
                scope.fork {
                    Thread.sleep(10)
                    "result1"
                }
                scope.fork {
                    Thread.sleep(100)
                    "result2"
                }
                scope.join().result { IllegalStateException("all failed: ${it.message}") }
            }
            result shouldBeEqualTo "result1"
        }

        @Test
        fun `firstSuccess - 첫번째 작업 실패 시 두번째 성공 결과를 반환한다`() {
            val result = structuredTaskScopeFirstSuccess<String> { scope ->
                scope.fork {
                    Thread.sleep(10)
                    throw RuntimeException("first failed")
                }
                scope.fork {
                    Thread.sleep(100)
                    "result2"
                }
                scope.join().result { IllegalStateException("all failed: ${it.message}") }
            }
            result shouldBeEqualTo "result2"
        }
    }

    @Nested
    inner class WithAll {

        @Suppress("DEPRECATION")
        @Test
        fun `모든 SubTask 들이 완료될 때 결과를 반환한다`() {
            val results: List<String> = structuredTaskScopeAll { scope ->
                val result1 = scope.fork {
                    Thread.sleep(10)
                    "result1"
                }
                val result2 = scope.fork {
                    Thread.sleep(100)
                    "result2"
                }

                scope.join().throwIfFailed()
                listOf(result1.get(), result2.get())
            }

            results shouldBeEqualTo listOf("result1", "result2")
        }

        @Suppress("DEPRECATION")
        @Test
        fun `Subtask에서 예외가 발생하면 예외를 던진다`() {
            assertFailsWith<RuntimeException> {
                structuredTaskScopeAll { scope ->
                    val result1 = scope.fork {
                        Thread.sleep(10)
                        "result1"
                    }
                    val result2 = scope.fork {
                        Thread.sleep(100)
                        throw RuntimeException("Boom!")
                    }

                    scope.join().throwIfFailed()

                    listOf(result1.get(), result2.get())
                }
            }
        }

        @Test
        fun `failFast - 모든 SubTask 들이 완료될 때 결과를 반환한다`() {
            val results: List<String> = structuredTaskScopeFailFast { scope ->
                val result1 = scope.fork {
                    Thread.sleep(10)
                    "result1"
                }
                val result2 = scope.fork {
                    Thread.sleep(100)
                    "result2"
                }
                scope.join().throwIfFailed()
                listOf(result1.get(), result2.get())
            }
            results shouldBeEqualTo listOf("result1", "result2")
        }

        @Test
        fun `failFast - Subtask 실패 시 예외가 전파되어야 한다`() {
            assertFailsWith<RuntimeException> {
                structuredTaskScopeFailFast { scope ->
                    scope.fork { Thread.sleep(10); "ok" }
                    scope.fork<String> { throw IllegalStateException("subtask failed") }
                    scope.join().throwIfFailed()
                    emptyList<String>()
                }
            }
        }
    }
}
