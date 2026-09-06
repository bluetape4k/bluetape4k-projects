package io.bluetape4k.ktor.core

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.ktor.server.application.Application
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertTimeout
import org.junit.jupiter.api.Test
import java.lang.reflect.InvocationTargetException
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

class ApplicationResourceLifecycleTest {

    @Test
    fun `installer is idempotent and closes resources at ApplicationStopped`() {
        val closeCount = AtomicInteger()
        lateinit var registry: ApplicationResourceRegistry

        testApplication {
            application {
                val first = installApplicationResourceLifecycle()
                val second = installApplicationResourceLifecycle()
                first shouldBeSameInstanceAs second
                registry = first
                registry.register { closeCount.incrementAndGet() }
            }
        }

        closeCount.get() shouldBeEqualTo 1
        registry.closeReport.state shouldBeEqualTo ApplicationResourceRegistryState.CLOSED
        registry.closeReport.closed shouldBeEqualTo 1
    }

    @Test
    fun `ApplicationStopped closes resources after application children are joined`() {
        lateinit var registry: ApplicationResourceRegistry
        var childJoinedBeforeClose = false

        testApplication {
            application {
                val child = launch {
                    awaitCancellation()
                }
                registry = installApplicationResourceLifecycle()
                registry.register { childJoinedBeforeClose = child.isCompleted }
            }
        }

        childJoinedBeforeClose.shouldBeTrue()
        registry.closeReport shouldBeEqualTo ApplicationResourceCloseReport(
            state = ApplicationResourceRegistryState.CLOSED,
            attempted = 1,
            inFlight = 0,
            closed = 1,
            failures = emptyList()
        )
    }

    @Test
    fun `ApplicationStopped after disposal timeout lets a bounded adapter drain before resource close`() {
        val resourceInUse = AtomicBoolean()
        val childCancellationStarted = CountDownLatch(1)
        val allowChildFinish = CountDownLatch(1)
        val childDrained = CountDownLatch(1)
        val activeWhenCloseStarted = AtomicBoolean()
        val overlapAfterDrain = AtomicBoolean(true)
        lateinit var registry: ApplicationResourceRegistry

        try {
            assertTimeout(Duration.ofSeconds(10)) {
                testApplication {
                    engine {
                        shutdownTimeout = 1L
                    }
                    application {
                        launch(start = CoroutineStart.UNDISPATCHED) {
                            resourceInUse.set(true)
                            try {
                                awaitCancellation()
                            } finally {
                                withContext(NonCancellable) {
                                    childCancellationStarted.countDown()
                                    allowChildFinish.await(5, TimeUnit.SECONDS).shouldBeTrue()
                                    resourceInUse.set(false)
                                    childDrained.countDown()
                                }
                            }
                        }

                        registry = installApplicationResourceLifecycle()
                        registry.register {
                            activeWhenCloseStarted.set(resourceInUse.get())
                            childCancellationStarted.await(5, TimeUnit.SECONDS).shouldBeTrue()
                            allowChildFinish.countDown()
                            childDrained.await(5, TimeUnit.SECONDS).shouldBeTrue()
                            overlapAfterDrain.set(resourceInUse.get())
                        }
                    }
                }
            }
        } finally {
            allowChildFinish.countDown()
        }

        activeWhenCloseStarted.get().shouldBeTrue()
        overlapAfterDrain.get().shouldBeFalse()
        registry.closeReport.state shouldBeEqualTo ApplicationResourceRegistryState.CLOSED
        registry.closeReport.closed shouldBeEqualTo 1
    }

    @Test
    fun `installer fatal close failure does not prevent remaining cleanup`() {
        val remainingCloseCount = AtomicInteger()
        lateinit var registry: ApplicationResourceRegistry

        testApplication {
            application {
                registry = installApplicationResourceLifecycle()
                registry.register { remainingCloseCount.incrementAndGet() }
                registry.register { throw AssertionError("credential-secret") }
            }
        }

        remainingCloseCount.get() shouldBeEqualTo 1
        registry.closeReport.state shouldBeEqualTo ApplicationResourceRegistryState.CLOSED
        registry.closeReport.failures.single().fatal.shouldBeTrue()
        registry.closeReport.toString() shouldBeEqualTo
            "ApplicationResourceCloseReport(state=CLOSED, attempted=2, inFlight=0, closed=1, " +
            "failures=[ApplicationResourceCloseFailure(registrationId=2, phase=SHUTDOWN, fatal=true)])"
    }

