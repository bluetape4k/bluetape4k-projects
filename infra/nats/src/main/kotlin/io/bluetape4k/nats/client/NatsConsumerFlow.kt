package io.bluetape4k.nats.client

import io.bluetape4k.support.requireNotBlank
import io.nats.client.ConsumerContext
import io.nats.client.ConsumeOptions
import io.nats.client.IterableConsumer
import io.nats.client.JetStream
import io.nats.client.JetStreamSubscription
import io.nats.client.Message
import io.nats.client.PushSubscribeOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * NATS client pending queue의 기본 상한을 Flow adapter에 맞게 제한합니다.
 */
val defaultNatsFlowPushOptions: PushSubscribeOptions = PushSubscribeOptions.builder()
    .pendingMessageLimit(1_024)
    .pendingByteLimit(16L * 1024 * 1024)
    .build()

/**
 * Push adapter의 pending queue drop 또는 read-back 실패를 나타냅니다.
 *
 * [droppedMessages]가 양수이면 subscription pending queue에서 관찰된 증가량이고,
 * 0이면 drop count read-back 같은 관찰 실패를 뜻하며 원인은 [cause]에 보존됩니다.
 */
class NatsConsumerFlowException(
    val droppedMessages: Long,
    cause: Throwable? = null,
) : RuntimeException(
    if (cause == null) {
        "NATS consumer Flow dropped $droppedMessages message(s)"
    } else {
        "NATS consumer Flow state could not be verified"
    },
    cause,
) {
    init {
        require(droppedMessages >= 0) {
            "droppedMessages must be zero or positive: $droppedMessages"
        }
    }
}

/**
 * JetStream push subscription을 수집마다 만드는 cold [Flow]입니다.
 *
 * Flow는 message를 자동 승인하지 않습니다. business 처리 성공 뒤 수집자가
 * `ack()`을 호출하고, 재시도 가능한 실패에는 `nak()`, 재전달하지 않을 실패에는
 * `term()`을 선택해야 합니다. [capacity]는 adapter channel의 유한 용량이며
 * NATS pending limit 및 server `maxAckPending`과 별개의 상한입니다.
 *
 * 같은 Flow 인스턴스를 동시에 수집하면 subscription 생성 전에 실패합니다.
 * 수집이 취소되면 blocking receive를 interrupt하고 adapter가 만든 subscription만
 * `unsubscribe()`합니다. pending queue drop은 조용히 무시하지 않고
 * [NatsConsumerFlowException]으로 전달합니다.
 */
@Suppress("TooGenericExceptionCaught")
fun JetStream.consumeAsFlow(
    subject: String,
    options: PushSubscribeOptions = defaultNatsFlowPushOptions,
    capacity: Int = DEFAULT_NATS_FLOW_CAPACITY,
    receiveTimeout: Duration = 1.seconds,
): Flow<Message> {
    subject.requireNotBlank("subject")
    validateFlowArguments(capacity, receiveTimeout)
    validatePushOptions(options)

    val collecting = AtomicBoolean(false)
    return channelFlow {
        check(collecting.compareAndSet(false, true)) {
            "같은 NATS Flow 인스턴스는 동시에 collect할 수 없습니다."
        }

        var subscription: JetStreamSubscription? = null
        var primaryFailure: Throwable? = null
        var observedDropped = 0L

        try {
            subscription = runInterruptible(Dispatchers.IO) {
                subscribe(subject, options)
            }
            verifyPendingLimits(subscription, options)
            observedDropped = readDroppedCount(subscription)

            while (currentCoroutineContext().isActive) {
                val beforeReceive = readDroppedCount(subscription)
                val message = runInterruptible(Dispatchers.IO) {
                    subscription.nextMessage(receiveTimeout.toJavaDuration())
                }
                observedDropped = observeDropped(
                    subscription = subscription,
                    previous = maxOf(observedDropped, beforeReceive),
                )

                if (message == null && !subscription.isActive) {
                    break
                }
                if (message != null) {
                    send(message)
                }
            }
        } catch (throwable: Throwable) {
            primaryFailure = throwable
            throw throwable
        } finally {
            val cleanupFailure = cleanupPushSubscription(subscription)
            val dropFailure = subscription?.let { activeSubscription ->
                try {
                    val current = readDroppedCount(activeSubscription)
                    val delta = current - observedDropped
                    delta.takeIf { it > 0 }?.let(::NatsConsumerFlowException)
                } catch (cancellation: CancellationException) {
                    cancellation
                } catch (error: Error) {
                    error
                } catch (exception: Exception) {
                    exception
                }
            }
            try {
                finishFailure(primaryFailure, dropFailure, cleanupFailure)
            } finally {
                collecting.set(false)
            }
        }
    }.buffer(capacity)
}

