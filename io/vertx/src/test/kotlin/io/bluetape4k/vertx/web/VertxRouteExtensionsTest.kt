package io.bluetape4k.vertx.web

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.vertx.AbstractVertxTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext
import io.vertx.junit5.VertxTestContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class VertxRouteExtensionsTest: AbstractVertxTest() {

    @Test
    fun `suspendHandler propagates cancellation without failing routing context`(
        vertx: Vertx,
        testContext: VertxTestContext,
    ) = runSuspendIO {
        val handlerCompleted = CompletableDeferred<Unit>()
        val handler = captureHandler { route ->
            route.suspendHandler {
                try {
                    throw CancellationException("route cancelled")
                } finally {
                    handlerCompleted.complete(Unit)
                }
            }
        }
        val ctx = mockRoutingContext()

        executeHandler(vertx, handler, ctx, handlerCompleted)

        verify(exactly = 0) { ctx.fail(any<Throwable>()) }
        testContext.completeNow()
    }

    @Test
    fun `suspendHandler fails routing context for normal exceptions`(
        vertx: Vertx,
        testContext: VertxTestContext,
    ) = runSuspendIO {
        val handlerCompleted = CompletableDeferred<Unit>()
        val failure = IllegalStateException("boom")
        val handler = captureHandler { route ->
            route.suspendHandler {
                try {
                    throw failure
                } finally {
                    handlerCompleted.complete(Unit)
                }
            }
        }
        val ctx = mockRoutingContext()

        executeHandler(vertx, handler, ctx, handlerCompleted)

        verify(exactly = 1) { ctx.fail(failure) }
        testContext.completeNow()
    }

    @Test
    fun `suspendFailureHandler propagates cancellation without failing routing context`(
        vertx: Vertx,
        testContext: VertxTestContext,
    ) = runSuspendIO {
        val handlerCompleted = CompletableDeferred<Unit>()
        val handler = captureFailureHandler { route ->
            route.suspendFailureHandler {
                try {
                    throw CancellationException("failure route cancelled")
                } finally {
                    handlerCompleted.complete(Unit)
                }
            }
        }
        val ctx = mockRoutingContext()

        executeHandler(vertx, handler, ctx, handlerCompleted)

        verify(exactly = 0) { ctx.fail(any<Throwable>()) }
        testContext.completeNow()
    }

    @Test
    fun `suspendFailureHandler fails routing context for normal exceptions`(
        vertx: Vertx,
        testContext: VertxTestContext,
    ) = runSuspendIO {
        val handlerCompleted = CompletableDeferred<Unit>()
        val failure = IllegalArgumentException("failure boom")
        val handler = captureFailureHandler { route ->
            route.suspendFailureHandler {
                try {
                    throw failure
                } finally {
                    handlerCompleted.complete(Unit)
                }
            }
        }
        val ctx = mockRoutingContext()

        executeHandler(vertx, handler, ctx, handlerCompleted)

        verify(exactly = 1) { ctx.fail(failure) }
        testContext.completeNow()
    }

    private fun captureHandler(register: (Route) -> Unit): Handler<RoutingContext> {
        val route = mockk<Route>()
        val handler = mutableListOf<Handler<RoutingContext>>()
        every { route.handler(capture(handler)) } returns route

        register(route)

        return handler.single()
    }

    private fun captureFailureHandler(register: (Route) -> Unit): Handler<RoutingContext> {
        val route = mockk<Route>()
        val handler = mutableListOf<Handler<RoutingContext>>()
        every { route.failureHandler(capture(handler)) } returns route

        register(route)

        return handler.single()
    }

    private suspend fun executeHandler(
        vertx: Vertx,
        handler: Handler<RoutingContext>,
        ctx: RoutingContext,
        handlerCompleted: CompletableDeferred<Unit>,
    ) {
        vertx.runOnContext {
            handler.handle(ctx)
        }
        withTimeout(3.seconds) {
            handlerCompleted.await()
        }
        delay(100)
    }

    private fun mockRoutingContext(): RoutingContext =
        mockk(relaxed = true)
}