    @Test
    fun `installer ordinary close failure does not prevent remaining cleanup`() {
        val remainingCloseCount = AtomicInteger()
        lateinit var registry: ApplicationResourceRegistry

        testApplication {
            application {
                registry = installApplicationResourceLifecycle()
                registry.register { remainingCloseCount.incrementAndGet() }
                registry.register { throw IllegalStateException("credential-secret") }
            }
        }

        remainingCloseCount.get() shouldBeEqualTo 1
        registry.closeReport.state shouldBeEqualTo ApplicationResourceRegistryState.CLOSED
        registry.closeReport.closed shouldBeEqualTo 1
        registry.closeReport.failures.single().fatal.shouldBeFalse()
    }

    @Test
    fun `concurrent installer calls return one registry`() {
        lateinit var application: Application
        val registries = mutableListOf<ApplicationResourceRegistry>()
        val listLock = Any()
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(8)

        try {
            testApplication {
                application {
                    application = this
                    val futures = List(8) {
                        executor.submit<ApplicationResourceRegistry> {
                            start.await(5, TimeUnit.SECONDS)
                            application.installApplicationResourceLifecycle()
                        }
                    }
                    start.countDown()
                    futures.forEach { future ->
                        synchronized(listLock) {
                            registries += future.get(5, TimeUnit.SECONDS)
                        }
                    }
                }
            }
        } finally {
            executor.shutdownNow()
        }

        registries.size shouldBeEqualTo 8
        registries.drop(1).forEach { it shouldBeSameInstanceAs registries.first() }
    }

    @Test
    fun `late registration through installer reference closes after application stopped`() {
        lateinit var registry: ApplicationResourceRegistry
        val lateCloseCount = AtomicInteger()

        testApplication {
            application {
                registry = installApplicationResourceLifecycle()
            }
        }

        registry.register { lateCloseCount.incrementAndGet() }

        lateCloseCount.get() shouldBeEqualTo 1
        registry.closeReport.state shouldBeEqualTo ApplicationResourceRegistryState.CLOSED
        registry.closeReport.closed shouldBeEqualTo 1
    }

    @Test
    fun `installer fatal registration after stop exposes sanitized marker`() {
        lateinit var registry: ApplicationResourceRegistry

        testApplication {
            application {
                registry = installApplicationResourceLifecycle()
            }
        }

        val marker = assertFailsWith<Error> {
            registry.register { throw AssertionError("credential-secret") }
        }

        marker.message.orEmpty().contains("credential-secret").shouldBeFalse()
        registry.closeReport.failures.single().phase shouldBeEqualTo
            ApplicationResourceClosePhase.LATE_REGISTRATION
    }