/**
 * Pull [ConsumerContext]를 수집마다 만드는 cold [Flow]입니다.
 *
 * [options]의 batch는 [capacity]와 receiver가 보유할 수 있는 한계를 넘지 않도록
 * 유한하게 정규화합니다. byte-only batch는 message count 상한과 동시에 보장할 수
 * 없으므로 거부합니다. Flow는 `ack`/`nak`/`term`을 호출하지 않으며, 취소 시
 * adapter가 만든 [IterableConsumer]만 닫습니다.
 */
@Suppress("TooGenericExceptionCaught")
fun ConsumerContext.consumeAsFlow(
    options: ConsumeOptions = ConsumeOptions.DEFAULT_CONSUME_OPTIONS,
    capacity: Int = DEFAULT_NATS_FLOW_CAPACITY,
    receiveTimeout: Duration = 1.seconds,
): Flow<Message> {
    validateFlowArguments(capacity, receiveTimeout)
    require(options.batchBytes == 0L) {
        "pull batchBytes는 bounded Flow에서 지원하지 않습니다."
    }
    val effectiveOptions = boundedConsumeOptions(options, capacity)
    val collecting = AtomicBoolean(false)

    return channelFlow {
        check(collecting.compareAndSet(false, true)) {
            "같은 NATS Flow 인스턴스는 동시에 collect할 수 없습니다."
        }

        var consumer: IterableConsumer? = null
        var primaryFailure: Throwable? = null

        try {
            consumer = runInterruptible(Dispatchers.IO) {
                iterate(effectiveOptions)
            }

            while (currentCoroutineContext().isActive) {
                val message = runInterruptible(Dispatchers.IO) {
                    consumer.nextMessage(receiveTimeout.toJavaDuration())
                }

                if (message == null && (consumer.isStopped || consumer.isFinished)) {
                    break
                }
                if (message != null) {
                    send(message)
                }
            }
        } catch (throwable: Throwable) {
            primaryFailure = throwable
            throw throwable
        } finally {
            val cleanupFailure = cleanupIterableConsumer(consumer)
            try {
                finishFailure(primaryFailure, null, cleanupFailure)
            } finally {
                collecting.set(false)
            }
        }
    }.buffer(capacity)
}

private const val DEFAULT_NATS_FLOW_CAPACITY = 64
private const val MIN_NATS_FLOW_CAPACITY = 1
private const val MAX_NATS_FLOW_CAPACITY = 1_024
private val MIN_NATS_FLOW_RECEIVE_TIMEOUT = 100.milliseconds
private const val MAX_PENDING_MESSAGES = 65_536L
private const val MAX_PENDING_BYTES = 64L * 1024 * 1024

private fun validateFlowArguments(capacity: Int, receiveTimeout: Duration) {
    require(capacity in MIN_NATS_FLOW_CAPACITY..MAX_NATS_FLOW_CAPACITY) {
        "capacity must be between $MIN_NATS_FLOW_CAPACITY and $MAX_NATS_FLOW_CAPACITY: $capacity"
    }
    require(receiveTimeout.isFinite() && receiveTimeout >= MIN_NATS_FLOW_RECEIVE_TIMEOUT) {
        "receiveTimeout must be finite and at least $MIN_NATS_FLOW_RECEIVE_TIMEOUT: $receiveTimeout"
    }
}

