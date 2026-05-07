package io.bluetape4k.assertions

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.function.Executable

/**
 * Soft assertion 수집 및 실행을 위한 스코프.
 *
 * 상태는 단일 인스턴스에 한정되며, thread-local이나 동기화를 사용하지 않는다.
 * 스코프가 여러 스레드 간에 공유되지 않는 한 가상 스레드 안전하다.
 * [assertSoftly]를 통한 표준 사용 패턴이 이를 보장한다.
 */
class SoftAssertionScope {
    private val _executables = mutableListOf<Executable>()

    /**
     * 스코프 완료 시 평가할 assertion을 등록한다.
     *
     * @param block 실행할 assertion 블록
     */
    fun add(block: () -> Unit) {
        _executables.add(Executable { block() })
    }

    @PublishedApi
    internal fun executables(): List<Executable> = _executables.toList()
}

/**
 * 모든 soft assertion을 수집하여 일괄 실패 보고.
 *
 * 모든 assertion이 수집된 후, 실패가 있으면 JUnit 5의 [Assertions.assertAll]을 통해
 * `MultipleFailuresError`로 일괄 던진다. 모든 assertion이 성공하면 정상 반환한다.
 * 가상 스레드 안전 (thread-local 미사용).
 *
 * @param block [SoftAssertionScope] 내에서 실행할 assertion들 (add { } 형태)
 */
inline fun assertSoftly(block: SoftAssertionScope.() -> Unit) {
    val scope = SoftAssertionScope()
    scope.block()
    Assertions.assertAll(scope.executables())
}