    @Test
    fun `winner holder subscribes once and disposes once`() {
        val registrar = RecordingRegistrar()
        val holder = PrivateLifecycleHolder(registrar::subscribe)
        val barrier = java.util.concurrent.CyclicBarrier(8)
        val executor = Executors.newFixedThreadPool(8)
        try {
            val futures = List(8) {
                executor.submit<ApplicationResourceRegistry> {
                    barrier.await(5, TimeUnit.SECONDS)
                    holder.install()
                }
            }
            futures.forEach { it.get(5, TimeUnit.SECONDS) shouldBeSameInstanceAs holder.registry }
            registrar.subscribeCount.get() shouldBeEqualTo 1

            registrar.raiseStopped()
            registrar.raiseStopped()

            registrar.disposeCount.get() shouldBeEqualTo 1
            holder.registry.closeReport.state shouldBeEqualTo ApplicationResourceRegistryState.CLOSED
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `callback before handle publication waits for the ready holder and disposes once`() {
        val registrar = BlockingRecordingRegistrar()
        val observedLock = ObservedReentrantLock()
        val holder = PrivateLifecycleHolder(registrar::subscribe, observedLock)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val install = executor.submit<ApplicationResourceRegistry> { holder.install() }
            registrar.handlerRegistered.await(5, TimeUnit.SECONDS).shouldBeTrue()
            observedLock.observeNextContention()
            val stop = executor.submit { registrar.raiseStopped() }

            observedLock.callbackContended.await(5, TimeUnit.SECONDS).shouldBeTrue()
            registrar.allowHandleReturn.countDown()

            install.get(5, TimeUnit.SECONDS) shouldBeSameInstanceAs holder.registry
            stop.get(5, TimeUnit.SECONDS)
            registrar.subscribeCount.get() shouldBeEqualTo 1
            registrar.disposeCount.get() shouldBeEqualTo 1
        } finally {
            registrar.allowHandleReturn.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `failed holder never retries and exposes only sanitized failure`() {
        val registrar = FailingRegistrar(IllegalStateException("credential-secret"))
        val holder = PrivateLifecycleHolder(registrar::subscribe)

        repeat(2) {
            val failure = assertFailsWith<IllegalStateException> { holder.install() }
            failure.message shouldBeEqualTo "Application resource lifecycle installation failed."
            failure.cause shouldBeEqualTo null
            failure.suppressed.toList() shouldBeEqualTo emptyList()
        }
        registrar.subscribeCount.get() shouldBeEqualTo 1
        holder.registry.closeReport.state shouldBeEqualTo ApplicationResourceRegistryState.CLOSED
    }

    @Test
    fun `subscription disposal failure is swallowed without changing the close report`() {
        val registrar = RecordingRegistrar(IllegalStateException("credential-secret"))
        val holder = PrivateLifecycleHolder(registrar::subscribe)
        holder.install()

        registrar.raiseStopped()

        registrar.disposeCount.get() shouldBeEqualTo 1
        holder.registry.closeReport shouldBeEqualTo ApplicationResourceCloseReport(
            state = ApplicationResourceRegistryState.CLOSED,
            attempted = 0,
            inFlight = 0,
            closed = 0,
            failures = emptyList()
        )
    }

    @Test
    fun `registry and marker diagnostics exclude exception and resource secrets`() {
        val logger = ApplicationResourceRegistry.log as Logger
        val appender = CapturingAppender().also { logger.attach(it) }
        val registry = ApplicationResourceRegistry()
        val resource = SecretResource("resource-secret") {
            throw CredentialSecretFailure("message-secret")
        }

        try {
            registry.register(resource)
            val marker = assertFailsWith<Error> { registry.close() }

            val surfaces = buildList {
                add(registry.closeReport.toString())
                add(marker.message.orEmpty())
                add(marker.cause?.toString().orEmpty())
                addAll(marker.suppressed.map(Throwable::toString))
                addAll(appender.events.map { it.formattedMessage.orEmpty() })
                addAll(appender.events.map { it.throwableProxy?.message.orEmpty() })
            }
            listOf("resource-secret", "message-secret", "CredentialSecretFailure").forEach { secret ->
                surfaces.forEach { it.contains(secret).shouldBeFalse() }
            }
            appender.events.size shouldBeEqualTo 1
        } finally {
            logger.detach(appender)
        }
    }

    @Test
    fun `Ktor environment logger excludes fatal resource secrets`() {
        val appender = CapturingAppender()
        var logger: Logger? = null
        var previousLevel: Level? = null
        lateinit var registry: ApplicationResourceRegistry

        try {
            testApplication {
                application {
                    logger = environment.log as Logger
                    previousLevel = logger?.level
                    logger?.level = Level.DEBUG
                    logger?.attach(appender)
                    registry = installApplicationResourceLifecycle()
                    registry.register(SecretResource("resource-secret") {
                        throw CredentialSecretFailure("message-secret")
                    })
                }
            }

            val surfaces = buildList {
                add(registry.closeReport.toString())
                addAll(appender.events.map { it.formattedMessage.orEmpty() })
                addAll(appender.events.map { it.throwableProxy?.message.orEmpty() })
            }
            listOf("resource-secret", "message-secret", "CredentialSecretFailure").forEach { secret ->
                surfaces.forEach { it.contains(secret).shouldBeFalse() }
            }
            appender.events.any { event ->
                event.throwableProxy?.message == "Application resource close failed"
            }.shouldBeTrue()
            registry.closeReport.failures.single().fatal.shouldBeTrue()
        } finally {
            logger?.detach(appender)
            logger?.level = previousLevel
        }
    }

    @Test
    fun `throwing logging backend does not stop remaining cleanup`() {
        val logger = ApplicationResourceRegistry.log as Logger
        val appender = ThrowingAppender().also { logger.attach(it) }
        val remainingCloseCount = AtomicInteger()
        val registry = ApplicationResourceRegistry()
        registry.register { remainingCloseCount.incrementAndGet() }
        registry.register { throw IllegalStateException("message-secret") }

        try {
            registry.close()
        } finally {
            logger.detach(appender)
        }

        remainingCloseCount.get() shouldBeEqualTo 1
        appender.appendCount.get() shouldBeEqualTo 1
        registry.closeReport.closed shouldBeEqualTo 1
        registry.closeReport.failures.single().fatal.shouldBeFalse()
    }

    private class RecordingRegistrar(
        private val disposeFailure: Throwable? = null,
    ) {
        val subscribeCount = AtomicInteger()
        val disposeCount = AtomicInteger()

        @Volatile
        private var callback: (() -> Unit)? = null

        fun subscribe(onStopped: () -> Unit): DisposableHandle {
            subscribeCount.incrementAndGet()
            callback = onStopped
            return RecordingHandle(disposeCount, disposeFailure)
        }

        fun raiseStopped() {
            callback?.invoke()
        }
    }

    private class BlockingRecordingRegistrar {
        val subscribeCount = AtomicInteger()
        val disposeCount = AtomicInteger()
        val handlerRegistered = CountDownLatch(1)
        val allowHandleReturn = CountDownLatch(1)

        @Volatile
        private var callback: (() -> Unit)? = null

        fun subscribe(onStopped: () -> Unit): DisposableHandle {
            subscribeCount.incrementAndGet()
            callback = onStopped
            handlerRegistered.countDown()
            allowHandleReturn.await(5, TimeUnit.SECONDS).shouldBeTrue()
            return RecordingHandle(disposeCount)
        }

        fun raiseStopped() {
            callback?.invoke()
        }
    }

    private class FailingRegistrar(
        private val failure: Throwable,
    ) {
        val subscribeCount = AtomicInteger()

        fun subscribe(onStopped: () -> Unit): DisposableHandle {
            subscribeCount.incrementAndGet()
            throw failure
        }
    }

    private class RecordingHandle(
        private val disposeCount: AtomicInteger,
        private val disposeFailure: Throwable? = null,
    ) : DisposableHandle {
        override fun dispose() {
            disposeCount.incrementAndGet()
            disposeFailure?.let { throw it }
        }
    }

    /**
     * production holder를 public/internal ABI로 노출하지 않고도 lifecycle state machine을 검증합니다.
     */
    private class PrivateLifecycleHolder(
        registrar: (onStopped: () -> Unit) -> DisposableHandle,
        lock: ReentrantLock = ReentrantLock(),
    ) {
        private val holderClass = Class.forName(
            "io.bluetape4k.ktor.core.ApplicationResourceLifecycleHolder"
        )
        private val delegate = holderClass.declaredConstructors
            .single { it.parameterCount == 2 }
            .also { it.isAccessible = true }
            .newInstance(registrar, lock)
        private val registryGetter = holderClass
            .getDeclaredMethod("getRegistry")
            .also { it.isAccessible = true }
        private val installMethod = holderClass
            .getDeclaredMethod("install")
            .also { it.isAccessible = true }

        val registry: ApplicationResourceRegistry
            get() = registryGetter.invoke(delegate) as ApplicationResourceRegistry

        fun install(): ApplicationResourceRegistry = try {
            installMethod.invoke(delegate) as ApplicationResourceRegistry
        } catch (failure: InvocationTargetException) {
            throw failure.targetException
        }
    }

    private class ObservedReentrantLock : java.util.concurrent.locks.ReentrantLock() {
        val callbackContended = CountDownLatch(1)
        private val observeContention = java.util.concurrent.atomic.AtomicBoolean()

        fun observeNextContention() {
            check(observeContention.compareAndSet(false, true))
        }

        override fun lock() {
            if (!observeContention.compareAndSet(true, false)) {
                super.lock()
                return
            }
            if (super.tryLock()) return
            callbackContended.countDown()
            super.lock()
        }
    }

    private class SecretResource(
        private val name: String,
        private val closeAction: () -> Unit,
    ) : AutoCloseable {
        override fun close(): Unit = closeAction()

        override fun toString(): String = name
    }

    private class CredentialSecretFailure(message: String) : AssertionError(message)

    private class CapturingAppender : AppenderBase<ILoggingEvent>() {
        val events = CopyOnWriteArrayList<ILoggingEvent>()

        override fun append(eventObject: ILoggingEvent) {
            events += eventObject
        }
    }

    private class ThrowingAppender : AppenderBase<ILoggingEvent>() {
        val appendCount = AtomicInteger()

        override fun append(eventObject: ILoggingEvent) {
            appendCount.incrementAndGet()
            throw AssertionError("logger-secret")
        }
    }

    private fun Logger.attach(appender: AppenderBase<ILoggingEvent>) {
        appender.context = loggerContext
        appender.start()
        addAppender(appender)
    }

    private fun Logger.detach(appender: AppenderBase<ILoggingEvent>) {
        detachAppender(appender)
        appender.stop()
    }
}
