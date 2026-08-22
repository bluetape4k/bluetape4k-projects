import io.bluetape4k.nats.client.consumeAsFlow
import io.nats.client.ConsumerContext
import io.nats.client.ConsumeOptions
import io.nats.client.IterableConsumer
import io.nats.client.JetStream
import io.nats.client.JetStreamSubscription
import io.nats.client.Message
import io.nats.client.PushSubscribeOptions
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

private class PublishedApiProbe {
    val pushReceiveEntered = AtomicBoolean(false)
    val pushClosed = AtomicBoolean(false)
    val pullReceiveEntered = AtomicBoolean(false)
    val pullClosed = AtomicBoolean(false)
}

fun main() = runBlocking {
    val probe = PublishedApiProbe()
    val pushSubscription = proxy<JetStreamSubscription> { method, _ ->
        when (method.name) {
            "getPendingMessageLimit" -> 1_024L
            "getPendingByteLimit" -> 16L * 1024 * 1024
            "getDroppedCount" -> 0L
            "nextMessage" -> {
                probe.pushReceiveEntered.set(true)
                null
            }
            "isActive" -> true
            "unsubscribe" -> {
                probe.pushClosed.set(true)
                Unit
            }
            else -> defaultValue(method.returnType)
        }
    }
    val jetStream = proxy<JetStream> { method, _ ->
        if (method.name == "subscribe") pushSubscription else defaultValue(method.returnType)
    }

    withTimeoutOrNull(2.seconds) {
        jetStream.consumeAsFlow("events").take(1).collect { error("probe should not receive a message") }
    }

    val iterable = proxy<IterableConsumer> { method, _ ->
        when (method.name) {
            "nextMessage" -> {
                probe.pullReceiveEntered.set(true)
                null
            }
            "isStopped", "isFinished" -> false
            "close" -> {
                probe.pullClosed.set(true)
                Unit
            }
            else -> defaultValue(method.returnType)
        }
    }
    val consumerContext = proxy<ConsumerContext> { method, _ ->
        if (method.name == "iterate") iterable else defaultValue(method.returnType)
    }

    withTimeoutOrNull(2.seconds) {
        consumerContext.consumeAsFlow(ConsumeOptions.DEFAULT_CONSUME_OPTIONS).take(1)
            .collect { error("probe should not receive a message") }
    }

    check(probe.pushReceiveEntered.get()) { "published push Flow did not enter receive" }
    check(probe.pushClosed.get()) { "published push Flow did not clean up subscription" }
    check(probe.pullReceiveEntered.get()) { "published pull Flow did not enter receive" }
    check(probe.pullClosed.get()) { "published pull Flow did not clean up consumer" }
}

@Suppress("UNCHECKED_CAST")
private inline fun <reified T> proxy(
    crossinline handler: (Method, Array<out Any?>?) -> Any?,
): T = Proxy.newProxyInstance(
    T::class.java.classLoader,
    arrayOf(T::class.java),
    InvocationHandler { _, method, args -> handler(method, args) },
) as T

private fun defaultValue(type: Class<*>): Any? = when {
    !type.isPrimitive -> null
    type == Boolean::class.javaPrimitiveType -> false
    type == Char::class.javaPrimitiveType -> '\u0000'
    type == Byte::class.javaPrimitiveType -> 0.toByte()
    type == Short::class.javaPrimitiveType -> 0.toShort()
    type == Int::class.javaPrimitiveType -> 0
    type == Long::class.javaPrimitiveType -> 0L
    type == Float::class.javaPrimitiveType -> 0.0f
    type == Double::class.javaPrimitiveType -> 0.0
    else -> null
}