private fun validatePushOptions(options: PushSubscribeOptions) {
    require(options.pendingMessageLimit in 1..MAX_PENDING_MESSAGES) {
        "pending message limit must be between 1 and $MAX_PENDING_MESSAGES: ${options.pendingMessageLimit}"
    }
    require(options.pendingByteLimit in 1..MAX_PENDING_BYTES) {
        "pending byte limit must be between 1 and $MAX_PENDING_BYTES: ${options.pendingByteLimit}"
    }
}

private fun boundedConsumeOptions(
    options: ConsumeOptions,
    capacity: Int,
): ConsumeOptions {
    val effectiveBatchSize = min(options.batchSize, capacity + 1)
    return ConsumeOptions.builder()
        .json(options.toJson())
        .batchSize(effectiveBatchSize)
        .build()
}

@Suppress("ThrowsCount", "TooGenericExceptionCaught")
private fun verifyPendingLimits(
    subscription: JetStreamSubscription,
    options: PushSubscribeOptions,
) {
    val (actualMessages, actualBytes) = try {
        subscription.pendingMessageLimit to subscription.pendingByteLimit
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Error) {
        throw error
    } catch (exception: Exception) {
        throw NatsConsumerFlowException(0, exception)
    }
    if (actualMessages != options.pendingMessageLimit || actualBytes != options.pendingByteLimit) {
        throw NatsConsumerFlowException(
            0,
            IllegalStateException(
                "NATS pending limit read-back mismatch: " +
                    "messages=$actualMessages/${options.pendingMessageLimit}, " +
                    "bytes=$actualBytes/${options.pendingByteLimit}",
            ),
        )
    }
}

@Suppress("TooGenericExceptionCaught")
private fun readDroppedCount(subscription: JetStreamSubscription): Long = try {
    subscription.droppedCount
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (error: Error) {
    throw error
} catch (exception: Exception) {
    throw NatsConsumerFlowException(0, exception)
}

private fun observeDropped(
    subscription: JetStreamSubscription,
    previous: Long,
): Long {
    val current = readDroppedCount(subscription)
    val delta = current - previous
    if (delta > 0) {
        throw NatsConsumerFlowException(delta)
    }
    return maxOf(previous, current)
}

@Suppress("TooGenericExceptionCaught")
private suspend fun cleanupPushSubscription(subscription: JetStreamSubscription?): Throwable? {
    if (subscription == null) {
        return null
    }
    return try {
        withContext(NonCancellable + Dispatchers.IO) {
            subscription.unsubscribe()
        }
        null
    } catch (cancellation: CancellationException) {
        cancellation
    } catch (error: Error) {
        error
    } catch (exception: Exception) {
        exception
    }
}

@Suppress("TooGenericExceptionCaught")
private suspend fun cleanupIterableConsumer(consumer: IterableConsumer?): Throwable? {
    if (consumer == null) {
        return null
    }
    return try {
        withContext(NonCancellable + Dispatchers.IO) {
            consumer.close()
        }
        null
    } catch (cancellation: CancellationException) {
        cancellation
    } catch (error: Error) {
        error
    } catch (exception: Exception) {
        exception
    }
}

private fun finishFailure(
    primary: Throwable?,
    dropFailure: Throwable?,
    cleanupFailure: Throwable?,
) {
    val addSuppressedToChain: (Throwable, Throwable) -> Unit = { root, suppressed ->
        if (root !== suppressed) {
            root.addSuppressed(suppressed)
            var cause = root.cause
            while (cause != null && cause !== root) {
                if (cause !== suppressed) {
                    cause.addSuppressed(suppressed)
                }
                cause = cause.cause
            }
        }
    }
    val terminalFailure = when {
        primary != null -> {
            dropFailure?.let { addSuppressedToChain(primary, it) }
            cleanupFailure?.let { addSuppressedToChain(primary, it) }
            primary
        }

        dropFailure != null -> {
            cleanupFailure?.let { addSuppressedToChain(dropFailure, it) }
            dropFailure
        }

        else -> cleanupFailure
    }
    if (terminalFailure != null) {
        throw terminalFailure
    }
}
