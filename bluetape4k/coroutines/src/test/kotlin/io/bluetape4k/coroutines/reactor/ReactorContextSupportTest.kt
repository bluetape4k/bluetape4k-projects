package io.bluetape4k.coroutines.reactor

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.asCoroutineContext
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.util.context.Context

class ReactorContextSupportTest {

    companion object: KLoggingChannel()

    private val key = "answer"
    private val value = "42"

    @Test
    fun `coroutines에서 전달하는 context를 Reactor에서 사용하기`() = runTest {
        var captured: String? = null
        val flux = Flux.just("A")
            .contextWrite { context ->
                captured = context.getOrNull(key)
                context
            }
        flux.awaitFirst()
        captured.shouldBeNull()

        // Coroutines에서 ReactorContext로 key-value를 전달합니다.
        withContext(Context.of(key, value).asCoroutineContext()) {
            flux.awaitFirst()
        }
        captured shouldBeEqualTo value
    }

    @Test
    fun `reactor의 context를 coroutines에서 사용하기`() = runTest {
        var captured: String? = null

        // ReactorContext에 있는 `key` 값을 CoroutineContext에서 조회하여 사용합니다.
        val flow = flow {
            // captured = currentCoroutineContext()[ReactorContext]?.context?.getOrNull(key)
            captured = currentReactiveContext()?.getOrNull(key)
            emit("A")
        }

        // ReactorContext에 아무 값도 전달되지 않았으므로, captured는 null입니다.
        flow.asFlux()
            .awaitFirst()

        captured.shouldBeNull()

        // contextWrite 에서 Reactor Context로 key-value를 저장하면, flow에서 사용할 수 있습니다.
        flow.asFlux()
            .contextWrite { context -> context.put(key, value) }
            .awaitFirst()

        captured shouldBeEqualTo value
    }

    @Test
    fun `CoroutineContext 확장으로 ReactorContext 값을 조회할 수 있다`() = runTest {
        withContext(Context.of(key, value).asCoroutineContext()) {
            coroutineContext.getReactiveContext()?.getOrNull<String>(key) shouldBeEqualTo value
            coroutineContext.getReactorContextValueOrNull<String>(key) shouldBeEqualTo value
        }
    }
}
